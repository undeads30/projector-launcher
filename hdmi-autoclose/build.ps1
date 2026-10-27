param(
    [string]$AndroidSdk = $env:ANDROID_HOME,
    [string]$Jdk = $env:JAVA_HOME,
    [string]$SigningKey = '',
    [string]$BuildToolsVersion = '34.0.0'
)
$ErrorActionPreference = 'Stop'
if (-not $AndroidSdk) { throw 'Set ANDROID_HOME or pass -AndroidSdk.' }
if (-not $Jdk) { throw 'Set JAVA_HOME or pass -Jdk (JDK 17).' }
$env:JAVA_HOME = $Jdk
$javaBin = Join-Path $Jdk 'bin'
$buildTools = Join-Path $AndroidSdk "build-tools/$BuildToolsVersion"
$androidJar = Join-Path $AndroidSdk 'platforms/android-34/android.jar'
foreach ($required in @("$javaBin/javac.exe", "$buildTools/aapt.exe", $androidJar)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Missing build dependency: $required" }
}
function Check-Exit { if ($LASTEXITCODE -ne 0) { throw "Build tool failed: $LASTEXITCODE" } }
if (-not $SigningKey) {
    $projectKey = Join-Path $PSScriptRoot '../launcher/app/launcher-debug.keystore'
    if (Test-Path -LiteralPath $projectKey) { $SigningKey = $projectKey }
    else {
        $SigningKey = Join-Path $env:USERPROFILE '.android/debug.keystore'
        if (-not (Test-Path -LiteralPath $SigningKey)) {
            New-Item -ItemType Directory -Force (Split-Path -Parent $SigningKey) | Out-Null
            & "$javaBin/keytool.exe" -genkeypair -keystore $SigningKey -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
            Check-Exit
        }
    }
}
$stage = Join-Path $env:TEMP ('projector-hdmi-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'src') -Destination $stage -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'res') -Destination $stage -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'AndroidManifest.xml') -Destination $stage
New-Item -ItemType Directory -Path "$stage/classes", "$stage/dex", "$stage/tests" | Out-Null
$javaSources = @(Get-ChildItem "$stage/src" -Filter '*.java' -Recurse | ForEach-Object FullName)
& "$javaBin/javac.exe" -encoding UTF-8 --release 8 -classpath $androidJar -d "$stage/classes" @javaSources
Check-Exit
& "$javaBin/javac.exe" -encoding UTF-8 -d "$stage/tests" (Join-Path $PSScriptRoot 'src/ru/projector/hdmiautoclose/HdmiPromptPolicy.java') (Join-Path $PSScriptRoot 'test/ru/projector/hdmiautoclose/HdmiPromptPolicyTest.java')
Check-Exit
& "$javaBin/java.exe" -cp "$stage/tests" ru.projector.hdmiautoclose.HdmiPromptPolicyTest
Check-Exit
& "$javaBin/jar.exe" cf "$stage/classes.jar" -C "$stage/classes" .
Check-Exit
& "$buildTools/d8.bat" --min-api 26 --lib $androidJar --output "$stage/dex" "$stage/classes.jar"
Check-Exit
& "$buildTools/aapt.exe" package -f -M "$stage/AndroidManifest.xml" -S "$stage/res" -I $androidJar -F "$stage/unsigned.apk"
Check-Exit
Push-Location "$stage/dex"
try { & "$buildTools/aapt.exe" add "$stage/unsigned.apk" classes.dex; Check-Exit } finally { Pop-Location }
& "$buildTools/zipalign.exe" -f 4 "$stage/unsigned.apk" "$stage/aligned.apk"
Check-Exit
& "$buildTools/apksigner.bat" sign --ks $SigningKey --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$stage/HDMI-Autoclose.apk" "$stage/aligned.apk"
Check-Exit
& "$buildTools/apksigner.bat" verify --verbose "$stage/HDMI-Autoclose.apk"
Check-Exit
$output = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force -Path $output | Out-Null
Copy-Item -LiteralPath "$stage/HDMI-Autoclose.apk" -Destination "$output/HDMI-Autoclose.apk" -Force
Write-Output "APK: $output/HDMI-Autoclose.apk"
