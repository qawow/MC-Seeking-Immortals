[CmdletBinding()]
param(
    [switch]$SkipVersionBumpCheck,
    [switch]$RecordStateOnly
)

$ErrorActionPreference = 'Stop'

function Get-RepoRoot {
    try {
        $root = (& git rev-parse --show-toplevel 2>$null)
        if ($LASTEXITCODE -eq 0 -and $root) {
            return (Resolve-Path -LiteralPath $root).Path
        }
    } catch {
        # Fall through to current directory when git is unavailable.
    }

    return (Resolve-Path -LiteralPath '.').Path
}

function Get-ModVersion {
    param([string]$Root)

    $propertiesPath = Join-Path $Root 'gradle.properties'
    if (-not (Test-Path -LiteralPath $propertiesPath)) {
        throw 'gradle.properties not found; cannot validate mod_version.'
    }

    $line = Get-Content -LiteralPath $propertiesPath -Encoding UTF8 |
        Where-Object { $_ -match '^mod_version\s*=' } |
        Select-Object -First 1

    if (-not $line) {
        throw 'mod_version is missing from gradle.properties.'
    }

    return ($line -replace '^mod_version\s*=\s*', '').Trim()
}

function Get-TrackedFileFingerprint {
    param(
        [string]$Root,
        [string[]]$Paths
    )

    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $builder = [System.Text.StringBuilder]::new()
        $files = New-Object System.Collections.Generic.List[string]

        foreach ($path in $Paths) {
            $fullPath = Join-Path $Root $path
            if (Test-Path -LiteralPath $fullPath -PathType Leaf) {
                $files.Add((Resolve-Path -LiteralPath $fullPath).Path)
            } elseif (Test-Path -LiteralPath $fullPath -PathType Container) {
                Get-ChildItem -LiteralPath $fullPath -Recurse -File |
                    ForEach-Object { $files.Add($_.FullName) }
            }
        }

        foreach ($fullPath in ($files | Sort-Object -Unique)) {
            $relative = Get-RelativePath -Root $Root -FullPath $fullPath
            [void]$builder.Append($relative).Append(':')
            $bytes = [System.IO.File]::ReadAllBytes($fullPath)
            $hashBytes = $sha.ComputeHash($bytes)
            [void]$builder.Append([BitConverter]::ToString($hashBytes).Replace('-', '').ToLowerInvariant()).AppendLine()
        }

        $textBytes = [System.Text.Encoding]::UTF8.GetBytes($builder.ToString())
        return [BitConverter]::ToString($sha.ComputeHash($textBytes)).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Get-RelativePath {
    param(
        [string]$Root,
        [string]$FullPath
    )

    $rootPath = (Resolve-Path -LiteralPath $Root).Path.TrimEnd('\', '/')
    $full = (Resolve-Path -LiteralPath $FullPath).Path

    if ($full.StartsWith($rootPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $full.Substring($rootPath.Length).TrimStart('\', '/').Replace('\', '/')
    }

    $rootUri = [System.Uri]::new($rootPath + [System.IO.Path]::DirectorySeparatorChar)
    $fileUri = [System.Uri]::new($full)
    return [System.Uri]::UnescapeDataString($rootUri.MakeRelativeUri($fileUri).ToString()).Replace('\', '/')
}

function Get-ChangedPathsFromGit {
    param([string]$Root)

    try {
        $inside = (& git -C $Root rev-parse --is-inside-work-tree 2>$null)
        if ($LASTEXITCODE -ne 0 -or $inside.Trim() -ne 'true') {
            return $null
        }

        $porcelain = & git -C $Root status --porcelain=v1 -uall
        if ($LASTEXITCODE -ne 0) {
            return $null
        }

        $paths = New-Object System.Collections.Generic.List[string]
        foreach ($line in $porcelain) {
            if (-not $line -or $line.Length -lt 4) {
                continue
            }

            $path = $line.Substring(3).Trim()
            if ($path -match ' -> ') {
                $path = ($path -split ' -> ')[-1].Trim()
            }
            $path = $path.Trim('"').Replace('\', '/')
            if ($path) {
                $paths.Add($path)
            }
        }

        return $paths.ToArray()
    } catch {
        return $null
    }
}

function Test-ShippablePath {
    param([string]$Path)

    $normalized = $Path.Replace('\', '/')
    if ($normalized -eq 'gradle.properties') {
        return $false
    }

    return (
        $normalized -like 'src/main/java/*' -or
        $normalized -like 'src/main/resources/*' -or
        $normalized -like 'src/main/generated/*' -or
        $normalized -eq 'build.gradle' -or
        $normalized -eq 'settings.gradle' -or
        $normalized -like 'gradle/*' -or
        $normalized -like 'scripts/*'
    )
}

function Test-NetworkPath {
    param([string]$Path)

    $normalized = $Path.Replace('\', '/')
    return $normalized -like 'src/main/java/com/xunxian/seekingimmortals/network/*'
}

function Test-ModVersionChangedInGit {
    param([string]$Root)

    $diff = & git -C $Root diff -- gradle.properties
    if ($LASTEXITCODE -ne 0) {
        return $false
    }

    return ($diff | Where-Object { $_ -match '^[+-]mod_version\s*=' }).Count -gt 0
}

$repoRoot = Get-RepoRoot
$modVersion = Get-ModVersion -Root $repoRoot
$trackedPaths = @(
    'src/main/java',
    'src/main/resources',
    'src/main/generated',
    'build.gradle',
    'settings.gradle',
    'gradle.properties',
    'gradle',
    'scripts'
)

if ($SkipVersionBumpCheck) {
    Write-Warning 'AI preflight version bump check skipped by -SkipVersionBumpCheck.'
    exit 0
}

if ($modVersion -notmatch '^0\.1\.\d+$') {
    Write-Error "mod_version '$modVersion' does not match required 0.1.X format."
}

$changedPaths = Get-ChangedPathsFromGit -Root $repoRoot
$shippableChanges = @()
$networkChanges = @()
$versionChanged = $false

if ($null -ne $changedPaths) {
    $shippableChanges = @($changedPaths | Where-Object { Test-ShippablePath $_ })
    $networkChanges = @($changedPaths | Where-Object { Test-NetworkPath $_ })
    $versionChanged = Test-ModVersionChangedInGit -Root $repoRoot

    if ($shippableChanges.Count -gt 0 -and -not $versionChanged) {
        Write-Error @"
AI preflight failed: shippable code/resource/build changes exist, but gradle.properties did not change mod_version.

Required flow:
  1. Bump mod_version in gradle.properties by one 0.1.X patch version.
  2. Re-run ./gradlew build.

Changed shippable paths:
$($shippableChanges -join [Environment]::NewLine)
"@
    }

    if ($networkChanges.Count -gt 0) {
        Write-Warning @"
Network package changed. If packet fields, field order, decode/encode format, or compatibility changed, bump ModNetwork.PROTOCOL_VERSION before release.
Network paths:
$($networkChanges -join [Environment]::NewLine)
"@
    }
} else {
    Write-Warning 'Git status is unavailable; using last successful preflight fingerprint as a fallback.'
}

$stateDir = Join-Path $repoRoot '.gradle/ai-preflight'
$statePath = Join-Path $stateDir 'last-success.json'
$fingerprint = Get-TrackedFileFingerprint -Root $repoRoot -Paths $trackedPaths

if ($RecordStateOnly) {
    New-Item -ItemType Directory -Force -Path $stateDir | Out-Null
    [ordered]@{
        modVersion = $modVersion
        fingerprint = $fingerprint
        checkedAt = (Get-Date).ToString('s')
    } | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8

    Write-Host "AI preflight state recorded: mod_version=$modVersion"
    exit 0
}

$lastState = $null

if (Test-Path -LiteralPath $statePath) {
    try {
        $lastState = Get-Content -LiteralPath $statePath -Raw -Encoding UTF8 | ConvertFrom-Json
    } catch {
        Write-Warning 'Could not read previous AI preflight state; rewriting it after this successful check.'
    }
}

if ($null -ne $lastState -and $lastState.fingerprint -ne $fingerprint -and $lastState.modVersion -eq $modVersion) {
    Write-Error @"
AI preflight failed: shippable tracked files changed since the last successful build fingerprint, but mod_version is still $modVersion.

Required flow:
  1. Bump mod_version in gradle.properties by one 0.1.X patch version.
  2. Re-run ./gradlew build.

If this build is intentionally docs-only or an emergency rebuild, rerun with:
  ./gradlew build -PaiSkipVersionBumpCheck=true
"@
}

Write-Host "AI preflight passed: mod_version=$modVersion"
