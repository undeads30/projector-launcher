# Verify and fetch pinned build dependencies from the matching release.
$ErrorActionPreference = 'Stop'
$publishRepoRoot = Split-Path -Parent $PSScriptRoot
$dependencyLines = Get-Content (Join-Path $publishRepoRoot 'DEPENDENCIES.sha256')
foreach ($dependencyLine in $dependencyLines) {
    if ($dependencyLine -notmatch '^([0-9a-f]{64})  (launcher/.*)$') { throw 'Invalid dependency manifest' }
    $expectedHash = $Matches[1]
    $relativePath = $Matches[2]
    $destination = Join-Path $publishRepoRoot $relativePath
    if ((Test-Path -LiteralPath $destination) -and (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLower() -eq $expectedHash) {
        Write-Output "Verified: $relativePath"
        continue
    }
    $fileName = [IO.Path]::GetFileName($relativePath)
    $uri = "https://github.com/undeads30/projector-launcher/releases/download/v2.1.0/$fileName"
    $downloadFile = Join-Path $env:TEMP ('projector-dependency-' + [guid]::NewGuid().ToString('N'))
    try {
        Invoke-WebRequest -Uri $uri -OutFile $downloadFile
        if ((Get-FileHash -LiteralPath $downloadFile -Algorithm SHA256).Hash.ToLower() -ne $expectedHash) { throw "Checksum mismatch: $fileName" }
        New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($destination)) -Force | Out-Null
        Move-Item -LiteralPath $downloadFile -Destination $destination -Force
        Write-Output "Downloaded and verified: $relativePath"
    } finally {
        if (Test-Path -LiteralPath $downloadFile) { Remove-Item -LiteralPath $downloadFile }
    }
}
