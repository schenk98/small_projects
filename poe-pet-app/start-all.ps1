$ErrorActionPreference = "Stop"

# Repo root = folder containing this script (works when launched from elsewhere)
$Root = if ($PSScriptRoot) { $PSScriptRoot } else { Get-Location }
Set-Location $Root

function Stop-ListenerOnPort {
    param([int] $PortNumber)
    try {
        $listenerPids = Get-NetTCPConnection -LocalPort $PortNumber -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $listenerPids) {
            if ($procId -and $procId -gt 0) {
                Write-Host "==> Stopping process $procId on port $PortNumber"
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
    }
    catch {
        Write-Host "Port $PortNumber - could not query or stop listeners (continuing)."
    }
}

Write-Host "==> Starting MongoDB + Mailhog via Docker Compose (from $Root)"
docker compose up -d mongodb mailhog

Write-Host "==> Waiting for MongoDB startup"
Start-Sleep -Seconds 3

Write-Host "==> Freeing dev ports 5173 (Vite) and 8080 (Spring)"
Stop-ListenerOnPort -PortNumber 5173
Stop-ListenerOnPort -PortNumber 8080

if (Test-Path ".\backend\pom.xml") {
    Write-Host "==> Compiling backend (ensures latest API e.g. reward-preview is on classpath)"
    Push-Location .\backend
    try {
        mvn -q -DskipTests compile
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "==> Backend not found (missing backend\pom.xml)"
}

if (Test-Path ".\frontend\package.json") {
    Write-Host "==> Starting frontend in a new window (npm install + dev)"
    $fe = Join-Path $Root "frontend"
    Start-Process powershell -ArgumentList @(
        "-NoExit", "-Command",
        "Set-Location `"$fe`"; npm install; npm run dev"
    )
}
else {
    Write-Host "==> Frontend not initialized (missing frontend\package.json)"
}

if (Test-Path ".\backend\pom.xml") {
    Write-Host "==> Starting backend in a new window (mvn spring-boot:run)"
    $be = Join-Path $Root "backend"
    Start-Process powershell -ArgumentList @(
        "-NoExit", "-Command",
        "Set-Location `"$be`"; mvn spring-boot:run"
    )
}

Write-Host ""
Write-Host "==> Done."
Write-Host "    - MongoDB + Mailhog: docker (see docker-compose.yml)"
Write-Host "    - Frontend: http://localhost:5173/"
Write-Host "    - Backend:  http://localhost:8080/"
Write-Host "    - MailHog UI: http://localhost:8025/"
Write-Host "See README.md section: Verify local stack"
