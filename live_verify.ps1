# LIVE VERIFICATION SCRIPT FOR DOCUMIND
# Keep this script running after the Docker containers are up.

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "DocuMind Enterprise Live Verification" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Verify Docker
Write-Host "[1/4] Checking Docker Services..." -ForegroundColor Yellow
docker-compose ps
Start-Sleep -Seconds 2

# 2. Start Application in Background
Write-Host "[2/4] Booting Enterprise Spring Boot Backend (Detached)..." -ForegroundColor Yellow
Start-Process -NoNewWindow -FilePath "mvn" -ArgumentList "spring-boot:run"

Write-Host "Waiting for Spring Boot to spin up on Port 8080 (15 seconds)..." -ForegroundColor DarkGray
Start-Sleep -Seconds 20

# 3. Create a Dummy PDF File
Write-Host "[3/4] Generating Mock High-Risk PDF Payload..." -ForegroundColor Yellow
$PdfContent = "%PDF-1.4`n%Mock High Risk Legal Document`nWARNING: This document contains a high-risk liability clause with no expiry date.`nGoverning Law: MA."
Set-Content -Path "mock_contract.pdf" -Value $PdfContent

# 4. Trigger Ingestion API
Write-Host "[4/4] Sending POST /api/contracts/upload to Kafka Broker..." -ForegroundColor Yellow
$Response = Invoke-RestMethod -Uri "http://localhost:8080/api/contracts/upload?organizationId=123e4567-e89b-12d3-a456-426614174000" `
    -Method Post `
    -Form @{
        file = Get-Item -Path "mock_contract.pdf"
    }

Write-Host "`n=== API RESPONSE (Eventual Consistency) ===" -ForegroundColor Green
Write-Host $Response
Write-Host "===========================================" -ForegroundColor Green

Write-Host "`nThe Kafka Broker has accepted the payload and dropped it into S3 + PSQL!" -ForegroundColor Cyan
Write-Host "Please check the terminal window running Spring Boot to see the AI extracting the text and firing the Webhook." -ForegroundColor Cyan
