# Builds a beta Oraxen jar and copies the file to the Windows clipboard.
$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $PSScriptRoot
$propertiesFile = Join-Path $projectDir 'gradle.properties'
$originalProperties = [System.IO.File]::ReadAllBytes($propertiesFile)
$utf8 = New-Object System.Text.UTF8Encoding($false)

try {
    $properties = [System.IO.File]::ReadAllText($propertiesFile)
    $versionMatch = [regex]::Match($properties, '(?m)^\s*pluginVersion\s*=\s*(.*)$')
    if (-not $versionMatch.Success -or [string]::IsNullOrWhiteSpace($versionMatch.Groups[1].Value)) {
        throw "pluginVersion was not found in $propertiesFile"
    }
    $savedVersion = $versionMatch.Groups[1].Value.Trim()
    $commit = (& git -C $projectDir rev-parse --short HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or -not $commit) { throw 'Could not determine the current Git commit' }

    $properties = [regex]::Replace($properties, '(?m)^(\s*pluginVersion\s*=\s*).*$', {
        param($match)
        $match.Groups[1].Value + $savedVersion + '-' + $commit
    })
    if ($properties -match '(?m)^\s*oraxen_compiled\s*=') {
        $properties = [regex]::Replace($properties, '(?m)^(\s*oraxen_compiled\s*=\s*).*$', '${1}false')
    } else {
        if ($properties.Length -gt 0 -and -not $properties.EndsWith("`n")) { $properties += "`r`n" }
        $properties += "oraxen_compiled=false`r`n"
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

    # Use an STA process and persist the file-drop entry after it exits.
    $clipboardScript = @'
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
$files = New-Object System.Collections.Specialized.StringCollection
[void]$files.Add('__JAR_PATH__')
[System.Windows.Forms.Clipboard]::SetFileDropList($files)
'@
    $clipboardScript = $clipboardScript.Replace('__JAR_PATH__', $builtJar.FullName.Replace("'", "''"))
    $clipboardCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($clipboardScript))
    & powershell.exe -NoProfile -STA -EncodedCommand $clipboardCommand
    if ($LASTEXITCODE -ne 0) { throw "Could not copy the built jar to the clipboard" }
    Write-Host "Copied jar to the clipboard: $($builtJar.FullName)"
} finally {
    [System.IO.File]::WriteAllBytes($propertiesFile, $originalProperties)
}
