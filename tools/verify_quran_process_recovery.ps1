param(
  [string]$Adb = 'E:/AndroidSDK/platform-tools/adb.exe',
  [ValidatePattern('^127\.0\.0\.1:\d+$')][string]$Serial = '127.0.0.1:5583',
  [int]$ServerPort = 5042,
  [Parameter(Mandatory = $true)][string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Use a new evidence directory; existing files are preserved.' }
$output = (New-Item -ItemType Directory -Path $OutputDirectory).FullName
function Invoke-Device {
  $result = & $Adb -P $ServerPort -s $Serial @args
  if ($LASTEXITCODE -ne 0) { throw 'Owned-emulator command failed; inspect device and test prerequisites.' }
  return $result
}

$model = (Invoke-Device shell getprop ro.product.model | Out-String).Trim()
if ($model -notmatch 'sdk|emulator') { throw 'Only a disposable Android emulator is allowed.' }
Invoke-Device shell run-as com.zikra.app test -f files/quran-failure-emulator-authorized
& $Adb -P $ServerPort -s $Serial shell run-as com.zikra.app test -e files/quran-process-owned
if ($LASTEXITCODE -eq 0) { throw 'Preparation requires a fresh owned sandbox. Do not reuse an old ready marker or remove user data.' }
$prepare = 'com.zikra.app.reading.QuranProcessRecoveryTest#testPrepareThenWaitForProcessKill'
$recover = 'com.zikra.app.reading.QuranProcessRecoveryTest#testRecoverCommittedDataAfterProcessKill'
$runner = 'com.zikra.app.test/android.test.InstrumentationTestRunner'
$driver = Start-Process -FilePath $Adb -ArgumentList '-P', $ServerPort, '-s', $Serial,
    'shell', 'am', 'instrument', '-w', '-e', 'class', $prepare, $runner `
    -WindowStyle Hidden -PassThru -RedirectStandardOutput "$output/prepare.txt" `
    -RedirectStandardError "$output/prepare-error.txt"
try {
  $ready = $false
  $deadline = [DateTime]::UtcNow.AddSeconds(35)
  while ([DateTime]::UtcNow -lt $deadline) {
    & $Adb -P $ServerPort -s $Serial shell run-as com.zikra.app test -f files/quran-process-owned/kill-ready
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    if ($driver.HasExited) { break }
    Start-Sleep -Milliseconds 200
  }
  if (-not $ready) { throw 'Preparation did not reach its staged write. Review prepare.txt; do not count this as passed.' }
  $oldProcess = (Invoke-Device shell run-as com.zikra.app cat files/quran-process-owned/kill-ready | Out-String).Trim()
  $observed = (Invoke-Device shell pidof com.zikra.app | Out-String).Trim()
  if ($oldProcess -notmatch '^\d+$' -or $oldProcess -notin ($observed -split ' ')) {
    throw 'Process marker does not match the currently running target.'
  }
  Invoke-Device shell am force-stop com.zikra.app
  $remaining = (& $Adb -P $ServerPort -s $Serial shell pidof com.zikra.app | Out-String).Trim()
  if ($oldProcess -in ($remaining -split ' ')) { throw 'Target process was not terminated.' }
  if (-not $driver.WaitForExit(5000)) { throw 'Preparation driver did not exit after force-stop.' }
  Invoke-Device shell am instrument -w -e class $recover $runner | Set-Content "$output/recovery.txt"
  $passed = (Get-Content -LiteralPath "$output/recovery.txt" -Raw) -match 'OK \(1 test\)'
  $proof = [ordered]@{
    actualForceStop = $true
    targetProcessObservedBeforeKill = $true
    oldProcessGone = $true
    recoveryPassed = $passed
    privateContentLogged = $false
  }
  $proof | ConvertTo-Json | Set-Content "$output/proof.json"
  if (-not $passed) { throw 'Recovery did not pass. Inspect recovery.txt; no success is implied by process termination.' }
  $proof | ConvertTo-Json
} finally {
  if (-not $driver.HasExited) { Stop-Process -Id $driver.Id -Force }
}
