$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginPage = Invoke-WebRequest -Uri "http://localhost:8080/login" -WebSession $session
Write-Host "LOGIN_LEN=$($loginPage.Content.Length)"
Write-Host "HAS_EMAIL_FIELD=$($loginPage.Content.Contains('name=\"email\"'))"
Write-Host "HAS_PASSWORD_FIELD=$($loginPage.Content.Contains('name=\"password\"'))"

$form = @{ email = 'teacher@readingtracker.local'; password = 'Teacher123!' }
$loginResponse = Invoke-WebRequest -Uri 'http://localhost:8080/login' -Method Post -Body $form -WebSession $session -SkipHttpErrorCheck
Write-Host "POST_STATUS=$($loginResponse.StatusCode)"
Write-Host "HAS_LOCATION=$($loginResponse.Headers.ContainsKey('Location'))"
Write-Host "LOCATION=$($loginResponse.Headers['Location'])"

$dashboard = Invoke-WebRequest -Uri 'http://localhost:8080/dashboard' -WebSession $session -SkipHttpErrorCheck
Write-Host "DASHBOARD_STATUS=$($dashboard.StatusCode)"
Write-Host "DASHBOARD_HAS_EMAIL=$($dashboard.Content.Contains('teacher@readingtracker.local'))"

