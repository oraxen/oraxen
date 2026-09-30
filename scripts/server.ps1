# Starts a local Paper/Folia server for a version used by VersionLoadingTest.
# Usage: ./scripts/server.ps1 --version Paper/26.2 [--no-reset]
$ErrorActionPreference = 'Stop'

function Show-Usage {
    Write-Host 'Usage: ./scripts/server.ps1 --version <Paper|Folia>/<version> [--no-reset]'
    Write-Host '  --version, -v  Server and Minecraft version, for example Paper/26.2'
    Write-Host '  --no-reset     Keep the existing plugins and world files'
    Write-Host '  --help, -h     Show this help'
}

$versionSpec = ''
$noReset = $false
for ($i = 0; $i -lt $args.Count; $i++) {
    switch -Regex ($args[$i]) {
        '^(--version|-v)$' {
            if ($i + 1 -ge $args.Count -or -not $args[$i + 1]) { throw "$($args[$i]) requires a value" }
            $versionSpec = $args[++$i]
            break
        }
        '^--version=(.*)$' {
            $versionSpec = $Matches[1]
            if (-not $versionSpec) { throw '--version requires a value' }
            break
        }
        '^--no-reset$' { $noReset = $true; break }
        '^(--help|-h)$' { Show-Usage; exit 0 }
        default { throw "Unknown option '$($args[$i])' (use --help for usage)" }
    }
}
if (-not $versionSpec) { throw '--version is required' }
if ($versionSpec -cnotmatch '^(Paper|Folia)/([0-9]+(?:\.[0-9]+)*)$') {
    throw 'Version must look like Paper/26.2 or Folia/1.21.11'
}
$serverName = $Matches[1]
$mcVersion = $Matches[2]
$projectName = $serverName.ToLowerInvariant()
$projectDir = Split-Path -Parent $PSScriptRoot
$serversDir = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'Oraxen/Servers'
$serverDir = Join-Path (Join-Path $serversDir $serverName) $mcVersion
$serverJar = Join-Path $serverDir 'server.jar'

# The version is restricted to digits and dots. Confirm the reset target is below Servers.
$serversRoot = [System.IO.Path]::GetFullPath($serversDir).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
if (-not [System.IO.Path]::GetFullPath($serverDir).StartsWith($serversRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Server directory is outside $serversDir"
}

$requiredJavaVersion = if ($mcVersion.StartsWith('26.')) { '25' } else { '21' }
function Test-JavaVersion([string]$javaCommand, [string]$expectedVersion) {
    if (-not (Test-Path -LiteralPath $javaCommand -PathType Leaf)) { return $false }
    try {
        $startInfo = New-Object System.Diagnostics.ProcessStartInfo
        $startInfo.FileName = $javaCommand
        $startInfo.Arguments = '-version'
        $startInfo.UseShellExecute = $false
        $startInfo.RedirectStandardError = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.CreateNoWindow = $true
        $process = [System.Diagnostics.Process]::Start($startInfo)
        try {
            $versionOutput = $process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd()
            $process.WaitForExit()
        } finally { $process.Dispose() }
        return $versionOutput -match ('version\s+"' + [regex]::Escape($expectedVersion) + '(?![0-9])')
    } catch { return $false }
}

$javaCommand = $null
foreach ($name in @("JAVA_${requiredJavaVersion}_HOME", "JDK_${requiredJavaVersion}_HOME", "JAVA${requiredJavaVersion}_HOME", "JDK${requiredJavaVersion}_HOME", 'JAVA_HOME')) {
    $javaHome = [Environment]::GetEnvironmentVariable($name)
    if ($javaHome) {
        $candidate = Join-Path $javaHome 'bin/java.exe'
        if (Test-JavaVersion $candidate $requiredJavaVersion) { $javaCommand = $candidate; break }
    }
}
if (-not $javaCommand) {
    $pathJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($pathJava -and (Test-JavaVersion $pathJava.Source $requiredJavaVersion)) { $javaCommand = $pathJava.Source }
}
if (-not $javaCommand) {
    throw "Java $requiredJavaVersion is required for $mcVersion; set JAVA_${requiredJavaVersion}_HOME or JAVA_HOME"
}

function Get-OraxenJar {
    $libsDir = Join-Path $projectDir 'build/libs'
    $jars = @(Get-ChildItem -LiteralPath $libsDir -Filter 'oraxen-*.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Sort-Object LastWriteTimeUtc -Descending)
    if ($jars.Count -eq 0) {
        Write-Host 'No Oraxen jar found; building shadowJar...'
        $wrapper = Join-Path $projectDir 'gradlew.bat'
        if (Test-Path -LiteralPath $wrapper -PathType Leaf) {
            & $wrapper -p $projectDir shadowJar | Out-Host
        } else {
            & gradle -p $projectDir shadowJar | Out-Host
        }
        if ($LASTEXITCODE -ne 0) { throw "Gradle shadowJar failed with exit code $LASTEXITCODE" }
        $jars = @(Get-ChildItem -LiteralPath $libsDir -Filter 'oraxen-*.jar' -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
            Sort-Object LastWriteTimeUtc -Descending)
    }
    if ($jars.Count -eq 0) { throw "Could not find the built Oraxen jar in $libsDir" }
    return $jars[0].FullName
}

$oraxenJar = Get-OraxenJar
$knownDownloads = @{
    'paper/26.3' = 'https://fill-data.papermc.io/v1/objects/ddfea9cddc8f40e33080d4d9a3e9818fd1f9ea33f6100b42ac228ef80eb7ceed/paper-26.3-31.jar'
    'paper/26.2' = 'https://fill-data.papermc.io/v1/objects/36fee4f3a7020eb2e2d6f8d70d849beaf0f024d86f09302b9ccf2d96f266127e/paper-26.2-71.jar'
    'paper/26.1.2' = 'https://fill-data.papermc.io/v1/objects/d30fae0c74092b10855f0412ca6b265c60301a013d34bc28a2a41bf5682dd80b/paper-26.1.2-69.jar'
    'paper/1.21.11' = 'https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar'
    'paper/1.21.10' = 'https://fill-data.papermc.io/v1/objects/158703f75a26f842ea656b3dc6d75bf3d1ec176b97a2c36384d0b80b3871af53/paper-1.21.10-130.jar'
    'paper/1.21.8' = 'https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar'
    'paper/1.21.5' = 'https://fill-data.papermc.io/v1/objects/2ae6ae22adf417699746e0f89fc2ef6cb6ee050a5f6608cee58f0535d60b509e/paper-1.21.5-114.jar'
    'paper/1.21.4' = 'https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar'
    'paper/1.21.3' = 'https://fill-data.papermc.io/v1/objects/87e973e1d338e869e7fdbc4b8fadc1579d7bb0246a0e0cf6e5700ace6c8bc17e/paper-1.21.3-83.jar'
    'paper/1.20.6' = 'https://fill-data.papermc.io/v1/objects/4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640/paper-1.20.6-151.jar'
    'paper/1.20.4' = 'https://fill-data.papermc.io/v1/objects/cabed3ae77cf55deba7c7d8722bc9cfd5e991201c211665f9265616d9fe5c77b/paper-1.20.4-499.jar'
    'paper/1.20.1' = 'https://fill-data.papermc.io/v1/objects/234a9b32098100c6fc116664d64e36ccdb58b5b649af0f80bcccb08b0255eaea/paper-1.20.1-196.jar'
    'folia/26.1.2' = 'https://fill-data.papermc.io/v1/objects/607afd1c3320008e1ffd2eaee6780ace4419d5f8c527b75e79f259be79ebf57b/folia-26.1.2-8.jar'
    'folia/1.21.11' = 'https://fill-data.papermc.io/v1/objects/f52c408490a0225611e67907a3ca19f7e6da2c6bc899e715d5f46844e7103c39/folia-1.21.11-14.jar'
}

if (-not (Test-Path -LiteralPath $serverJar -PathType Leaf)) {
    $downloadUrl = $knownDownloads["$projectName/$mcVersion"]
    if (-not $downloadUrl) {
        $buildsUrl = "https://api.papermc.io/v2/projects/$projectName/versions/$mcVersion/builds"
        try { $builds = Invoke-RestMethod -Uri $buildsUrl } catch { throw "Could not fetch $serverName $mcVersion build information: $_" }
        $build = @($builds.builds | Select-Object -Last 1)[0].build
        if (-not $build) { throw "Could not find a $serverName build for Minecraft $mcVersion" }
        $downloadUrl = "https://api.papermc.io/v2/projects/$projectName/versions/$mcVersion/builds/$build/downloads/$projectName-$mcVersion-$build.jar"
    }
    [void][System.IO.Directory]::CreateDirectory($serverDir)
    $temporaryJar = Join-Path $serverDir 'server.jar.download'
    Write-Host "Downloading $serverName $mcVersion..."
    try {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $temporaryJar
        Move-Item -LiteralPath $temporaryJar -Destination $serverJar -Force
    } catch {
        Remove-Item -LiteralPath $temporaryJar -Force -ErrorAction SilentlyContinue
        throw "Could not download $serverName $mcVersion`: $_"
    }
}

[void][System.IO.Directory]::CreateDirectory($serverDir)
if (-not $noReset) {
    Get-ChildItem -LiteralPath $serverDir -Force | Where-Object { $_.FullName -ne $serverJar } |
        Remove-Item -Recurse -Force
}
$pluginsDir = Join-Path $serverDir 'plugins'
[void][System.IO.Directory]::CreateDirectory($pluginsDir)
[System.IO.File]::WriteAllText((Join-Path $serverDir 'eula.txt'), "eula=true`n", (New-Object System.Text.UTF8Encoding($false)))
Copy-Item -LiteralPath $oraxenJar -Destination (Join-Path $pluginsDir 'Oraxen.jar') -Force

Write-Host "Starting $serverName $mcVersion"
Write-Host "Server directory: $serverDir"
Write-Host "Oraxen jar: $oraxenJar"
Push-Location $serverDir
try {
    & $javaCommand -Xmx1G -jar server.jar --nogui --port 25568
    $serverExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $serverExitCode
