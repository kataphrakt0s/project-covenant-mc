. "$PSScriptRoot/Environment.ps1"
$InstanceId = $LocalConfig.prismInstanceId
if ($InstanceId -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple folder name for prismInstanceId.' }
$Instance = Join-Path $LocalConfig.prismInstances $InstanceId
if (Test-Path -LiteralPath $Instance) {
    $null = Get-DevInstance
    Write-Host "Existing project instance retained: $Instance"
    return
}
$Properties = Get-ProjectProperties
New-Item -ItemType Directory -Path (Join-Path $Instance 'minecraft/mods') -Force | Out-Null
$Pack = @{
    formatVersion = 1
    components = @(
        @{ uid = 'net.minecraft'; version = $Properties.minecraft_version; important = $true },
        @{ uid = 'net.neoforged'; version = $Properties.neo_version }
    )
}
$Pack | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $Instance 'mmc-pack.json') -Encoding utf8
$JavaPath = (Join-Path $JavaHome 'bin/javaw.exe').Replace('\', '/')
@"
[General]
ConfigVersion=1.3
InstanceType=OneSix
name=Project Covenant - Dev ($($Properties.minecraft_version))
iconKey=default
OverrideJavaLocation=true
AutomaticJava=false
JavaPath=$JavaPath
OverrideMemory=true
MinMemAlloc=512
MaxMemAlloc=4096
"@ | Set-Content -LiteralPath (Join-Path $Instance 'instance.cfg') -Encoding utf8
@{ projectRoot = $ProjectRoot } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Instance '.covenant-dev.json') -Encoding utf8
Write-Host "Created blank NeoForge development instance: $Instance"
