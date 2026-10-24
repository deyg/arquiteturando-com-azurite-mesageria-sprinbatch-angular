[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$systems = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$state = Join-Path $systems '.runtime\processes.json'
if (-not (Test-Path $state)) { Write-Host 'Nenhum processo registrado pelo start-local.ps1.'; exit 0 }
$records = @(Get-Content $state -Raw | ConvertFrom-Json) | Where-Object { $null -ne $_ -and $null -ne $_.pid }
foreach ($record in $records) {
    $process = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -eq 'java') {
        Stop-Process -Id $record.pid
        Write-Host "$($record.name): PID $($record.pid) parado."
    } else { Write-Host "$($record.name): PID registrado não está ativo como Java; ignorado." }
}
Remove-Item -LiteralPath $state
