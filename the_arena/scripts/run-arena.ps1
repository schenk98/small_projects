param(
    [switch]$SkipBuild,
    [switch]$AutoStartDocker
)

$ErrorActionPreference = "Stop"
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot
Set-Location $projectRoot

function Require-Command([string]$command) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "Required command '$command' is not available on PATH."
    }
}

function Ensure-DockerDaemon([switch]$AutoStart) {
    cmd /c "docker info >nul 2>nul"
    if ($LASTEXITCODE -eq 0) {
        return
    }

    if (-not $AutoStart) {
        throw "Docker daemon is not running. Start Docker Desktop and run the script again, or use -AutoStartDocker."
    }

    Write-Host "Docker daemon is not running. Attempting to start Docker Desktop..."
    $dockerDesktopExe = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path $dockerDesktopExe)) {
        throw "Docker Desktop executable not found at '$dockerDesktopExe'. Start Docker Desktop manually."
    }

    Start-Process -FilePath $dockerDesktopExe | Out-Null

    $timeoutSeconds = 180
    $elapsed = 0
    while ($elapsed -lt $timeoutSeconds) {
        Start-Sleep -Seconds 3
        $elapsed += 3
        cmd /c "docker info >nul 2>nul"
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Docker daemon is ready."
            return
        }
    }

    throw "Docker Desktop started but daemon is still unavailable after $timeoutSeconds seconds."
}

function Wait-ContainerHealthy([string]$containerName, [int]$timeoutSeconds = 120) {
    $elapsed = 0
    while ($elapsed -lt $timeoutSeconds) {
        $status = docker inspect --format "{{.State.Health.Status}}" $containerName 2>$null
        if ($LASTEXITCODE -eq 0 -and $status -eq "healthy") {
            Write-Host "Container '$containerName' is healthy."
            return
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    throw "Timeout waiting for container '$containerName' to become healthy."
}

Write-Host "Checking prerequisites..."
Require-Command "docker"
Require-Command "mvn"
Ensure-DockerDaemon -AutoStart:$AutoStartDocker

Write-Host "Starting database containers..."
docker compose up -d mysql mongo
if ($LASTEXITCODE -ne 0) {
    throw "Failed to start docker compose services."
}

Write-Host "Waiting for MySQL and MongoDB health checks..."
Wait-ContainerHealthy "the-arena-mysql" 180
Wait-ContainerHealthy "the-arena-mongo" 180

if (-not $SkipBuild) {
    Write-Host "Running tests before startup..."
    mvn test
}

Write-Host "Starting The Arena application with docker profile..."
Write-Host "Press Ctrl+C to stop the app. Databases stay up."
mvn spring-boot:run "-Dspring-boot.run.profiles=docker"
