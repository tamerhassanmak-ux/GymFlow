$ErrorActionPreference = 'Stop'
[xml]$pom = Get-Content pom.xml
$version = [string]$pom.project.version
$inputPath = 'target\package-input'
New-Item -ItemType Directory -Force "$inputPath\libs", 'target\installers' | Out-Null
Copy-Item target\GymFlow.jar "$inputPath\"
Copy-Item target\libs\*.jar "$inputPath\libs\"
$modules = 'java.scripting,java.sql,java.sql.rowset,java.naming,java.management,java.desktop,java.logging,java.security.jgss,java.security.sasl,java.xml,java.net.http,java.rmi,java.transaction.xa,java.instrument,jdk.crypto.ec,jdk.unsupported,jdk.charsets,jdk.localedata,jdk.zipfs'
jpackage --type app-image --name GymFlow --app-version $version --vendor MAKSOFT --input $inputPath --main-jar GymFlow.jar --main-class com.mycompany.lazar_app.Launcher --add-modules $modules --java-options '-Dfile.encoding=UTF-8' --icon packaging\windows\gymflow.ico --dest target\installers
if ($LASTEXITCODE -ne 0) { throw 'Application image build failed' }
# Exercise the native launcher and its embedded runtime (jpackage may omit java.exe).
$smoke = Start-Process -FilePath (Resolve-Path 'target\installers\GymFlow\GymFlow.exe') -ArgumentList '--self-test' -PassThru
if (-not $smoke.WaitForExit(30000)) { $smoke.Kill(); throw 'Bundled runtime smoke test timed out' }
if ($smoke.ExitCode -ne 0) { throw 'Bundled runtime smoke test failed' }
foreach ($kind in @('exe', 'msi')) {
    jpackage --type $kind --name GymFlow --app-version $version --vendor MAKSOFT --app-image target\installers\GymFlow --win-per-user-install --win-menu --win-shortcut --win-dir-chooser --win-upgrade-uuid 'f5c40991-21f0-48fb-86b6-d334aa3f8526' --dest target\installers
    if ($LASTEXITCODE -ne 0) { throw "$kind package failed" }
}
Get-ChildItem target\installers -File | Where-Object { $_.Extension -in '.exe', '.msi' } | ForEach-Object {
    $hash = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $($_.Name)"
} | Set-Content -Encoding ascii target\installers\SHA256SUMS
