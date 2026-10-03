param(
    [Parameter(Mandatory=$true)][string]$Instance,
    [string]$MinecraftRoot='F:\game\异界战斗幻想\.minecraft',
    [string]$JavaHome='D:\APPs\Java\java21',
    [string]$Artifacts='D:\NEWMODS',
    [string]$ModId='textstudio',
    [Parameter(Mandatory=$true)][string]$Node,
    [string]$Version='26.10.3',
    [int]$MemoryGB=0
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$gameDir=Join-Path $MinecraftRoot "versions\$Instance"
$metadata=Join-Path $gameDir "$Instance.json"
if (!(Test-Path -LiteralPath $metadata)) { throw "Installed version metadata missing: $metadata" }
$model=Get-Content -Raw -Encoding UTF8 -LiteralPath $metadata | ConvertFrom-Json
if ($model.inheritsFrom) { throw 'Use an installed version with merged metadata, no downloads are performed.' }
$runId=Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$workspace=Join-Path $PSScriptRoot "..\.gradle\migration\runtime\$Instance\$runId"
New-Item -ItemType Directory -Path $workspace -Force | Out-Null
$workspace=[IO.Path]::GetFullPath($workspace)
$nativeDir=Join-Path $workspace 'natives'
New-Item -ItemType Directory -Path $nativeDir -Force | Out-Null
$libraryDir=Join-Path $MinecraftRoot 'libraries'

function Test-Rules($rules) {
    if (!$rules) { return $true }
    $allowed=$false
    foreach ($rule in $rules) {
        $matches=$true
        if ($rule.os) {
            if ($rule.os.name -and $rule.os.name -ne 'windows') { $matches=$false }
            if ($rule.os.arch -and 'amd64' -notmatch $rule.os.arch) { $matches=$false }
            if ($rule.os.version -and [Environment]::OSVersion.Version.ToString() -notmatch $rule.os.version) { $matches=$false }
        }
        if ($rule.features) {
            foreach ($property in $rule.features.PSObject.Properties) {
                if ($property.Value -eq $true) { $matches=$false }
            }
        }
        if ($matches) { $allowed=$rule.action -eq 'allow' }
    }
    return $allowed
}
function Get-LibraryPath($library) {
    if ($library.downloads.artifact.path) { return Join-Path $libraryDir $library.downloads.artifact.path }
    $parts=$library.name.Split(':')
    $file="$($parts[1])-$($parts[2])"
    if ($parts.Length -gt 3) { $file += "-$($parts[3])" }
    return Join-Path $libraryDir "$($parts[0].Replace('.','/'))/$($parts[1])/$($parts[2])/$file.jar"
}
$classpath=New-Object 'System.Collections.Generic.List[string]'
foreach ($library in $model.libraries) {
    if (!(Test-Rules $library.rules)) { continue }
    $file=Get-LibraryPath $library
    if (!(Test-Path -LiteralPath $file)) { throw "Installed library missing (not downloaded): $file" }
    if (!$classpath.Contains($file)) { $classpath.Add($file) }
    if ($file -match 'natives-windows') {
        $zip=[IO.Compression.ZipFile]::OpenRead($file)
        try {
            foreach($entry in $zip.Entries) {
                if ($entry.FullName -match '\.dll$') {
                    [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,(Join-Path $nativeDir $entry.Name),$true)
                }
            }
        } finally { $zip.Dispose() }
    }
}
$versionJar=Join-Path $gameDir "$Instance.jar"
if (!(Test-Path -LiteralPath $versionJar)) { throw "Installed game JAR missing: $versionJar" }
$classpath.Add($versionJar)
$variables=@{
    natives_directory=$nativeDir; launcher_name='CodexLocalVerification'; launcher_version='1';
    classpath=($classpath -join ';'); classpath_separator=';'; library_directory=$libraryDir;
    version_name=$Instance; game_directory=$gameDir; assets_root=(Join-Path $MinecraftRoot 'assets');
    assets_index_name=$model.assetIndex.id; auth_player_name='CodexVerify'; auth_uuid='0000000000003001998f501bc686b6bb';
    auth_access_token='0'; clientid=''; auth_xuid=''; user_type='legacy'; version_type='release';
    resolution_width='1280'; resolution_height='720'
}
function Expand-Argument([string]$value) {
    return [regex]::Replace($value,'\$\{([^}]+)\}',{
        param($match)
        $key=$match.Groups[1].Value
        if (!$variables.ContainsKey($key)) { throw "Unsupported launch variable: $key" }
        return [string]$variables[$key]
    })
}
function Get-Arguments($arguments) {
    foreach ($argument in $arguments) {
        if ($argument -is [string]) { Expand-Argument $argument }
        elseif (Test-Rules $argument.rules) {
            foreach ($value in $argument.value) { Expand-Argument $value }
        }
    }
}
if ($MemoryGB -le 0) { $MemoryGB=if ($Node -eq '1.20.1-forge') {8} else {4} }
$launchArgs=@("-Xmx${MemoryGB}G",'-Dfile.encoding=UTF-8','-Dmixin.debug.verbose=true')
$launchArgs += @(Get-Arguments $model.arguments.jvm)
$launchArgs += $model.mainClass
$launchArgs += @(Get-Arguments $model.arguments.game)
$argfile=Join-Path $workspace 'client.args'
$escaped=$launchArgs | ForEach-Object { '"'+$_.Replace('\','\\').Replace('"','\"')+'"' }
[IO.File]::WriteAllLines($argfile,$escaped,(New-Object Text.UTF8Encoding($false)))

# Install only the two matching artifacts, keeping replaced JARs outside the mods directory.
$mods=Join-Path $gameDir 'mods'
$backup=Join-Path $gameDir ('codex-migration-backup\'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $mods,$backup -Force | Out-Null
$loader=$Node.Substring($Node.IndexOf('-')+1)
$minecraft=$Node.Substring(0,$Node.IndexOf('-'))
foreach ($id in @('kineticcore',$ModId)) {
    $artifact=Join-Path $Artifacts "$id-$loader-$minecraft-$Version.jar"
    if (!(Test-Path -LiteralPath $artifact)) { throw "Matching release JAR missing: $artifact" }
    foreach($old in Get-ChildItem -LiteralPath $mods -Filter "*$id*.jar") {
        if($old.Name -eq "$id-validation.jar") { continue }
        $source=[IO.Path]::GetFullPath($old.FullName)
        if (!$source.StartsWith([IO.Path]::GetFullPath($mods)+'\',[StringComparison]::OrdinalIgnoreCase)) { throw 'Backup source escaped instance mods directory' }
        Move-Item -LiteralPath $source -Destination (Join-Path $backup $old.Name)
    }
    Copy-Item -LiteralPath $artifact -Destination $mods
}
$commandLine=($launchArgs | ForEach-Object { '"'+$_.Replace('"','\"')+'"' }) -join ' '
if ($commandLine.Length -gt 30000) { throw 'Installed launch arguments exceed the safe Windows command line limit.' }
$process=Start-Process -FilePath (Join-Path $JavaHome 'bin\java.exe') -ArgumentList $commandLine -WorkingDirectory $gameDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $workspace 'stdout.log') -RedirectStandardError (Join-Path $workspace 'stderr.log') -PassThru
[PSCustomObject]@{ProcessId=$process.Id; Instance=$Instance; MemoryGB=$MemoryGB; Backup=$backup; Logs=$workspace} | ConvertTo-Json
