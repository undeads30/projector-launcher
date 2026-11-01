param([Parameter(Mandatory=$true)][string]$Toolchain, [Parameter(Mandatory=$true)][string]$SigningKey)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = Join-Path $Toolchain 'jdk17/jdk-17.0.20+8'
$javaBin = Join-Path $env:JAVA_HOME 'bin'
$buildTools = Join-Path $Toolchain 'sdk/build-tools/34.0.0'
$androidJar = Join-Path $Toolchain 'sdk/platforms/android-34/android.jar'
$stage = Join-Path $env:TEMP ('projector-clock-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'src') -Destination $stage -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'assets') -Destination $stage -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'AndroidManifest.xml') -Destination $stage
New-Item -ItemType Directory -Path "$stage/classes", "$stage/dex" | Out-Null
function Check-Exit { if ($LASTEXITCODE -ne 0) { throw "Build tool failed: $LASTEXITCODE" } }
$javaSources = @(Get-ChildItem "$stage/src" -Filter '*.java' -Recurse | ForEach-Object FullName)
& "$javaBin/javac.exe" -encoding UTF-8 --release 8 -classpath $androidJar -d "$stage/classes" @javaSources
Check-Exit
& "$javaBin/jar.exe" cf "$stage/classes.jar" -C "$stage/classes" .
Check-Exit
& "$buildTools/d8.bat" --min-api 23 --lib $androidJar --output "$stage/dex" "$stage/classes.jar"
Check-Exit
& "$buildTools/aapt.exe" package -f -M "$stage/AndroidManifest.xml" -A "$stage/assets" -I $androidJar -F "$stage/unsigned.apk"
Check-Exit
Push-Location "$stage/dex"
try { & "$buildTools/aapt.exe" add "$stage/unsigned.apk" classes.dex; Check-Exit } finally { Pop-Location }
& "$buildTools/zipalign.exe" -f 4 "$stage/unsigned.apk" "$stage/aligned.apk"
Check-Exit
& "$buildTools/apksigner.bat" sign --ks $signingKey --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$stage/DigitalClock.apk" "$stage/aligned.apk"
Check-Exit
& "$buildTools/apksigner.bat" verify --verbose --print-certs "$stage/DigitalClock.apk"
Check-Exit
New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot '../dist') -Force | Out-Null
Copy-Item -LiteralPath "$stage/DigitalClock.apk" -Destination (Join-Path $PSScriptRoot '../dist/DigitalClock.apk') -Force
Write-Output "Build complete. Staging directory: $stage"
