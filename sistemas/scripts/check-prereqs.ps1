[CmdletBinding()]
param()

$ErrorActionPreference = 'SilentlyContinue'
$results = [System.Collections.Generic.List[object]]::new()
$runtimeRoot = Join-Path $env:LOCALAPPDATA 'payroll-export-poc'
$runtimePaths = @(
    (Join-Path $runtimeRoot 'node\node-v26.12.1-win-x64'),
    (Join-Path $runtimeRoot 'npm'),
    (Join-Path $runtimeRoot 'mysql\mysql-8.4.13-winx64\bin'),
    (Join-Path $runtimeRoot 'kafka\kafka_2.13-4.3.1\bin\windows')
) | Where-Object { Test-Path $_ }
if ($runtimePaths) {
    $env:Path = (($runtimePaths -join ';') + ';' + $env:Path)
}

function Add-Check {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Detected,
        [string]$Required
    )

    $results.Add([pscustomobject]@{
        Item = $Name
        Status = $Status
        Detected = $Detected
        Required = $Required
    })
}

function Get-FirstLine {
    param([scriptblock]$Command)
    $output = & $Command 2>&1
    return (($output | Select-Object -First 1) -as [string])
}

$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    $javaText = ((& cmd.exe /d /c 'java -version 2>&1') | Where-Object { $_ } | Select-Object -First 1) -as [string]
    $javaStatus = if ($javaText -match 'version "21\.') { 'OK' } else { 'BLOCKED' }
    Add-Check 'Java' $javaStatus $javaText 'Java 21'
} else {
    Add-Check 'Java' 'BLOCKED' 'não encontrado' 'Java 21'
}

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($maven) {
    Add-Check 'Maven' 'OK' (Get-FirstLine { mvn -version }) 'Maven 3.9+'
} else {
    Add-Check 'Maven' 'BLOCKED' 'não encontrado' 'Maven 3.9+'
}

$node = Get-Command node -ErrorAction SilentlyContinue
if ($node) {
    $nodeText = Get-FirstLine { node --version }
    $nodeVersion = [version]($nodeText.TrimStart('v'))
    $nodeStatus = if ($nodeVersion -ge [version]'22.22.3') { 'OK' } else { 'BLOCKED' }
    Add-Check 'Node.js' $nodeStatus $nodeText '^22.22.3 || ^24.15.0 || ^26.0.0'
} else {
    Add-Check 'Node.js' 'BLOCKED' 'não encontrado' '^22.22.3 || ^24.15.0 || ^26.0.0'
}

$npm = Get-Command npm -ErrorAction SilentlyContinue
if ($npm) {
    Add-Check 'npm' 'OK' (Get-FirstLine { npm --version }) 'compatível com Node.js'
} else {
    Add-Check 'npm' 'BLOCKED' 'não encontrado' 'compatível com Node.js'
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysql) {
    $mysqlText = Get-FirstLine { mysql --version }
    $mysqlStatus = if ($mysqlText -match '\b8\.4\.13\b') { 'OK' } else { 'BLOCKED' }
    Add-Check 'MySQL' $mysqlStatus $mysqlText 'MySQL 8.4.13 LTS'
} else {
    Add-Check 'MySQL' 'BLOCKED' 'não encontrado no PATH' 'MySQL 8.4.13 LTS'
}

$checks = @(
    @{ Name = 'Angular CLI'; Commands = @('ng.cmd', 'ng'); Required = 'Angular CLI 22.x' },
    @{ Name = 'Kafka storage'; Commands = @('kafka-storage.bat'); Required = 'Kafka KRaft 4.3.1' },
    @{ Name = 'Kafka server'; Commands = @('kafka-server-start.bat'); Required = 'Kafka KRaft 4.3.1' },
    @{ Name = 'Kafka topics'; Commands = @('kafka-topics.bat'); Required = 'Kafka KRaft 4.3.1' },
    @{ Name = 'Azurite Blob'; Commands = @('azurite-blob.cmd', 'azurite-blob'); Required = 'Azurite Blob' }
)

foreach ($check in $checks) {
    $command = $null
    foreach ($candidate in $check.Commands) {
        $command = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($command) { break }
    }
    if ($command) {
        Add-Check $check.Name 'OK' $command.Source $check.Required
    } else {
        Add-Check $check.Name 'BLOCKED' 'não encontrado no PATH' $check.Required
    }
}

$ports = @(4200, 8080, 8081, 8082, 8083, 8084, 9092, 3306, 10000)
foreach ($port in $ports) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        $expected = switch ($port) {
            3306 { $process.ProcessName -eq 'mysqld' }
            9092 { $process.ProcessName -eq 'java' }
            10000 { $process.ProcessName -eq 'node' }
            default { $false }
        }
        if ($expected) {
            Add-Check "Porta $port" 'OK' "serviço esperado: $($process.ProcessName) (PID $($listener.OwningProcess))" 'serviço local pronto'
        } else {
            Add-Check "Porta $port" 'BLOCKED' "ocupada por $($process.ProcessName) (PID $($listener.OwningProcess))" 'livre antes da inicialização'
        }
    } else {
        $requiredService = $port -in @(3306, 9092, 10000)
        if ($requiredService) {
            Add-Check "Porta $port" 'BLOCKED' 'serviço não está escutando' 'serviço local pronto'
        } else {
            Add-Check "Porta $port" 'OK' 'livre' 'livre antes da inicialização'
        }
    }
}

$results | Format-Table -AutoSize

if ($results.Status -contains 'BLOCKED') {
    exit 1
}

exit 0
