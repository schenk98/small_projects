$ErrorActionPreference = "Stop"

function Test-PortAvailable {
    param([int]$Port)

    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
        $listener.Start()
        $listener.Stop()
        return $true
    }
    catch {
        return $false
    }
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

Write-Host "==> Checking required tools..."
Get-Command docker -ErrorAction Stop | Out-Null
Get-Command mvn -ErrorAction Stop | Out-Null

$appPort = 8080
if (-not (Test-PortAvailable -Port $appPort)) {
    Write-Host "Port 8080 is already in use; trying next free port..."
    $appPort = 8081
    while (-not (Test-PortAvailable -Port $appPort)) {
        $appPort++
    }
}

$env:APP_PORT = $appPort

Write-Host "==> Building the Spring Boot application jar..."
mvn -q -DskipTests clean package

Write-Host "==> Stopping any previous local stack..."
docker compose down --remove-orphans

Write-Host "==> Starting PostgreSQL + web app on http://localhost:$appPort ..."
docker compose up --build -d

Write-Host ""
Write-Host "The app is running."
Write-Host "Open: http://localhost:$appPort"
Write-Host "PostgreSQL: localhost:5432"
Write-Host ""
Write-Host "Useful commands:"
Write-Host "  docker compose logs -f app"
Write-Host "  docker compose down"
