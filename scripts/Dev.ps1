param(
    [ValidateSet('doctor', 'build', 'deploy', 'prism', 'client', 'server', 'data')]
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
    $Previous = @(Get-ChildItem -LiteralPath $Mods -Filter "$($Properties.mod_id)-*.jar" -File)
    $Previous | Remove-Item
    Copy-Item -LiteralPath $Source -Destination (Join-Path $Mods $JarName)
    $SourceHash = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
    $DeployedHash = (Get-FileHash -LiteralPath (Join-Path $Mods $JarName) -Algorithm SHA256).Hash
    if ($SourceHash -ne $DeployedHash) { throw 'Deployment checksum mismatch.' }
    Write-Host "Deployed $JarName to $Mods (SHA256 $DeployedHash)"
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
    deploy { Publish-DevMod }
    prism {
        Publish-DevMod
        Start-Process -FilePath $LocalConfig.prismExe -ArgumentList @('--launch', $LocalConfig.prismInstanceId) -WindowStyle Hidden
    }
    client { Invoke-ProjectGradle -Tasks @('runClient') }
    server { Invoke-ProjectGradle -Tasks @('runServer') }
    data { Invoke-ProjectGradle -Tasks @('runData') }
}
