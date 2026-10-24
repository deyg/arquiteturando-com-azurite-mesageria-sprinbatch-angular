[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$runtimeRoot = Join-Path $env:LOCALAPPDATA 'payroll-export-poc'
$mysqlRoot = Join-Path $runtimeRoot 'mysql\mysql-8.4.13-winx64'
$mysqlBin = Join-Path $mysqlRoot 'bin'
$mysqlData = Join-Path $runtimeRoot 'mysql-data'
$mysqlLog = Join-Path $runtimeRoot 'mysql.err.log'
$mysqlPid = Join-Path $runtimeRoot 'mysql.pid'
$mysqld = Join-Path $mysqlBin 'mysqld.exe'
$mysql = Join-Path $mysqlBin 'mysql.exe'
$mysqladmin = Join-Path $mysqlBin 'mysqladmin.exe'

if (-not (Test-Path $mysqld)) {
    throw "MySQL 8.4.13 não encontrado em $mysqlRoot. Execute o bootstrap documentado no README."
}

$version = (& $mysqld --version) -as [string]
if ($version -notmatch '\b8\.4\.13\b') {
    throw "Versão MySQL incompatível: $version. Esperado: 8.4.13 LTS."
}

if (-not (Test-Path (Join-Path $mysqlData 'mysql'))) {
    New-Item -ItemType Directory -Force -Path $mysqlData | Out-Null
    & $mysqld --initialize-insecure "--basedir=$mysqlRoot" "--datadir=$mysqlData"
    if ($LASTEXITCODE) { throw 'Falha ao inicializar o diretório de dados MySQL.' }
}

if (-not (Get-NetTCPConnection -State Listen -LocalPort 3306 -ErrorAction SilentlyContinue)) {
    $process = Start-Process -FilePath $mysqld -ArgumentList @(
        "--basedir=$mysqlRoot", "--datadir=$mysqlData", '--port=3306',
        '--bind-address=127.0.0.1', '--mysqlx=0', "--log-error=$mysqlLog"
    ) -WindowStyle Hidden -PassThru
    Set-Content -Path $mysqlPid -Value $process.Id
}

$deadline = (Get-Date).AddSeconds(60)
do {
    Start-Sleep -Seconds 1
    & $mysqladmin '--protocol=tcp' '--host=127.0.0.1' '--port=3306' '-uroot' ping 2>$null | Out-Null
    $ready = $LASTEXITCODE -eq 0
} while (-not $ready -and (Get-Date) -lt $deadline)

if (-not $ready) {
    throw "MySQL não ficou pronto em 60 segundos. Consulte $mysqlLog."
}

& $mysql '--protocol=tcp' '--host=127.0.0.1' '--port=3306' '-uroot' '-e' @"
CREATE DATABASE IF NOT EXISTS payroll CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'payroll'@'127.0.0.1' IDENTIFIED BY 'payroll';
GRANT ALL PRIVILEGES ON payroll.* TO 'payroll'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
if ($LASTEXITCODE) { throw 'Falha ao preparar database e usuário MySQL.' }

Write-Host "MySQL 8.4.13 pronto em 127.0.0.1:3306."
