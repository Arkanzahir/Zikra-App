param(
    [string]$ConfigPath = (Join-Path $PSScriptRoot '../.local/quran-foundation.env'),
    [Parameter(Mandatory = $true)][string]$OutputPath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$config = @{}
foreach ($line in [IO.File]::ReadAllLines([IO.Path]::GetFullPath($ConfigPath))) {
    if ($line -match '^([A-Z_]+)=(.*)$') {
        $config[$Matches[1]] = $Matches[2].Trim().Trim([char]34).Trim([char]39)
    }
}
if ($config['QF_ENV'] -notin @('production', 'prelive') `
    -or -not $config['QF_CLIENT_ID'] -or -not $config['QF_CLIENT_SECRET']) {
    throw 'Missing or invalid local QF configuration; credential values are not logged.'
}
$environment = $config['QF_ENV']
$authHost = if ($environment -eq 'production') { 'https://oauth2.quran.foundation' } else { 'https://prelive-oauth2.quran.foundation' }
$apiHost = if ($environment -eq 'production') { 'https://apis.quran.foundation' } else { 'https://apis-prelive.quran.foundation' }
$report = [ordered]@{
    checked_at = [DateTimeOffset]::Now.ToString('o')
    environment = $environment
    authentication_http = $null
    probes = @()
    credentials_or_tokens_saved = $false
    android_playback_tested = $false
}
$handler = New-Object System.Net.Http.HttpClientHandler
$handler.AllowAutoRedirect = $false
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds(35)
$stage = 'authentication'
try {
    $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, ($authHost + '/oauth2/token'))
    $basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($config['QF_CLIENT_ID'] + ':' + $config['QF_CLIENT_SECRET']))
    $request.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Basic', $basic)
    $request.Content = New-Object System.Net.Http.StringContent('grant_type=client_credentials&scope=content', [Text.Encoding]::UTF8, 'application/x-www-form-urlencoded')
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        try {
            $report.authentication_http = [int]$response.StatusCode
            if (-not $response.IsSuccessStatusCode) { throw 'Token endpoint rejected request' }
            $tokenData = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            if (-not $tokenData.access_token) { throw 'Token unavailable' }
            $token = $tokenData.access_token
        } finally { if ($response) { $response.Dispose() } }
    } finally { $request.Dispose() }
    $stage = 'content'
    $paths = @(
        @{ Name='chapters'; Path='/chapters'; Kind='chapters' },
        @{ Name='mulk_text'; Path='/verses/by_chapter/67?language=id&translations=33&fields=text_uthmani&per_page=50&page=1'; Kind='text' },
        @{ Name='mushaf_page_1'; Path='/verses/by_page/1?words=true&word_fields=code_v2,line_number,page_number&mushaf=1&per_page=50'; Kind='page' },
        @{ Name='alafasy_word_timing_1_1'; Path='/recitations/7/by_ayah/1:1?fields=segments'; Kind='timing' }
    )
    foreach ($probe in $paths) {
        $row = [ordered]@{ name=$probe.Name; path=$probe.Path; http=$null }
        $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Get, ($apiHost + '/content/api/v4' + $probe.Path))
        $request.Headers.Add('x-client-id', $config['QF_CLIENT_ID'])
        $request.Headers.Add('x-auth-token', $token)
        try {
            $response = $client.SendAsync($request).GetAwaiter().GetResult()
            try {
                $row.http = [int]$response.StatusCode
                if ($response.IsSuccessStatusCode) {
                    $data = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
                    switch ($probe.Kind) {
                        'chapters' { $row.chapter_count = @($data.chapters).Count }
                        'text' {
                            $row.verse_count = @($data.verses).Count
                            $row.first_key = $data.verses[0].verse_key
                            $row.last_key = $data.verses[-1].verse_key
                            $row.uthmani_present = [bool]$data.verses[0].text_uthmani
                            $row.translation_resource = $data.verses[0].translations[0].resource_id
                        }
                        'page' {
                            $words = @($data.verses | ForEach-Object { $_.words })
                            $row.verse_count = @($data.verses).Count
                            $row.word_count = $words.Count
                            $row.glyphs_present = @($words | Where-Object { $_.code_v2 }).Count
                            $row.line_metadata_present = @($words | Where-Object { $_.line_number }).Count
                        }
                        'timing' {
                            $row.audio_items = @($data.audio_files).Count
                            $row.segment_count = @($data.audio_files[0].segments).Count
                            $row.verse_key = $data.audio_files[0].verse_key
                        }
                    }
                }
            } finally { $response.Dispose() }
        } catch { $row.error = 'Request or response parsing failed; sensitive details suppressed' }
        finally { $request.Dispose() }
        $report.probes += $row
    }
} catch {
    $report.failure_stage = $stage
    $report.error = 'Request failed; raw response and credentials intentionally not logged'
} finally {
    $client.Dispose()
    $handler.Dispose()
    $config.Clear()
    $basic = $null
    $token = $null
    $tokenData = $null
    $report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $OutputPath -Encoding UTF8
}
$report | ConvertTo-Json -Depth 6
