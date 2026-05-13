# API smoke: auth register -> MailHog verification email -> verify -> login -> shop purchase -> inventory -> progress + notification prefs.

$ErrorActionPreference = "Stop"

$email = "e2e$(Get-Date -Format yyyyMMddHHmmss)@example.com"
$password = "Abcde1"

Write-Host "Registering user $email"
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/register" -ContentType "application/json" -Body (@{ email = $email; password = $password } | ConvertTo-Json) | Out-Null

Start-Sleep -Seconds 1
$mailJson = Invoke-RestMethod -Method Get -Uri "http://localhost:8025/api/v2/messages"
$msg = $mailJson.items | Where-Object { $_.Content.Headers.To -match $email } | Select-Object -First 1
if (-not $msg) { throw "Verification email not found" }
$token = [regex]::Match($msg.Content.Body, "token=([^\s]+)").Groups[1].Value
if (-not $token) { throw "Verification token not found" }

Invoke-RestMethod -Method Get -Uri ("http://localhost:8080/auth/verify-email?token=" + $token) | Out-Null
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/login" -ContentType "application/json" -Body (@{ email = $email; password = $password } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.accessToken)" }

$shop = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/shop/items" -Headers $headers
if ($shop.Count -lt 1) { throw "Shop empty" }

$consumable = ($shop | Where-Object { $_.type -eq "CONSUMABLE" } | Select-Object -First 1).code
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/shop/purchase" -Headers $headers -ContentType "application/json" -Body (@{ itemCode = $consumable } | ConvertTo-Json) | Out-Null

$inventory = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/inventory" -Headers $headers
if (-not ($inventory | Where-Object { $_.itemCode -eq $consumable })) { throw "Inventory purchase failed" }

$progress = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/progress/summary" -Headers $headers
if (-not $progress.dailyChallenges) { throw "Progress summary missing daily challenges" }
if ($progress.dailyChallenges.Count -ne 3) { throw "Expected exactly 3 daily challenges" }
if (-not $progress.achievements) { throw "Progress summary missing achievements" }
if (-not $progress.recentActivity) { throw "Progress summary missing recent activity" }

$notificationPrefs = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/notification-preferences" -Headers $headers
if ($notificationPrefs.lowHungerEnabled -ne $false) { throw "Expected default low-hunger toggle to be false" }
if ($notificationPrefs.dailyAiSummaryEnabled -ne $false) { throw "Expected default daily summary toggle to be false" }

$updatedPrefs = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/notification-preferences" -Headers $headers -ContentType "application/json" -Body (@{
    lowHungerEnabled = $true
    dailyAiSummaryEnabled = $true
} | ConvertTo-Json)
if ($updatedPrefs.lowHungerEnabled -ne $true) { throw "Failed to enable low-hunger toggle" }
if ($updatedPrefs.dailyAiSummaryEnabled -ne $true) { throw "Failed to enable daily summary toggle" }

Write-Host "E2E smoke passed"
