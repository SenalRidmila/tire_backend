# Debug Manager Approval TTO Email Issue
$baseUrl = "https://tirebackend-production.up.railway.app/api/tire-requests"

Write-Host "=== Debug Manager to TTO Email Issue ===" -ForegroundColor Magenta

# Test 1: Check manager requests
Write-Host "`n1. Checking manager requests..." -ForegroundColor Green
try {
    $managerRequests = Invoke-RestMethod -Uri "$baseUrl/manager/requests" -Method GET
    Write-Host "Manager requests found: $($managerRequests.Count)" -ForegroundColor Cyan
    
    $pendingRequests = $managerRequests | Where-Object { $_.status -eq "pending" -or $_.status -eq "PENDING" }
    Write-Host "Pending requests: $($pendingRequests.Count)" -ForegroundColor Yellow
    
    if ($pendingRequests.Count -gt 0) {
        $testRequest = $pendingRequests[0]
        $testRequestId = $testRequest.id
        Write-Host "Testing with request: $testRequestId - Vehicle: $($testRequest.vehicleNo)" -ForegroundColor Cyan
    } else {
        Write-Host "No pending requests found. Need to create one first." -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Approve request (should trigger TTO email)
Write-Host "`n2. Approving request (should trigger TTO email)..." -ForegroundColor Green
try {
    $approvalResponse = Invoke-RestMethod -Uri "$baseUrl/$testRequestId/approve" -Method POST -ContentType "application/json"
    Write-Host "Success: Approval completed!" -ForegroundColor Green
    Write-Host "   - Request ID: $($approvalResponse.id)" -ForegroundColor Cyan
    Write-Host "   - New Status: $($approvalResponse.status)" -ForegroundColor Cyan
    Write-Host "   - Vehicle: $($approvalResponse.vehicleNo)" -ForegroundColor Cyan
    
    if ($approvalResponse.status -eq "MANAGER_APPROVED") {
        Write-Host "TTO EMAIL SHOULD HAVE BEEN SENT!" -ForegroundColor Yellow -BackgroundColor Blue
    }
} catch {
    Write-Host "Error in approval: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 3: Check TTO dashboard
Write-Host "`n3. Checking TTO dashboard..." -ForegroundColor Green
try {
    $ttoRequests = Invoke-RestMethod -Uri "$baseUrl/tto/requests" -Method GET
    $ourRequest = $ttoRequests | Where-Object { $_.id -eq $testRequestId }
    
    if ($ourRequest) {
        Write-Host "Request found in TTO dashboard!" -ForegroundColor Green
        Write-Host "   - Status: $($ourRequest.status)" -ForegroundColor Cyan
    } else {
        Write-Host "Request NOT found in TTO dashboard!" -ForegroundColor Red
    }
} catch {
    Write-Host "Error checking TTO dashboard: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== EMAIL DEBUG INFO ===" -ForegroundColor Blue
Write-Host "TTO Email: slttransportofficer@gmail.com" -ForegroundColor Cyan
Write-Host "Sender: slthrmanager@gmail.com" -ForegroundColor Cyan
Write-Host "Request tested: $testRequestId" -ForegroundColor Yellow

Write-Host "`nCheck TTO email inbox and Railway backend logs for any errors." -ForegroundColor Magenta
