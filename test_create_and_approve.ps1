# Create test request and test TTO email
$baseUrl = "https://tirebackend-production.up.railway.app/api/tire-requests"

Write-Host "=== Testing Manager Approval TTO Email ===" -ForegroundColor Magenta

# Create a test request
Write-Host "`n1. Creating test request..." -ForegroundColor Green
$newRequest = @{
    vehicleNo = "TEST-$(Get-Random -Maximum 9999)"
    vehicleType = "Car"
    vehicleBrand = "Toyota" 
    vehicleModel = "Corolla"
    userSection = "Transport"
    replacementDate = "2025-08-22"
    existingMake = "Bridgestone"
    tireSize = "195/65R15"
    noOfTires = 4
    noOfTubes = 4
    costCenter = "CC001"
    presentKm = 50000
    previousKm = 30000
    wearIndicator = "2mm"
    wearPattern = "Even"
    officerServiceNo = "EMP001"
    email = "test@example.com"
    comments = "Test request for TTO email debug"
}

try {
    $createdRequest = Invoke-RestMethod -Uri $baseUrl -Method POST -Body ($newRequest | ConvertTo-Json) -ContentType "application/json"
    Write-Host "Created request: $($createdRequest.id)" -ForegroundColor Green
    Write-Host "Vehicle: $($createdRequest.vehicleNo)" -ForegroundColor Cyan
    $testRequestId = $createdRequest.id
} catch {
    Write-Host "Error creating request: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Wait a moment
Start-Sleep -Seconds 2

# Approve the request (should trigger TTO email)
Write-Host "`n2. Manager approving request (should send TTO email)..." -ForegroundColor Green
try {
    $approvalResponse = Invoke-RestMethod -Uri "$baseUrl/$testRequestId/approve" -Method POST -ContentType "application/json"
    Write-Host "Success! Request approved!" -ForegroundColor Green
    Write-Host "   - Request ID: $($approvalResponse.id)" -ForegroundColor Cyan
    Write-Host "   - Status: $($approvalResponse.status)" -ForegroundColor Cyan
    Write-Host "   - Vehicle: $($approvalResponse.vehicleNo)" -ForegroundColor Cyan
    
    if ($approvalResponse.status -eq "MANAGER_APPROVED") {
        Write-Host ""
        Write-Host "EMAIL TO TTO SHOULD HAVE BEEN SENT!" -ForegroundColor White -BackgroundColor Red
        Write-Host "Email: slttransportofficer@gmail.com" -ForegroundColor Yellow
        Write-Host "Subject: Tire Request Approved - Action Required #$($approvalResponse.id)" -ForegroundColor Yellow
        Write-Host ""
    }
} catch {
    Write-Host "Error approving: $($_.Exception.Message)" -ForegroundColor Red
}

# Check TTO dashboard
Write-Host "3. Checking TTO dashboard..." -ForegroundColor Green
try {
    $ttoRequests = Invoke-RestMethod -Uri "$baseUrl/tto/requests" -Method GET
    $ourRequest = $ttoRequests | Where-Object { $_.id -eq $testRequestId }
    
    if ($ourRequest) {
        Write-Host "Request appears in TTO dashboard: $($ourRequest.status)" -ForegroundColor Green
    } else {
        Write-Host "Request NOT found in TTO dashboard!" -ForegroundColor Red
    }
} catch {
    Write-Host "Error checking TTO: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== SUMMARY ===" -ForegroundColor Blue
Write-Host "Request ID: $testRequestId" -ForegroundColor Yellow
Write-Host "Check slttransportofficer@gmail.com for email!" -ForegroundColor Magenta
