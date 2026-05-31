$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

if (-not (Test-Path $env:ANDROID_HOME)) {
    throw "Android SDK not found at $env:ANDROID_HOME"
}

$signingFile = Join-Path $projectRoot "signing.properties"
if (-not (Test-Path $signingFile)) {
    throw "Missing signing.properties. Create it from signing.properties.example or run create_release_keystore.ps1 first."
}

$gradleVersion = "8.10.2"
$gradleDir = Join-Path $projectRoot ".gradle-local\gradle-$gradleVersion"
$gradleZip = Join-Path $projectRoot ".gradle-local\gradle-$gradleVersion-bin.zip"
$gradleExe = Join-Path $gradleDir "bin\gradle.bat"

if (-not (Test-Path $gradleExe)) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $gradleZip) | Out-Null
    if (-not (Test-Path $gradleZip)) {
        $url = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
        Write-Host "Downloading Gradle $gradleVersion..."
        Invoke-WebRequest -Uri $url -OutFile $gradleZip
    }
    Write-Host "Extracting Gradle..."
    Expand-Archive -LiteralPath $gradleZip -DestinationPath (Join-Path $projectRoot ".gradle-local") -Force
}

Push-Location $projectRoot
try {
    & $gradleExe --no-daemon clean :app:assembleRelease :app:lintRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle release build failed with exit code $LASTEXITCODE"
    }
    New-Item -ItemType Directory -Force -Path "release" | Out-Null
    Copy-Item -LiteralPath "app\build\outputs\apk\release\app-release.apk" -Destination "release\MaternalTrackerIndia-v1.0.5-release.apk" -Force
    Get-ChildItem "release\MaternalTrackerIndia-v1.0.5-release.apk" | Select-Object FullName,Length,LastWriteTime
}
finally {
    Pop-Location
}
