[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$TimeoutSeconds = 60
)
$ErrorActionPreference = 'Stop'

$body = @{
    employeeIds = @(101)
    startPeriod = '2026-01'
    endPeriod = '2026-01'
    paymentTypes = @('SALARY')
} | ConvertTo-Json

$accepted = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/exports" -ContentType 'application/json' -Body $body
if (-not $accepted.requestId -or $accepted.status -ne 'ACCEPTED') {
    throw 'A API não retornou uma aceitação assíncrona válida.'
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$request = $null
do {
    Start-Sleep -Milliseconds 500
    try {
        $request = Invoke-RestMethod -Uri "$BaseUrl/api/v1/exports/$($accepted.requestId)"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    }
} while (($null -eq $request -or $request.status -notin @('COMPLETED', 'PARTIAL', 'FAILED')) -and (Get-Date) -lt $deadline)

if ($null -eq $request -or $request.status -ne 'COMPLETED') {
    throw "Solicitação $($accepted.requestId) terminou em '$($request.status)' ou excedeu o prazo."
}

$documentResponse = Invoke-RestMethod -Uri "$BaseUrl/api/v1/exports/$($accepted.requestId)/documents"
$documents = @($documentResponse.documents)
if ($documents.Count -ne 1 -or $documents[0].status -ne 'COMPLETED') {
    throw "Esperado exatamente um documento concluído; obtido $($documents.Count)."
}

$temporaryPdf = Join-Path ([System.IO.Path]::GetTempPath()) "payroll-$($accepted.requestId).pdf"
try {
    Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/api/v1/documents/$($documents[0].documentId)/download" -OutFile $temporaryPdf
    $prefix = [System.Text.Encoding]::ASCII.GetString([System.IO.File]::ReadAllBytes($temporaryPdf), 0, 5)
    if ($prefix -ne '%PDF-') { throw 'O download não contém um PDF válido.' }
} finally {
    Remove-Item -LiteralPath $temporaryPdf -ErrorAction SilentlyContinue
}

Write-Host "OK E2E assíncrono: requestId=$($accepted.requestId), documentos=$($documents.Count), PDF válido."
