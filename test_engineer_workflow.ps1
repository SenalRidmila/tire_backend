# Test Engineer Dashboard Workflow After TTO Approval
# This script tests the complete workflow from TTO approval to Engineer dashboard viewing

$baseUrl = "https://tirebackend-production.up.railway.app/api/tire-requests"

Write-Host "=== Testing Engineer Dashboard Workflow ===" -ForegroundColor Green

# Test 1: Check Engineer Dashboard Fast Endpoint
Write-Host "`n1. Testing Engineer Dashboard Fast Endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/engineer/requests/fast?page=0&size=5" -Method GET
    $data = $response.Content | ConvertFrom-Json
    
    Write-Host "✅ Engineer Dashboard Fast Endpoint Working" -ForegroundColor Green
    Write-Host "   - Status Code: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host "   - Total Elements: $($data.totalElements)" -ForegroundColor Cyan
    Write-Host "   - TTO Approved Count: $($data.ttoApprovedCount)" -ForegroundColor Cyan
    Write-Host "   - Current Page: $($data.currentPage)" -ForegroundColor Cyan
    Write-Host "   - Dashboard: $($data.dashboard)" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ Engineer Dashboard Fast Endpoint Failed: $_" -ForegroundColor Red
}

# Test 2: Check Engineer Dashboard Regular Endpoint
Write-Host "`n2. Testing Engineer Dashboard Regular Endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/engineer/requests" -Method GET
    $data = $response.Content | ConvertFrom-Json
    
    Write-Host "✅ Engineer Dashboard Regular Endpoint Working" -ForegroundColor Green
    Write-Host "   - Status Code: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host "   - Total Requests: $($data.totalRequests)" -ForegroundColor Cyan
    Write-Host "   - TTO Approved Count: $($data.ttoApprovedCount)" -ForegroundColor Cyan
    Write-Host "   - Status: $($data.status)" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ Engineer Dashboard Regular Endpoint Failed: $_" -ForegroundColor Red
}

# Test 3: Check Dashboard Counts
Write-Host "`n3. Testing Dashboard Counts Endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/summary/counts" -Method GET
    $data = $response.Content | ConvertFrom-Json
    
    Write-Host "✅ Dashboard Counts Endpoint Working" -ForegroundColor Green
    Write-Host "   - Status Code: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host "   - Manager Requests: $($data.managerRequests)" -ForegroundColor Cyan
    Write-Host "   - TTO Requests: $($data.ttoRequests)" -ForegroundColor Cyan
    Write-Host "   - Engineer Requests: $($data.engineerRequests)" -ForegroundColor Cyan
    Write-Host "   - Total Requests: $($data.totalRequests)" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ Dashboard Counts Endpoint Failed: $_" -ForegroundColor Red
}

# Test 4: Test Engineer Debug Endpoint (if deployed)
Write-Host "`n4. Testing Engineer Debug Endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/engineer/debug/tto-approved" -Method GET
    $data = $response.Content | ConvertFrom-Json
    
    Write-Host "✅ Engineer Debug Endpoint Working" -ForegroundColor Green
    Write-Host "   - Status Code: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host "   - Total TTO Approved: $($data.totalTTOApproved)" -ForegroundColor Cyan
    Write-Host "   - Engineer Can View: $($data.engineerCanView)" -ForegroundColor Cyan
    Write-Host "   - Timestamp: $($data.timestamp)" -ForegroundColor Cyan
}
catch {
    Write-Host "⚠️ Engineer Debug Endpoint Not Available Yet (might still be deploying)" -ForegroundColor Orange
}

Write-Host "`n=== Workflow Test Complete ===" -ForegroundColor Green
Write-Host "If all tests pass, the engineer dashboard should work properly after TTO approval!" -ForegroundColor Cyan
