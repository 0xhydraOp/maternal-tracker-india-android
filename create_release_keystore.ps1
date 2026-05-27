$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$keystore = Join-Path $projectRoot "maternal-tracker-india-release.jks"
$signingFile = Join-Path $projectRoot "signing.properties"

if (Test-Path $keystore) {
    throw "Keystore already exists: $keystore"
}
if (Test-Path $signingFile) {
    throw "signing.properties already exists: $signingFile"
}

Add-Type -AssemblyName System.Web
$password = [System.Web.Security.Membership]::GeneratePassword(32, 8)

& keytool -genkeypair `
    -v `
    -keystore $keystore `
    -alias "maternal-tracker-india" `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $password `
    -keypass $password `
    -dname "CN=Maternal Tracker India, OU=Android, O=0xhydraOp, L=Murshidabad, ST=West Bengal, C=IN"

@"
storeFile=maternal-tracker-india-release.jks
storePassword=$password
keyAlias=maternal-tracker-india
keyPassword=$password
"@ | Set-Content -LiteralPath $signingFile -Encoding ASCII

Write-Host "Created release keystore and signing.properties."
Write-Host "Keep both files private. They are ignored by git."
