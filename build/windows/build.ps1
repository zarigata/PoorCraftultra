param(
    [string]$Runtime = "win-x64",
    [switch]$SkipPublish
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

$projectRoot = Split-Path $PSScriptRoot -Parent
$projectFile = Join-Path $projectRoot "PoorCraftUltra.csproj"

if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
    throw "dotnet CLI not found. Install .NET 8 SDK before running this script."
}

if (-not (Test-Path $projectFile)) {
    throw "Unable to locate project file at $projectFile"
}

[xml]$csproj = Get-Content $projectFile
$versionNode = $csproj.Project.PropertyGroup | ForEach-Object { $_.Version } | Where-Object { $_ }
if (-not $versionNode) {
    throw "Version property not found in PoorCraftUltra.csproj"
}
$version = $versionNode[0]

Write-Section "Preparing directories"
$publishDir = Join-Path $projectRoot "publish/windows"
$binDir = Join-Path $projectRoot "bin/Release"
$outputDir = Join-Path $PSScriptRoot "output"
$installerStagingDir = Join-Path $PSScriptRoot "installer-staging"

foreach ($path in @($publishDir, $outputDir, $installerStagingDir)) {
    if (Test-Path $path) {
        Remove-Item $path -Recurse -Force
    }
    New-Item -ItemType Directory -Path $path -Force | Out-Null
}

if (Test-Path $binDir) {
    Remove-Item $binDir -Recurse -Force
}

if (-not $SkipPublish) {
    Write-Section "Publishing self-contained build for $Runtime"
    $publishArgs = @(
        "publish",
        $projectFile,
        "-c", "Release",
        "-r", $Runtime,
        "--self-contained", "true",
        "-p:PublishSingleFile=false",
        "-p:PublishTrimmed=false",
        "-o", $publishDir
    )

    dotnet @publishArgs
}

Write-Section "Copying additional files"
$readmePath = Join-Path $projectRoot "README.md"
$licensePath = Join-Path $projectRoot "LICENSE"
if (Test-Path $readmePath) {
    Copy-Item $readmePath -Destination $publishDir -Force
}
if (Test-Path $licensePath) {
    Copy-Item $licensePath -Destination $publishDir -Force
}
$logsDir = Join-Path $publishDir "logs"
if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

Write-Section "Verifying publish output"
$exePath = Join-Path $publishDir "PoorCraftUltra.exe"
if (-not (Test-Path $exePath)) {
    throw "Expected executable not found: $exePath"
}

$glfwLib = Get-ChildItem -Path $publishDir -Filter "glfw3.dll" -Recurse -ErrorAction SilentlyContinue
if (-not $glfwLib) {
    throw "Silk.NET native dependency 'glfw3.dll' not found in publish directory."
}

$publishedFiles = Get-ChildItem -Path $publishDir -Recurse -File -ErrorAction SilentlyContinue
$sizeBytes = ($publishedFiles | Measure-Object -Property Length -Sum -ErrorAction SilentlyContinue).Sum
$sizeMB = if ($sizeBytes) { [Math]::Round($sizeBytes / 1MB, 2) } else { 0 }
Write-Host ("Published {0} files totaling {1} MB" -f $publishedFiles.Count, $sizeMB)

Write-Section "Creating portable ZIP"
$portableZip = Join-Path $outputDir ("PoorCraftUltra-v{0}-windows-x64-portable.zip" -f $version)
Compress-Archive -Path (Join-Path $publishDir '*') -DestinationPath $portableZip -Force
Write-Host "Created portable archive: $portableZip"

Write-Section "Preparing installer staging directory"
Copy-Item -Path (Join-Path $publishDir '*') -Destination $installerStagingDir -Recurse -Force
Write-Host "Installer staging ready at $installerStagingDir"

Write-Section "Summary"
Write-Host "Version: $version"
Write-Host "Publish directory: $publishDir"
Write-Host "Portable ZIP: $portableZip"
Write-Host "Installer staging: $installerStagingDir"
