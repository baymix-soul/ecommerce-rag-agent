# Smoke Test Script - Windows PowerShell
# Usage: .\smoke_test.ps1 -BaseUrl "https://api.your-domain.com"
#    or: .\smoke_test.ps1 -BaseUrl "http://localhost:8080"

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Continue"
$passed = 0
$failed = 0

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  E-Commerce RAG Agent - Smoke Test" -ForegroundColor Cyan
Write-Host "  Target: $BaseUrl" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Body,
        [string]$ContentType = "application/json",
        [scriptblock]$Validator
    )

    Write-Host "[TEST] $Name" -ForegroundColor Yellow
    Write-Host "  $Method $BaseUrl$Path" -ForegroundColor Gray

    try {
        $params = @{
            Uri         = "$BaseUrl$Path"
            Method      = $Method
            ContentType = $ContentType
            ErrorAction = "Stop"
        }
        if ($Body) {
            $params.Body = $Body
        }

        $response = Invoke-RestMethod @params

        if ($Validator) {
            $result = & $Validator $response
            if ($result) {
                Write-Host "  PASS: $result" -ForegroundColor Green
                $script:passed++
            } else {
                Write-Host "  FAIL: validation failed" -ForegroundColor Red
                Write-Host "  Response: $($response | ConvertTo-Json -Depth 3 -Compress)" -ForegroundColor DarkGray
                $script:failed++
            }
        } else {
            Write-Host "  PASS: HTTP $Method $Path succeeded" -ForegroundColor Green
            $script:passed++
        }
    } catch {
        Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
        $script:failed++
    }
    Write-Host ""
}

# 1. Health Check
Test-Endpoint -Name "Health Check" -Method "GET" -Path "/api/health" -Validator {
    param($r)
    $r.status -eq "ok" -and $r.service -eq "ecommerce-rag-agent"
}

# 2. Product List
Test-Endpoint -Name "Product List" -Method "GET" -Path "/api/products?limit=3" -Validator {
    param($r)
    $r -is [array] -and $r.Count -gt 0 -and $r[0].product_id -ne $null
}

# 3. Vector Index Stats
Test-Endpoint -Name "Vector Index Stats" -Method "GET" -Path "/api/rag/vector-index/stats" -Validator {
    param($r)
    $r.PSObject.Properties.Name -contains "count" -and $r.PSObject.Properties.Name -contains "embedding_model"
}

# 4. Retrieval Debug
Test-Endpoint -Name "Retrieval Debug" -Method "GET" -Path "/api/rag/retrieval/debug?query=%E6%8E%A8%E8%8D%90%E5%87%A0%E6%AC%BE%E8%B7%91%E9%9E%8B&limit=3" -Validator {
    param($r)
    $r.PSObject.Properties.Name -contains "total" -and $r.PSObject.Properties.Name -contains "query_analysis"
}

# 5. SSE Chat Stream
Write-Host "[TEST] SSE Chat Stream" -ForegroundColor Yellow
Write-Host "  POST $BaseUrl/api/chat/stream" -ForegroundColor Gray
try {
    $body = '{"message":"推荐一款跑鞋","session_id":"deploy-smoke-1","limit":3}'
    $response = Invoke-WebRequest `
        -Uri "$BaseUrl/api/chat/stream" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body `
        -ErrorAction Stop

    $content = $response.Content
    $hasText = $content -match "event:text"
    $hasProductCard = $content -match "event:product_card"
    $hasDone = $content -match "event:done"
    $hasError = $content -match "event:error"

    if ($hasDone -and -not $hasError) {
        Write-Host "  PASS: SSE stream completed (text=$hasText, product_card=$hasProductCard, done=$hasDone, error=$hasError)" -ForegroundColor Green
        $passed++
    } elseif ($hasError) {
        Write-Host "  FAIL: SSE stream returned error" -ForegroundColor Red
        Write-Host "  Response (first 500 chars): $($content.Substring(0, [Math]::Min(500, $content.Length)))" -ForegroundColor DarkGray
        $failed++
    } else {
        Write-Host "  PASS with warnings: SSE stream responded (done=$hasDone, error=$hasError)" -ForegroundColor Yellow
        $passed++
    }
} catch {
    Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
    $failed++
}
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Results: $passed passed, $failed failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "========================================" -ForegroundColor Cyan

if ($failed -gt 0) {
    exit 1
}
