$ErrorActionPreference = "Stop"

$Root = if ($PSScriptRoot) { Resolve-Path (Join-Path $PSScriptRoot "..\..") } else { Get-Location }

function Assert-PortFree {
    param([int] $PortNumber)

    $listener = Get-NetTCPConnection -LocalPort $PortNumber -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($listener) {
        throw "Port $PortNumber is already in use by PID $($listener.OwningProcess). Stop the conflicting process before running container-stack-smoke."
    }
}

Push-Location $Root
try {
    Assert-PortFree -PortNumber 5173
    Assert-PortFree -PortNumber 8080
    Assert-PortFree -PortNumber 8081
    Assert-PortFree -PortNumber 1025
    Assert-PortFree -PortNumber 8025

    Write-Host "Building and starting full container stack"
    docker compose --profile dev up -d --build frontend backend mongodb postgres notification-soap-service mailhog

    Write-Host "Waiting for frontend and backend"
    $frontendReady = $false
    $backendReady = $false
    for ($i = 0; $i -lt 60; $i++) {
        if (-not $frontendReady) {
            try {
                $html = Invoke-WebRequest -Uri "http://localhost:5173" -TimeoutSec 3
                if ($html.StatusCode -eq 200 -and $html.Content -match "<div id=`"root`">") { $frontendReady = $true }
            } catch {}
        }
        if (-not $backendReady) {
            try {
                Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/forgot-password" -ContentType "application/json" -Body (@{ email = "nobody@example.com" } | ConvertTo-Json) | Out-Null
                $backendReady = $true
            } catch {}
        }
        if ($frontendReady -and $backendReady) { break }
        Start-Sleep -Seconds 2
    }

    if (-not $frontendReady) { throw "Frontend did not become ready" }
    if (-not $backendReady) { throw "Backend did not become ready" }

    & powershell -ExecutionPolicy Bypass -File ".\tests\e2e\api-smoke.ps1"
    Write-Host "Container stack smoke passed"
}
finally {
    Write-Host "Stopping container stack"
    docker compose stop frontend backend mongodb postgres notification-soap-service mailhog
    Pop-Location
}
