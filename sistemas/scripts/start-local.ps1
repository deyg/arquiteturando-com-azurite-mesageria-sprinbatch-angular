[CmdletBinding()]
param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$systems = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$stateDir = Join-Path $systems '.runtime'
$logDir = Join-Path $stateDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
if (-not $SkipBuild) {
    & (Join-Path $systems 'mvnw.cmd') -q -DskipTests package
    if ($LASTEXITCODE) { throw 'Falha no build Maven.' }
}
$apps = @(
    @{ Name='pdf-motor-api'; Port=8084 }, @{ Name='batch-worker'; Port=8083 },
    @{ Name='request-api'; Port=8081 }, @{ Name='query-api'; Port=8082 }, @{ Name='bff-api'; Port=8080 }
)
$started = @()
foreach ($app in $apps) {
    if (Get-NetTCPConnection -State Listen -LocalPort $app.Port -ErrorAction SilentlyContinue) {
        Write-Host "$($app.Name): porta $($app.Port) já está ocupada; não iniciado."
        continue
    }
    $jar = Join-Path $systems "apps\$($app.Name)\target\$($app.Name)-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) { throw "JAR ausente: $jar" }
    $stdout = Join-Path $logDir "$($app.Name).out.log"
    $stderr = Join-Path $logDir "$($app.Name).err.log"
    $process = Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
    $started += [pscustomobject]@{ name=$app.Name; port=$app.Port; pid=$process.Id; startedAt=(Get-Date).ToString('o') }
    Write-Host "$($app.Name): PID $($process.Id), porta $($app.Port)."
}
ConvertTo-Json -InputObject @($started) | Set-Content -Encoding UTF8 (Join-Path $stateDir 'processes.json')
Write-Host 'Somente processos iniciados por este script foram registrados para parada.'
