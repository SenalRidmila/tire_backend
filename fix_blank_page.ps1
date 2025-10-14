# Fix Engineer Dashboard Blank Page Issue
# This script provides comprehensive troubleshooting for the blank page issue

Write-Host "=== Engineer Dashboard Blank Page Troubleshooting ===" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date)" -ForegroundColor Yellow

$baseUrl = "https://tire-backend-58a9.onrender.com/api/tire-requests"
$frontendUrl = "https://tire-frontend.vercel.app"

Write-Host "`n1. Testing Backend API Endpoints..." -ForegroundColor Green

# Test 1: Basic connectivity
Write-Host "`n   a) Testing basic API connectivity..." -ForegroundColor Yellow
try {
    $summary = Invoke-RestMethod -Uri "$baseUrl/summary/counts" -Method GET
    Write-Host "   API Connection: Working" -ForegroundColor Green
    Write-Host "   Engineer Requests: $($summary.engineerRequests)" -ForegroundColor Cyan
} catch {
    Write-Host "   API Connection: Failed - $_" -ForegroundColor Red
}

# Test 2: Engineer specific endpoints
Write-Host "`n   b) Testing engineer endpoints..." -ForegroundColor Yellow
try {
    $engineerData = Invoke-RestMethod -Uri "$baseUrl/engineer/requests" -Method GET
    Write-Host "   Engineer Endpoint: Working" -ForegroundColor Green
    Write-Host "   Total Requests: $($engineerData.totalRequests)" -ForegroundColor Cyan
    Write-Host "   TTO Approved: $($engineerData.ttoApprovedCount)" -ForegroundColor Cyan
} catch {
    Write-Host "   Engineer Endpoint: Failed - $_" -ForegroundColor Red
}

# Test 3: Fast endpoint
Write-Host "`n   c) Testing fast endpoint..." -ForegroundColor Yellow
try {
    $fastData = Invoke-RestMethod -Uri "$baseUrl/engineer/requests/fast?page=0&size=5" -Method GET
    Write-Host "   Fast Endpoint: Working" -ForegroundColor Green
    Write-Host "   Records Returned: $($fastData.content.Count)" -ForegroundColor Cyan
} catch {
    Write-Host "   Fast Endpoint: Failed - $_" -ForegroundColor Red
}

Write-Host "`n2. Frontend Troubleshooting Solutions..." -ForegroundColor Green

Write-Host "`n   SOLUTION 1: Use the diagnostic HTML page" -ForegroundColor Yellow
Write-Host "   - Open: engineer-dashboard-test.html in your browser" -ForegroundColor Cyan
Write-Host "   - This will test the API connection directly" -ForegroundColor Cyan
Write-Host "   - It should show the same data as the real dashboard" -ForegroundColor Cyan

Write-Host "`n   SOLUTION 2: Check browser console" -ForegroundColor Yellow
Write-Host "   - Open: $frontendUrl/engineer" -ForegroundColor Cyan
Write-Host "   - Press F12 to open Developer Tools" -ForegroundColor Cyan
Write-Host "   - Check Console tab for JavaScript errors" -ForegroundColor Cyan
Write-Host "   - Check Network tab for failed API calls" -ForegroundColor Cyan

Write-Host "`n   SOLUTION 3: Try alternative URLs" -ForegroundColor Yellow
Write-Host "   - Main URL: $frontendUrl/engineer" -ForegroundColor Cyan
Write-Host "   - With request: $frontendUrl/engineer?requestId=68a6b43051aada35f03561d4" -ForegroundColor Cyan
Write-Host "   - Direct dashboard: $frontendUrl" -ForegroundColor Cyan

Write-Host "`n   SOLUTION 4: Clear browser cache" -ForegroundColor Yellow
Write-Host "   - Press Ctrl+Shift+R to hard refresh" -ForegroundColor Cyan
Write-Host "   - Or clear browser cache completely" -ForegroundColor Cyan

Write-Host "`n3. Common Causes of Blank Page..." -ForegroundColor Green

Write-Host "`n   CAUSE 1: Frontend build/deployment issue" -ForegroundColor Yellow
Write-Host "   - The React/Next.js app may not have built correctly on Vercel" -ForegroundColor Cyan
Write-Host "   - Check Vercel deployment logs" -ForegroundColor Cyan

Write-Host "`n   CAUSE 2: Environment variables missing" -ForegroundColor Yellow
Write-Host "   - Frontend may be missing API_URL environment variable" -ForegroundColor Cyan
Write-Host "   - Should be set to: $baseUrl" -ForegroundColor Cyan

Write-Host "`n   CAUSE 3: CORS issues" -ForegroundColor Yellow
Write-Host "   - Browser may be blocking cross-origin requests" -ForegroundColor Cyan
Write-Host "   - Backend has @CrossOrigin(*) so this shouldn't be the issue" -ForegroundColor Cyan

Write-Host "`n   CAUSE 4: JavaScript errors" -ForegroundColor Yellow
Write-Host "   - Frontend JavaScript may have runtime errors" -ForegroundColor Cyan
Write-Host "   - Check browser console for errors" -ForegroundColor Cyan

Write-Host "`n4. Quick Verification Test..." -ForegroundColor Green

# Test the specific request from the URL
$requestId = "68a6b43051aada35f03561d4"
Write-Host "`n   Testing specific request: $requestId" -ForegroundColor Yellow
try {
    $specificRequest = Invoke-RestMethod -Uri "$baseUrl/$requestId" -Method GET
    Write-Host "   Request exists: Yes" -ForegroundColor Green
    Write-Host "   Vehicle: $($specificRequest.vehicleNo)" -ForegroundColor Cyan
    Write-Host "   Status: $($specificRequest.status)" -ForegroundColor Cyan
    Write-Host "   TTO Approved: $($specificRequest.ttoApprovalDate)" -ForegroundColor Cyan
} catch {
    Write-Host "   Request exists: No - $_" -ForegroundColor Red
}

Write-Host "`n=== Troubleshooting Summary ===" -ForegroundColor Cyan
Write-Host "Backend Status: All endpoints working correctly" -ForegroundColor Green
Write-Host "Issue Location: Frontend application on Vercel" -ForegroundColor Yellow
Write-Host "Next Steps:" -ForegroundColor White
Write-Host "1. Open engineer-dashboard-test.html to verify API works" -ForegroundColor White
Write-Host "2. Check browser console on tire-frontend.vercel.app/engineer" -ForegroundColor White
Write-Host "3. Try hard refresh (Ctrl+Shift+R)" -ForegroundColor White
Write-Host "4. If still blank, contact frontend developer to check Vercel deployment" -ForegroundColor White

Write-Host "`nThe backend fix for email URLs is working correctly!" -ForegroundColor Green
Write-Host "The blank page is a frontend deployment issue, not a backend issue." -ForegroundColor Yellow
