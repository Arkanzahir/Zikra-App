param(
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [string]$TemporaryRoot = [IO.Path]::GetTempPath()
)

$ErrorActionPreference = 'Stop'
$probeDirectory = Join-Path $TemporaryRoot ('zikra-provider-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $probeDirectory | Out-Null
$results = [ordered]@{
    checked_at = [DateTimeOffset]::Now.ToString('o')
    scope = 'Host HTTP and MP3 signatures; not Android playback or listening verification'
}

function Get-Probe([string]$Name, [string]$Url) {
    $destination = Join-Path $probeDirectory $Name
    $metadata = & curl.exe --fail-with-body --location --silent --show-error `
        --connect-timeout 15 --max-time 40 --output $destination `
        --write-out '%{http_code}|%{content_type}|%{size_download}' $Url
    $curlExit = $LASTEXITCODE
    $parts = "$metadata".Split('|')
    if ($curlExit -ne 0) { throw "Request failed: $Url (curl=$curlExit, status=$metadata)" }
    return @{ Path = $destination; Url = $Url; Http = [int]$parts[0]; Type = $parts[1] }
}

try {
    $text = Get-Probe 'verses.json' 'https://api.quran.com/api/v4/verses/by_chapter/67?language=id&translations=33&fields=text_uthmani&per_page=50&page=1'
    $verses = Get-Content -LiteralPath $text.Path -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($verses.verses.Count -ne 30 -or $verses.verses[0].verse_key -ne '67:1' `
        -or $verses.verses[-1].verse_key -ne '67:30' `
        -or -not $verses.verses[0].text_uthmani `
        -or $verses.verses[0].translations[0].resource_id -ne 33) {
        throw 'Unexpected Al-Mulk text/resource mapping'
    }
    $results.text = @{ url = $text.Url; http = $text.Http; verses = 30;
        first_key = '67:1'; last_key = '67:30'; translation_resource = 33 }
    $results.audio = @()
    foreach ($qari in @('ar.alafasy', 'ar.husary', 'ar.minshawi')) {
        $audio = Get-Probe "$qari.mp3" "https://cdn.islamic.network/quran/audio/128/$qari/1.mp3"
        $bytes = [IO.File]::ReadAllBytes($audio.Path)
        $id3 = $bytes.Length -ge 3 -and $bytes[0] -eq 73 -and $bytes[1] -eq 68 -and $bytes[2] -eq 51
        $frame = $bytes.Length -ge 2 -and $bytes[0] -eq 255 -and ($bytes[1] -band 224) -eq 224
        if ($bytes.Length -lt 129 -or $audio.Type -notmatch '^audio/' -or -not ($id3 -or $frame)) {
            throw "Invalid audio response: $qari"
        }
        $results.audio += @{ qari = $qari; verse_key = '1:1'; url = $audio.Url;
            http = $audio.Http; type = $audio.Type; bytes = $bytes.Length;
            mp3_signature = $true; sha256 = (Get-FileHash -LiteralPath $audio.Path -Algorithm SHA256).Hash }
    }
    $timing = Get-Probe 'timing.json' 'https://api.quran.com/api/v4/recitations/7/by_ayah/1:1?fields=segments'
    $timingData = Get-Content -LiteralPath $timing.Path -Raw | ConvertFrom-Json
    $item = $timingData.audio_files[0]
    if ($item.verse_key -ne '1:1' -or $item.segments.Count -eq 0) { throw 'Timing unavailable' }
    $recording = Get-Probe 'qf.mp3' ('https://audio.qurancdn.com/' + $item.url)
    $recordingHash = (Get-FileHash -LiteralPath $recording.Path -Algorithm SHA256).Hash
    $results.timing_sample = @{ url = $timing.Url; http = $timing.Http;
        verse_key = $item.verse_key; segments = $item.segments;
        recording_url = $recording.Url; recording_bytes = (Get-Item -LiteralPath $recording.Path).Length;
        recording_sha256 = $recordingHash;
        same_bytes_as_current_alafasy = ($recordingHash -eq $results.audio[0].sha256);
        scope = 'One ayah only; byte equality does not establish full-corpus timing coverage' }
} catch {
    $results.error = $_.Exception.Message
} finally {
    $outputFile = [IO.Path]::GetFullPath($OutputPath)
    $results | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $outputFile -Encoding UTF8
    # Only files in this invocation's newly created temporary folder are removed.
    $resolvedProbe = [IO.Path]::GetFullPath($probeDirectory)
    $resolvedRoot = [IO.Path]::GetFullPath($TemporaryRoot).TrimEnd('\') + '\'
    if (-not $resolvedProbe.StartsWith($resolvedRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Temporary cleanup path is outside selected temporary root'
    }
    Remove-Item -LiteralPath $resolvedProbe -Recurse -Force
}
if ($results.error) { throw $results.error }
Write-Output "Provider evidence saved: $outputFile"
