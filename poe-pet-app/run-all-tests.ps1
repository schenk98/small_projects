$ErrorActionPreference = "Stop"
$Root = if ($PSScriptRoot) { $PSScriptRoot } else { Get-Location }

Write-Host "==> Backend: mvn test"
Push-Location (Join-Path $Root "backend")
try {
    mvn -q test
}
finally {
    Pop-Location
}

Write-Host "==> Frontend: vitest + production build"
$fe = Join-Path $Root "frontend"
Push-Location $fe
try {
    npm run test
    npm run build
}
finally {
    Pop-Location
}

Write-Host "==> All automated tests completed (backend unit + frontend unit/build)."
Write-Host "    E2E API smoke (requires stack): pwsh -File tests/e2e/api-smoke.ps1"
