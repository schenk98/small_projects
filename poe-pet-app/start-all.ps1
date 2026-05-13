$ErrorActionPreference = "Stop"

# Repo root = folder containing this script (works when launched from elsewhere)
$Root = if ($PSScriptRoot) { $PSScriptRoot } else { Get-Location }
Set-Location $Root

function Import-EnvFile {
    param([string] $Path)

    if (!(Test-Path $Path)) {
        return
    }

    Write-Host "==> Loading env from $Path"
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (!$line -or $line.StartsWith("#")) { return }
        $parts = $line.Split("=", 2)
        if ($parts.Length -ne 2) { return }
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ($name) { Set-Item -Path "Env:$name" -Value $value }
    }
}

# Prefer local-only env file name.
Import-EnvFile -Path (Join-Path $Root ".env.local")

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

Write-Host "==> Starting MongoDB + PostgreSQL + Mailhog(dev profile) + notification SOAP service via Docker Compose (from $Root)"
docker compose --profile dev up -d mongodb postgres mailhog notification-soap-service

Write-Host "==> Waiting for database startup"
Start-Sleep -Seconds 3

Write-Host "==> Ensuring species shop items exist (non-destructive Mongo migration)"
try {
    docker exec poe-pet-mongodb mongosh -u admin -p admin123 --authenticationDatabase admin poe_pet /scripts/migrate-add-species-shop-items.js | Out-Null
}
catch {
    Write-Host "Mongo migration failed (continuing): $($_.Exception.Message)"
}

Write-Host "==> Freeing dev ports 5173 (Vite) and 8080 (Spring)"
Stop-ListenerOnPort -PortNumber 5173
Stop-ListenerOnPort -PortNumber 8080

if (Test-Path ".\backend\pom.xml") {
    Write-Host "==> Compiling backend (ensures latest API e.g. reward-preview is on classpath)"
    Push-Location .\backend
    try {
        if (Test-Path ".\mvnw.cmd") {
            .\mvnw.cmd -q -DskipTests compile
        }
        else {
            mvn -q -DskipTests compile
        }
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
    Write-Host "==> Starting backend in a new window (spring-boot:run)"
    $be = Join-Path $Root "backend"
    $mvnSpring = 'if (Test-Path .\mvnw.cmd) { .\mvnw.cmd spring-boot:run } else { mvn spring-boot:run }'
    Start-Process powershell -ArgumentList @(
        "-NoExit", "-Command",
        "Set-Location `"$be`"; $mvnSpring"
    )
}

Write-Host ""
Write-Host "==> Done."
Write-Host "    - MongoDB + PostgreSQL + Mailhog(dev profile) + notification SOAP service: docker (see docker-compose.yml)"
Write-Host "    - Frontend: http://localhost:5173/"
Write-Host "    - Backend:  http://localhost:8080/"
Write-Host "    - MailHog UI: http://localhost:8025/"
Write-Host "    - Notification SOAP WSDL: http://localhost:8081/ws/notifications.wsdl"
Write-Host "    - Full container stack: docker compose --profile dev up -d --build"
Write-Host "See README.md section: Verify local stack"
