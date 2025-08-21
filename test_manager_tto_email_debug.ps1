# Debug Manager Approval → TTO Email Issue
# මේක manager approve කරනකොට TTO email එක යනවද නැද්ද කියලා test කරන script එකක්

$baseUrl = "https://tirebackend-production.up.railway.app/api/tire-requests"

Write-Host "=== Debug Manager → TTO Email Issue ===" -ForegroundColor Magenta
Write-Host "Checking why TTO emails are not being sent after manager approval" -ForegroundColor Yellow

# Test 1: Check current manager requests
Write-Host "`n1. Checking current manager requests..." -ForegroundColor Green
try {
    $managerRequests = Invoke-RestMethod -Uri "$baseUrl/manager/requests" -Method GET
    Write-Host "Manager requests found: $($managerRequests.Count)" -ForegroundColor Cyan
    
    # Show first few requests that can be approved
    $pendingRequests = $managerRequests | Where-Object { $_.status -eq "pending" -or $_.status -eq "PENDING" }
    Write-Host "Pending requests that can be approved: $($pendingRequests.Count)" -ForegroundColor Yellow
    
    if ($pendingRequests.Count -gt 0) {
        $testRequest = $pendingRequests[0]
        Write-Host "Testing with request: $($testRequest.id) - Vehicle: $($testRequest.vehicleNo)" -ForegroundColor Cyan
        $testRequestId = $testRequest.id
    } else {
        Write-Host "No pending requests found. Creating a test request..." -ForegroundColor Yellow
        
        # Create a test request
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
        
        $createdRequest = Invoke-RestMethod -Uri $baseUrl -Method POST -Body ($newRequest | ConvertTo-Json) -ContentType "application/json"
        Write-Host "Created test request: $($createdRequest.id)" -ForegroundColor Green
        $testRequestId = $createdRequest.id
    }
} catch {
    Write-Host "Error checking manager requests: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Approve the request (should trigger TTO email)
Write-Host "`n2. Approving request (should trigger TTO email)..." -ForegroundColor Green
Write-Host "Approving request ID: $testRequestId" -ForegroundColor Cyan

try {
    $approvalResponse = Invoke-RestMethod -Uri "$baseUrl/$testRequestId/approve" -Method POST -ContentType "application/json"
    Write-Host "✅ Approval successful!" -ForegroundColor Green
    Write-Host "   - Request ID: $($approvalResponse.id)" -ForegroundColor Cyan
    Write-Host "   - New Status: $($approvalResponse.status)" -ForegroundColor Cyan
    Write-Host "   - Vehicle: $($approvalResponse.vehicleNo)" -ForegroundColor Cyan
    
    if ($approvalResponse.status -eq "MANAGER_APPROVED") {
        Write-Host "📧 TTO EMAIL SHOULD HAVE BEEN SENT!" -ForegroundColor Yellow -BackgroundColor Blue
        Write-Host "   - TTO Email: slttransportofficer@gmail.com" -ForegroundColor Yellow
        Write-Host "   - Subject: 'Tire Request Approved - Action Required #$($approvalResponse.id)'" -ForegroundColor Yellow
    } else {
        Write-Host "⚠️ Unexpected status: $($approvalResponse.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error in approval: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error details: $errorBody" -ForegroundColor Red
    }
    exit 1
}

# Test 3: Check TTO dashboard to see if request appears
Write-Host "`n3. Checking TTO dashboard for the approved request..." -ForegroundColor Green
try {
    $ttoRequests = Invoke-RestMethod -Uri "$baseUrl/tto/requests" -Method GET
    Write-Host "TTO requests found: $($ttoRequests.Count)" -ForegroundColor Cyan
    
    $ourRequest = $ttoRequests | Where-Object { $_.id -eq $testRequestId }
    if ($ourRequest) {
        Write-Host "✅ Request found in TTO dashboard!" -ForegroundColor Green
        Write-Host "   - Status: $($ourRequest.status)" -ForegroundColor Cyan
        Write-Host "   - Vehicle: $($ourRequest.vehicleNo)" -ForegroundColor Cyan
    } else {
        Write-Host "❌ Request NOT found in TTO dashboard!" -ForegroundColor Red
    }
} catch {
    Write-Host "Error checking TTO dashboard: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Check recent server logs (if available)
Write-Host "`n4. Email Debug Information:" -ForegroundColor Green
Write-Host "   - TTO Email configured: slttransportofficer@gmail.com" -ForegroundColor Cyan
Write-Host "   - SMTP Server: smtp.gmail.com:587" -ForegroundColor Cyan
Write-Host "   - Sender Email: slthrmanager@gmail.com" -ForegroundColor Cyan

Write-Host "`n=== POSSIBLE ISSUES ===" -ForegroundColor Red
Write-Host "1. Email server configuration issue" -ForegroundColor Yellow
Write-Host "2. TTO email address might be wrong or not receiving" -ForegroundColor Yellow
Write-Host "3. SMTP authentication failure" -ForegroundColor Yellow
Write-Host "4. Email template processing error" -ForegroundColor Yellow
Write-Host "5. Exception in sendApprovalNotificationToTTO method" -ForegroundColor Yellow

Write-Host "`n=== NEXT STEPS ===" -ForegroundColor Blue
Write-Host "1. Check TTO email inbox: slttransportofficer@gmail.com" -ForegroundColor Cyan
Write-Host "2. Check Railway backend logs for email errors" -ForegroundColor Cyan
Write-Host "3. Verify SMTP credentials are working" -ForegroundColor Cyan
Write-Host "4. Test email sending manually" -ForegroundColor Cyan

Write-Host "`n=== Test Completed ===" -ForegroundColor Magenta
Write-Host "Request ID tested: $testRequestId" -ForegroundColor Yellow
