[CmdletBinding()]
param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
function Assert-Status([string]$Name,[string]$Uri,[int]$Expected) {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri
    if ($response.StatusCode -ne $Expected) { throw "$Name retornou $($response.StatusCode), esperado $Expected" }
    Write-Host "OK $Name ($Expected)"
    return $response
}
Assert-Status 'Health BFF' "$BaseUrl/actuator/health" 200 | Out-Null
Assert-Status 'Empregados' "$BaseUrl/api/v1/employees?startPeriod=2026-01&endPeriod=2026-06&paymentTypes=SALARY,BENEFIT" 200 | Out-Null
Assert-Status 'Pagamentos' "$BaseUrl/api/v1/employees/101/payments?startPeriod=2026-01&endPeriod=2026-06" 200 | Out-Null
Assert-Status 'Histórico' "$BaseUrl/api/v1/exports?employeeId=101&startPeriod=2026-01&endPeriod=2026-06&page=0&size=20" 200 | Out-Null
$body = @{ employeeIds=@(101); startPeriod='2026-01'; endPeriod='2026-01'; paymentTypes=@('SALARY') } | ConvertTo-Json
$accepted = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BaseUrl/api/v1/exports" -ContentType 'application/json' -Body $body
if ($accepted.StatusCode -ne 202) { throw "Exportação retornou $($accepted.StatusCode), esperado 202" }
$requestId = ($accepted.Content | ConvertFrom-Json).requestId
if (-not $requestId) { throw 'Resposta 202 sem requestId.' }
Write-Host "OK Exportação aceita (202), requestId=$requestId"
