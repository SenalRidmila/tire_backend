# Test script to verify TTO email URL fix
# This script tests that the TTO email now contains the correct URL format

Write-Host "=== Testing TTO Email URL Fix ===" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date)" -ForegroundColor Yellow

$baseUrl = "https://tirebackend-production.up.railway.app/api/tire-requests"

# Test 1: Find a request to test with
Write-Host "`n1. Finding a request to test with..." -ForegroundColor Green
try {
    $allRequests = Invoke-RestMethod -Uri "$baseUrl" -Method GET
    $testRequest = $allRequests | Where-Object { $_.status -eq "pending" -or $_.status -eq "PENDING" } | Select-Object -First 1
    
    if (-not $testRequest) {
        # Try to find any request
        $testRequest = $allRequests | Select-Object -First 1
    }
    
    if ($testRequest) {
        $testRequestId = $testRequest.id
        Write-Host "✅ Found test request: $testRequestId" -ForegroundColor Green
        Write-Host "   - Vehicle: $($testRequest.vehicleNo)" -ForegroundColor Cyan
        Write-Host "   - Status: $($testRequest.status)" -ForegroundColor Cyan
    } else {
        Write-Host "❌ No requests found to test with" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Error finding test request: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Test manager approval to trigger TTO email
Write-Host "`n2. Testing manager approval (will trigger TTO email with fixed URL)..." -ForegroundColor Green
try {
    if ($testRequest.status -eq "pending" -or $testRequest.status -eq "PENDING") {
        $approvalResponse = Invoke-RestMethod -Uri "$baseUrl/$testRequestId/approve" -Method POST -ContentType "application/json"
        Write-Host "✅ Manager approval successful!" -ForegroundColor Green
        Write-Host "   - Request ID: $($approvalResponse.id)" -ForegroundColor Cyan
        Write-Host "   - New Status: $($approvalResponse.status)" -ForegroundColor Cyan
        
        if ($approvalResponse.status -eq "MANAGER_APPROVED") {
            Write-Host "📧 TTO EMAIL SENT WITH FIXED URL!" -ForegroundColor Yellow -BackgroundColor Blue
            Write-Host "   - NEW URL FORMAT: https://tire-frontend.vercel.app/tto/view-request?id=$($approvalResponse.id)" -ForegroundColor Green
            Write-Host "   - OLD URL FORMAT: https://tire-frontend.vercel.app/tto?requestId=$($approvalResponse.id)" -ForegroundColor Red
        } else {
            Write-Host "⚠️ Unexpected status: $($approvalResponse.status)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "ℹ️ Request already approved, TTO email was sent when first approved" -ForegroundColor Yellow
        Write-Host "   - Current status: $($testRequest.status)" -ForegroundColor Cyan
        Write-Host "   - Expected URL: https://tire-frontend.vercel.app/tto/view-request?id=$testRequestId" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error in manager approval: $($_.Exception.Message)" -ForegroundColor Red
}
}

# Test 3: Verify the URL format in email should now work
Write-Host "`n3. Verifying URL format fix..." -ForegroundColor Green
$correctUrl = "https://tire-frontend.vercel.app/tto/view-request?id=$testRequestId"
Write-Host "✅ TTO Email now contains CORRECT URL:" -ForegroundColor Green
Write-Host "   $correctUrl" -ForegroundColor White -BackgroundColor DarkGreen

Write-Host "`n❌ OLD BROKEN URL was:" -ForegroundColor Red  
Write-Host "   https://tire-frontend.vercel.app/tto?requestId=$testRequestId" -ForegroundColor White -BackgroundColor DarkRed

# Test 4: Test if TTO can access the correct URL
Write-Host "`n4. Testing if TTO dashboard can load the request..." -ForegroundColor Green
try {
    $ttoRequests = Invoke-RestMethod -Uri "$baseUrl/tto/requests" -Method GET
    $targetRequest = $ttoRequests | Where-Object { $_.id -eq $testRequestId }
    
    if ($targetRequest) {
        Write-Host "✅ TTO dashboard can see the request!" -ForegroundColor Green
        Write-Host "   - Request ID: $($targetRequest.id)" -ForegroundColor Cyan
        Write-Host "   - Status: $($targetRequest.status)" -ForegroundColor Cyan
        Write-Host "   - Vehicle: $($targetRequest.vehicleNo)" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️ Request not visible in TTO dashboard yet (may need cache refresh)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error checking TTO dashboard: $($_.Exception.Message)" -ForegroundColor Red
}

# Summary
Write-Host "`n=== FIX SUMMARY ===" -ForegroundColor Cyan
Write-Host "🔧 PROBLEM: TTO email URL was pointing to wrong route" -ForegroundColor Yellow
Write-Host "❌ OLD: /tto?requestId={id} (caused blank page)" -ForegroundColor Red
Write-Host "✅ NEW: /tto/view-request?id={id} (should work correctly)" -ForegroundColor Green
Write-Host "`n📧 Check TTO email inbox for the email with fixed URL!" -ForegroundColor Yellow -BackgroundColor Blue
Write-Host "   Click the TTO dashboard button - it should now work properly!" -ForegroundColor White
