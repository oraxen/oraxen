# Builds a compiled Oraxen jar and copies its path to the Windows clipboard.
$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $PSScriptRoot
$propertiesFile = Join-Path $projectDir 'gradle.properties'
$originalProperties = [System.IO.File]::ReadAllBytes($propertiesFile)
$utf8 = New-Object System.Text.UTF8Encoding($false)

try {
    $properties = [System.IO.File]::ReadAllText($propertiesFile)
    if ($properties -match '(?m)^\s*oraxen_compiled\s*=') {
        $properties = [regex]::Replace($properties, '(?m)^(\s*oraxen_compiled\s*=\s*).*$', '${1}true')
    } else {
        if ($properties.Length -gt 0 -and -not $properties.EndsWith("`n")) { $properties += "`r`n" }
        $properties += "oraxen_compiled=true`r`n"
    }
    [System.IO.File]::WriteAllText($propertiesFile, $properties, $utf8)

    $wrapper = Join-Path $projectDir 'gradlew.bat'
    if (Test-Path -LiteralPath $wrapper -PathType Leaf) {
        & $wrapper -p $projectDir build
    } else {
        & gradle -p $projectDir build
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }

    $libsDir = Join-Path $projectDir 'build/libs'
    $builtJar = Get-ChildItem -LiteralPath $libsDir -Filter '*.jar' -File |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    if ($null -eq $builtJar) { throw "Built jar was not found in $libsDir" }

    Set-Clipboard -Value $builtJar.FullName
    Write-Host "Copied jar path to the clipboard: $($builtJar.FullName)"
} finally {
    [System.IO.File]::WriteAllBytes($propertiesFile, $originalProperties)
}
