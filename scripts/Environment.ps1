Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$LocalConfigPath = Join-Path $ProjectRoot '.local/environment.json'
if (!(Test-Path -LiteralPath $LocalConfigPath)) {
    throw 'Missing .local/environment.json. Follow docs/DEVELOPMENT.md to configure this machine.'
}
$LocalConfig = Get-Content -LiteralPath $LocalConfigPath -Raw | ConvertFrom-Json
$JavaHome = $LocalConfig.javaHome
if (!(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/javac.exe'))) {
    throw "JDK not found at $JavaHome. Configure a 64-bit JDK 25 in .local/environment.json."
}
$JavaRelease = Get-Content -LiteralPath (Join-Path $JavaHome 'release') -Raw
if ($JavaRelease -notmatch 'JAVA_VERSION="25(?:\.|\")') { throw 'This project requires JDK 25.' }
$env:JAVA_HOME = $JavaHome
$env:PATH = (Join-Path $JavaHome 'bin') + [IO.Path]::PathSeparator + $env:PATH
$env:GRADLE_USER_HOME = Join-Path $ProjectRoot '.gradle-user-home'

function Invoke-ProjectGradle {
    param([string[]]$Tasks)
    Push-Location $ProjectRoot
    try {
        & (Join-Path $ProjectRoot 'gradlew.bat') @Tasks '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE." }
    } finally { Pop-Location }
}

function Get-ProjectProperties {
    $Properties = @{}
    foreach ($Line in Get-Content -LiteralPath (Join-Path $ProjectRoot 'gradle.properties')) {
        if ($Line -match '^([a-z_]+)=(.*)$') { $Properties[$Matches[1]] = $Matches[2] }
    }
    return $Properties
}

function Get-DevInstance {
    $Instance = Join-Path $LocalConfig.prismInstances $LocalConfig.prismInstanceId
    $Marker = Join-Path $Instance '.covenant-dev.json'
    if (!(Test-Path -LiteralPath $Marker)) { throw 'Run scripts/Setup-Prism.ps1 first.' }
    $Owner = Get-Content -LiteralPath $Marker -Raw | ConvertFrom-Json
    if ($Owner.projectRoot -ne $ProjectRoot) { throw 'Instance belongs to another project checkout.' }
    $Pack = Get-Content -LiteralPath (Join-Path $Instance 'mmc-pack.json') -Raw | ConvertFrom-Json
    $Properties = Get-ProjectProperties
    foreach ($Requirement in @(@('net.minecraft', $Properties.minecraft_version), @('net.neoforged', $Properties.neo_version))) {
        $Component = @($Pack.components | Where-Object uid -eq $Requirement[0])
        if ($Component.Count -ne 1 -or $Component[0].version -ne $Requirement[1]) {
            throw "Prism component $($Requirement[0]) differs from gradle.properties. Update the instance explicitly."
        }
    }
    return $Instance
}
