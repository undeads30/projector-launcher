param([Parameter(Mandatory=$true)][string]$Toolchain)
$ErrorActionPreference='Stop'
$projectorJava=Join-Path $Toolchain 'jdk17/jdk-17.0.20+8/bin'
$projectorTools=Join-Path $Toolchain 'sdk/build-tools/34.0.0'
$projectorAndroid=Join-Path $Toolchain 'sdk/platforms/android-34/android.jar'
$projectorStage=Join-Path $env:TEMP ('cec-power-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path "$projectorStage/src","$projectorStage/classes","$projectorStage/tests","$projectorStage/dex" | Out-Null
Copy-Item -Path "$PSScriptRoot/src/*" -Destination "$projectorStage/src" -Recurse
function Check-Exit { if($LASTEXITCODE -ne 0){throw "Tool failed: $LASTEXITCODE"} }
$projectorPolicy=Join-Path $projectorStage 'src/ru/projector/cecpower/PowerPressSequence.java'
& "$projectorJava/javac.exe" -encoding UTF-8 --release 8 -d "$projectorStage/tests" $projectorPolicy "$projectorStage/src/ru/projector/cecpower/StandbyTrigger.java" "$projectorStage/src/ru/projector/cecpower/NativeShutdownAction.java" "$PSScriptRoot/test/PowerPressSequenceTest.java" "$PSScriptRoot/test/StandbyTriggerTest.java" "$PSScriptRoot/test/NativeShutdownActionTest.java"
Check-Exit
& "$projectorJava/java.exe" -cp "$projectorStage/tests" ru.projector.cecpower.NativeShutdownActionTest
Check-Exit
& "$projectorJava/java.exe" -cp "$projectorStage/tests" ru.projector.cecpower.StandbyTriggerTest
Check-Exit
& "$projectorJava/java.exe" -cp "$projectorStage/tests" ru.projector.cecpower.PowerPressSequenceTest
Check-Exit
$projectorSources=@(Get-ChildItem "$projectorStage/src" -Filter '*.java' -Recurse | ForEach-Object FullName)
& "$projectorJava/javac.exe" -encoding UTF-8 --release 8 -cp $projectorAndroid -d "$projectorStage/classes" @projectorSources
Check-Exit
& "$projectorJava/jar.exe" cf "$projectorStage/classes.jar" -C "$projectorStage/classes" .
Check-Exit
$env:JAVA_HOME=Join-Path $Toolchain 'jdk17/jdk-17.0.20+8'
& "$projectorTools/d8.bat" --min-api 28 --lib $projectorAndroid --output "$projectorStage/dex" "$projectorStage/classes.jar"
Check-Exit
& "$projectorJava/jar.exe" cf "$projectorStage/cec-power.jar" -C "$projectorStage/dex" classes.dex
Check-Exit
New-Item -ItemType Directory -Force -Path "$PSScriptRoot/dist" | Out-Null
Copy-Item -LiteralPath "$projectorStage/cec-power.jar" -Destination "$PSScriptRoot/dist/cec-power.jar" -Force
Write-Output "Built: $PSScriptRoot/dist/cec-power.jar"
