param(
    [ValidateSet('doctor', 'build', 'test', 'verify', 'deploy', 'prism', 'client', 'server', 'data')]
    [string]$Task = 'build'
)
. "$PSScriptRoot/Environment.ps1"

function Publish-DevMod {
    Invoke-ProjectGradle -Tasks @('build')
    $Instance = Get-DevInstance
    $Properties = Get-ProjectProperties
    $JarName = "$($Properties.mod_id)-$($Properties.mod_version).jar"
    $Source = Join-Path $ProjectRoot "build/libs/$JarName"
    if (!(Test-Path -LiteralPath $Source)) { throw "Missing build output: $Source" }
    # This marker-owned development instance always has exactly one deployed Covenant build.
    $Mods = Join-Path $Instance 'minecraft/mods'
    New-Item -ItemType Directory -Force -Path $Mods | Out-Null
    $GeckoJarName = "geckolib-neoforge-$($Properties.minecraft_version)-$($Properties.geckolib_version).jar"
    $GeckoSource = Join-Path $ProjectRoot "build/runtime-mods/$GeckoJarName"
    if (!(Test-Path -LiteralPath $GeckoSource)) { throw "Missing staged dependency: $GeckoSource" }
    $GeckoDestination = Join-Path $Mods $GeckoJarName
    $OtherGecko = @(Get-ChildItem -LiteralPath $Mods -Filter 'geckolib*.jar' -File |
        Where-Object Name -ne $GeckoJarName)
    if ($OtherGecko.Count -gt 0) {
        throw "Conflicting GeckoLib JAR(s) in the dev instance: $($OtherGecko.Name -join ', '). Remove them explicitly before deploying the pinned $GeckoJarName."
    }
    $GeckoHash = (Get-FileHash -LiteralPath $GeckoSource -Algorithm SHA256).Hash
    if (Test-Path -LiteralPath $GeckoDestination) {
        if ((Get-FileHash -LiteralPath $GeckoDestination -Algorithm SHA256).Hash -ne $GeckoHash) {
            throw "Existing $GeckoJarName differs from the resolved dependency. Remove it explicitly before redeploying."
        }
    } else {
        Copy-Item -LiteralPath $GeckoSource -Destination $GeckoDestination
    }
    if ((Get-FileHash -LiteralPath $GeckoDestination -Algorithm SHA256).Hash -ne $GeckoHash) {
        throw 'GeckoLib deployment checksum mismatch.'
    }
    $Previous = @(Get-ChildItem -LiteralPath $Mods -Filter "$($Properties.mod_id)-*.jar" -File)
    $Previous | Remove-Item
    Copy-Item -LiteralPath $Source -Destination (Join-Path $Mods $JarName)
    $SourceHash = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
    $DeployedHash = (Get-FileHash -LiteralPath (Join-Path $Mods $JarName) -Algorithm SHA256).Hash
    if ($SourceHash -ne $DeployedHash) { throw 'Deployment checksum mismatch.' }
    Write-Host "Deployed $JarName to $Mods (SHA256 $DeployedHash)"
    Write-Host "GeckoLib dependency: $GeckoJarName (SHA256 $GeckoHash)"
}

switch ($Task) {
    doctor {
        & (Join-Path $JavaHome 'bin/java.exe') -version
        & (Join-Path $JavaHome 'bin/javac.exe') -version
        if (!(Test-Path -LiteralPath $LocalConfig.prismExe)) { throw 'Prism executable missing.' }
        Invoke-ProjectGradle -Tasks @('--version')
        Write-Host "Project: $ProjectRoot"
        Write-Host "Prism: $($LocalConfig.prismExe)"
        Write-Host "Instance: $(Get-DevInstance)"
    }
    build { Invoke-ProjectGradle -Tasks @('build') }
    test { Invoke-ProjectGradle -Tasks @('runGameTestServer') }
    verify { Invoke-ProjectGradle -Tasks @('build', 'runGameTestServer') }
    deploy { Publish-DevMod }
    prism {
        Publish-DevMod
        Start-Process -FilePath $LocalConfig.prismExe -ArgumentList @('--launch', $LocalConfig.prismInstanceId) -WindowStyle Hidden
    }
    client { Invoke-ProjectGradle -Tasks @('runClient') }
    server { Invoke-ProjectGradle -Tasks @('runServer') }
    data { Invoke-ProjectGradle -Tasks @('runData') }
}
