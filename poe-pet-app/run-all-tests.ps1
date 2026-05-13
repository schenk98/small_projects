$ErrorActionPreference = "Stop"
$Root = if ($PSScriptRoot) { $PSScriptRoot } else { Get-Location }

Write-Host "==> Backend: mvn test (wrapper if present)"
Push-Location (Join-Path $Root "backend")
try {
    if (Test-Path ".\mvnw.cmd") {
        .\mvnw.cmd -q test
    } else {
        mvn -q test
    }
}
finally {
    Pop-Location
}

Write-Host "==> Notification SOAP service: mvn test"
Push-Location (Join-Path $Root "notification-soap-service")
try {
    mvn -q test
}
finally {
    Pop-Location
}

Write-Host "==> Frontend: vitest + lint + production build"
$fe = Join-Path $Root "frontend"
Push-Location $fe
try {
    npm run test
    npm run lint
    npm run build
}
finally {
    Pop-Location
}

Write-Host "==> All automated tests completed (backend + notification service + frontend)."
Write-Host "    E2E API smoke (requires stack): pwsh -File tests/e2e/api-smoke.ps1"
Write-Host "    Full container stack smoke:    pwsh -File tests/e2e/container-stack-smoke.ps1"
