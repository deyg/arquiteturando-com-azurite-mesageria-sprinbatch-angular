[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$NoBrowser,
    [switch]$NoPdf,
    [switch]$Stop
)
$ErrorActionPreference = 'Stop'
$systems = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtime = Join-Path $systems '.runtime'
$logDir = Join-Path $runtime 'logs'
$angularDir = Join-Path $systems 'apps\web-angular'
$angularPid = Join-Path $runtime 'angular.pid'
$home_ = Join-Path $env:LOCALAPPDATA 'payroll-export-poc'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$env:Path = ((@(
    "$home_\node\node-v26.12.1-win-x64", "$home_\npm", "$home_\mysql\mysql-8.4.13-winx64\bin", "$home_\kafka\kafka_2.13-4.3.1\bin\windows"
) | Where-Object { Test-Path $_ }) -join ';') + ';' + $env:Path

function Test-Port([int]$Port) {
    [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}
function Wait-Http([string]$Url, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try { if ((Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5).StatusCode -eq 200) { return } } catch {}
        Start-Sleep -Seconds 2
    }
    throw "Timeout aguardando $Url"
}
function Wait-Port([int]$Port, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) { if (Test-Port $Port) { return }; Start-Sleep -Seconds 2 }
    throw "Timeout aguardando a porta $Port"
}

if ($Stop) {
    if (Test-Path $angularPid) {
        $id = [int](Get-Content $angularPid)
        if (Get-Process -Id $id -ErrorAction SilentlyContinue) { & taskkill.exe /PID $id /T /F | Out-Null }
        Remove-Item $angularPid
        Write-Host "Angular parado (PID $id)."
    }
    & (Join-Path $PSScriptRoot 'stop-local.ps1')
    Write-Host 'MySQL, Kafka e Azurite foram preservados.'
    return
}

Write-Host '==> Pré-requisitos' -ForegroundColor Cyan
foreach ($tool in 'java', 'node', 'npm.cmd') {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "$tool não encontrado no PATH." }
}

Write-Host '==> Infraestrutura (MySQL, Kafka, Azurite)' -ForegroundColor Cyan
if (-not (Test-Port 3306)) {
    & (Join-Path $PSScriptRoot 'start-mysql.ps1')
    Wait-Port 3306 60
}
if (-not (Test-Port 9092)) {
    Start-Process -FilePath 'kafka-server-start.bat' -ArgumentList "`"$home_\kafka-server.properties`"" -WindowStyle Hidden `
        -RedirectStandardOutput "$logDir\kafka.out.log" -RedirectStandardError "$logDir\kafka.err.log" | Out-Null
    Wait-Port 9092 90
}
if (-not (Test-Port 10000)) {
    Start-Process -FilePath 'azurite-blob.cmd' -ArgumentList '--location', "`"$home_\azurite-data`"", '--blobHost', '127.0.0.1', '--blobPort', '10000', '--silent' `
        -WindowStyle Hidden -RedirectStandardOutput "$home_\azurite.out.log" -RedirectStandardError "$home_\azurite.err.log" | Out-Null
    Wait-Port 10000 30
}
Write-Host 'Infraestrutura pronta (3306, 9092, 10000).'

Write-Host '==> Aplicações Java (portas 8080-8084)' -ForegroundColor Cyan
if ($SkipBuild) { & (Join-Path $PSScriptRoot 'start-local.ps1') -SkipBuild } else { & (Join-Path $PSScriptRoot 'start-local.ps1') }
Wait-Http 'http://127.0.0.1:8080/actuator/health' 120

Write-Host '==> Angular (porta 4200)' -ForegroundColor Cyan
if (-not (Test-Path (Join-Path $angularDir 'node_modules'))) {
    Push-Location $angularDir; try { npm.cmd ci } finally { Pop-Location }
}
if (-not (Test-Port 4200)) {
    $ng = Start-Process -FilePath $env:ComSpec -ArgumentList '/c', 'npm.cmd start -- --host 127.0.0.1' -WorkingDirectory $angularDir -WindowStyle Hidden `
        -RedirectStandardOutput "$logDir\angular.out.log" -RedirectStandardError "$logDir\angular.err.log" -PassThru
    Set-Content -Path $angularPid -Value $ng.Id
}
Wait-Http 'http://127.0.0.1:4200' 120

if ($NoPdf) {
    Write-Host ''
    Write-Host 'Tudo no ar. Use a tela: http://127.0.0.1:4200' -ForegroundColor Green
    Write-Host 'Os serviços continuam rodando. Para encerrar: .\scripts\run-demo.ps1 -Stop' -ForegroundColor Green
    if (-not $NoBrowser) { Start-Process 'http://127.0.0.1:4200' }
    return
}

Write-Host '==> Gerando PDF via exportação assíncrona' -ForegroundColor Cyan
$base = 'http://127.0.0.1:8080'
$body = @{ employeeIds = @(101); startPeriod = '2026-01'; endPeriod = '2026-01'; paymentTypes = @('SALARY') } | ConvertTo-Json
$accepted = Invoke-RestMethod -Method Post -Uri "$base/api/v1/exports" -ContentType 'application/json' -Body $body
$deadline = (Get-Date).AddSeconds(120)
do {
    Start-Sleep -Milliseconds 500
    try { $request = Invoke-RestMethod -Uri "$base/api/v1/exports/$($accepted.requestId)" } catch { $request = $null }
} while (($null -eq $request -or $request.status -notin 'COMPLETED', 'PARTIAL', 'FAILED') -and (Get-Date) -lt $deadline)
if ($request.status -ne 'COMPLETED') { throw "Exportação terminou em '$($request.status)'." }

$doc = @((Invoke-RestMethod -Uri "$base/api/v1/exports/$($accepted.requestId)/documents").documents)[0]
$pdf = Join-Path $runtime "payroll-$($accepted.requestId).pdf"
Invoke-WebRequest -UseBasicParsing -Uri "$base/api/v1/documents/$($doc.documentId)/download" -OutFile $pdf
$signature = [Text.Encoding]::ASCII.GetString([IO.File]::ReadAllBytes($pdf), 0, 5)
if ($signature -ne '%PDF-') { throw 'O arquivo baixado não é um PDF válido.' }

Write-Host ''
Write-Host "PDF gerado: $pdf" -ForegroundColor Green
Write-Host 'Tela: http://127.0.0.1:4200   |   Para encerrar: .\scripts\run-demo.ps1 -Stop' -ForegroundColor Green
if (-not $NoBrowser) {
    Start-Process 'http://127.0.0.1:4200'
    Start-Process $pdf
}
