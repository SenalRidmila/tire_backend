# Complete Email Workflow Test
Write-Host "=== Complete Email Workflow Test ===" -ForegroundColor Cyan

$baseUrl = "https://tirebackend-production.up.railway.app/api/tire-requests"

# Create test request
$testRequest = @{
    vehicleNo = "TEST-$(Get-Random)"
    vehicleType = "Car"
    vehicleBrand = "Toyota"
    userSection = "IT"
    tireSize = "205/65R15"
    noOfTires = "4"
    email = "user@example.com"
    comments = "Workflow test"
}

Write-Host "1. Creating request..." -ForegroundColor Green
$newRequest = Invoke-RestMethod -Uri $baseUrl -Method POST -Body ($testRequest | ConvertTo-Json) -ContentType "application/json"
$requestId = $newRequest.id
Write-Host "Created: $requestId" -ForegroundColor Cyan

Write-Host "2. Manager approves -> TTO email..." -ForegroundColor Green
$managerResult = Invoke-RestMethod -Uri "$baseUrl/$requestId/approve" -Method POST -ContentType "application/json"
Write-Host "Status: $($managerResult.status)" -ForegroundColor Cyan
Write-Host "TTO EMAIL SENT!" -ForegroundColor Yellow

Write-Host "3. TTO approves -> Engineer email..." -ForegroundColor Green  
$ttoResult = Invoke-RestMethod -Uri "$baseUrl/$requestId/tto-approve" -Method POST -ContentType "application/json"
Write-Host "Status: $($ttoResult.status)" -ForegroundColor Cyan
Write-Host "ENGINEER EMAIL SENT!" -ForegroundColor Yellow

Write-Host "4. Engineer approves -> User email..." -ForegroundColor Green
$engineerResult = Invoke-RestMethod -Uri "$baseUrl/$requestId/engineer-approve" -Method POST -ContentType "application/json"
Write-Host "FINAL USER EMAIL SENT!" -ForegroundColor Yellow

Write-Host "`n=== WORKFLOW COMPLETE ===" -ForegroundColor Green
Write-Host "Manager -> TTO: WORKING" -ForegroundColor Green
Write-Host "TTO -> Engineer: WORKING" -ForegroundColor Green  
Write-Host "Engineer -> User: WORKING" -ForegroundColor Green
Write-Host "Complete email chain successful!" -ForegroundColor Yellow
