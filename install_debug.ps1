$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$apk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apk)) {
    & (Join-Path $projectRoot "build_apk.ps1")
}

adb install -r $apk
