[CmdletBinding()]
param(
    # Evita recompilar quando os JARs ja foram gerados anteriormente.
    [switch]$SkipBuild,

    # Sobe somente a infraestrutura e as aplicacoes Java.
    [switch]$NoAngular,

    # Nao abre o navegador automaticamente depois que o Angular ficar pronto.
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Stop'

# Diretorios usados pelo projeto e pelos runtimes instalados localmente.
$systems = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeRoot = Join-Path $env:LOCALAPPDATA 'payroll-export-poc'
$kafkaRoot = Join-Path $runtimeRoot 'kafka\kafka_2.13-4.3.1'
$stateDir = Join-Path $systems '.runtime'
$logDir = Join-Path $stateDir 'logs'
$processState = Join-Path $stateDir 'processes.json'
$angularPid = Join-Path $stateDir 'angular.pid'
$angularDir = Join-Path $systems 'apps\web-angular'

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# Inclui no PATH os runtimes locais previstos pelo projeto.
$runtimePaths = @(
    (Join-Path $runtimeRoot 'node\node-v26.12.1-win-x64'),
    (Join-Path $runtimeRoot 'npm'),
    (Join-Path $runtimeRoot 'mysql\mysql-8.4.13-winx64\bin'),
    (Join-Path $runtimeRoot 'kafka\kafka_2.13-4.3.1\bin\windows')
) | Where-Object { Test-Path $_ }

if ($runtimePaths) {
    $env:Path = (($runtimePaths -join ';') + ';' + $env:Path)
}

function Test-Port {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Wait-Port {
    param([int]$Port, [int]$TimeoutSeconds = 90)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) { return }
        Start-Sleep -Seconds 2
    }
    throw "Timeout aguardando a porta $Port."
}

function Wait-ProcessPort {
    param(
        [System.Diagnostics.Process]$Process,
        [int]$Port,
        [int]$TimeoutSeconds,
        [string]$ErrorLog
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) { return }
        if ($Process.HasExited) {
            $details = if (Test-Path $ErrorLog) {
                (Get-Content $ErrorLog -Tail 20) -join [Environment]::NewLine
            } else {
                'Nenhum log de erro foi produzido.'
            }
            throw "O processo encerrou antes de abrir a porta $Port.`n$details"
        }
        Start-Sleep -Seconds 2
    }
    throw "Timeout aguardando a porta $Port. Consulte $ErrorLog"
}

function Wait-Health {
    param([string]$Name, [int]$Port, [int]$TimeoutSeconds = 120)

    $url = "http://127.0.0.1:$Port/actuator/health"
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Uri $url -TimeoutSec 5
            if ($response.status -eq 'UP') {
                Write-Host "$Name pronto em $url" -ForegroundColor Green
                return
            }
        } catch {
            # O processo ainda pode estar inicializando; a proxima tentativa confirmara.
        }
        Start-Sleep -Seconds 2
    }
    throw "Timeout aguardando o health check de $Name em $url."
}

function Assert-Command {
    param([string]$Command)
    if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) {
        throw "Comando obrigatorio nao encontrado no PATH: $Command"
    }
}

$startedJava = [System.Collections.Generic.List[object]]::new()

function Start-JavaApplication {
    param([string]$Name, [int]$Port)

    if (Test-Port $Port) {
        Write-Host "$Name ja esta usando a porta $Port; apenas validando o health check."
        Wait-Health -Name $Name -Port $Port
        return
    }

    $jar = Join-Path $systems "apps\$Name\target\$Name-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
        throw "JAR nao encontrado: $jar. Execute sem -SkipBuild."
    }

    $process = Start-Process -FilePath 'java' `
        -ArgumentList @('-jar', $jar) `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir "$Name.out.log") `
        -RedirectStandardError (Join-Path $logDir "$Name.err.log") `
        -PassThru

    $startedJava.Add([pscustomobject]@{
        name = $Name
        port = $Port
        pid = $process.Id
        startedAt = (Get-Date).ToString('o')
    })

    # Cada aplicacao deve estar saudavel antes que sua dependente seja iniciada.
    Wait-Health -Name $Name -Port $Port
}

Write-Host 'PASSO 1 - Validando ferramentas' -ForegroundColor Cyan
Assert-Command 'java'
Assert-Command 'azurite-blob.cmd'
if (-not (Test-Path (Join-Path $kafkaRoot 'libs'))) {
    throw "Kafka 4.3.1 nao encontrado em $kafkaRoot"
}
if (-not $SkipBuild) { Assert-Command (Join-Path $systems 'mvnw.cmd') }
if (-not $NoAngular) {
    Assert-Command 'node'
    Assert-Command 'npm.cmd'
}

Write-Host 'PASSO 2 - Subindo o MySQL' -ForegroundColor Cyan
# O script cria, se necessario, o database payroll e o usuario local payroll.
if (-not (Test-Port 3306)) {
    & (Join-Path $PSScriptRoot 'start-mysql.ps1')
}
Wait-Port -Port 3306 -TimeoutSeconds 60

Write-Host 'PASSO 3 - Subindo o Kafka em modo KRaft' -ForegroundColor Cyan
if (-not (Test-Port 9092)) {
    $kafkaConfig = Join-Path $runtimeRoot 'kafka-server.properties'
    if (-not (Test-Path $kafkaConfig)) { throw "Configuracao do Kafka nao encontrada: $kafkaConfig" }

    # O kafka-server-start.bat 4.3.1 pode ultrapassar o limite de tamanho da
    # linha de comando no Windows. O classpath curinga e expandido pela JVM e
    # inicia a mesma classe kafka.Kafka sem montar uma lista enorme de JARs.
    $kafkaErrorLog = Join-Path $logDir 'kafka.err.log'
    $kafkaProcess = Start-Process -FilePath 'java' `
        -ArgumentList @(
            '-Xms512M',
            '-Xmx512M',
            "-Dlog4j2.configurationFile=$(Join-Path $kafkaRoot 'config\log4j2.yaml')",
            '-cp',
            (Join-Path $kafkaRoot 'libs\*'),
            'kafka.Kafka',
            $kafkaConfig
        ) `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir 'kafka.out.log') `
        -RedirectStandardError $kafkaErrorLog `
        -PassThru
    Wait-ProcessPort -Process $kafkaProcess -Port 9092 -TimeoutSeconds 90 -ErrorLog $kafkaErrorLog
}
if (-not (Test-Port 9092)) { Wait-Port -Port 9092 -TimeoutSeconds 90 }

Write-Host 'PASSO 4 - Subindo o Azurite Blob' -ForegroundColor Cyan
if (-not (Test-Port 10000)) {
    Start-Process -FilePath 'azurite-blob.cmd' `
        -ArgumentList @('--location', (Join-Path $runtimeRoot 'azurite-data'), '--blobHost', '127.0.0.1', '--blobPort', '10000', '--silent') `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeRoot 'azurite.out.log') `
        -RedirectStandardError (Join-Path $runtimeRoot 'azurite.err.log') | Out-Null
}
Wait-Port -Port 10000 -TimeoutSeconds 30

Write-Host 'PASSO 5 - Compilando as aplicacoes Java' -ForegroundColor Cyan
if ($SkipBuild) {
    Write-Host 'Build ignorado por causa do parametro -SkipBuild.'
} else {
    # O Maven procura o pom.xml no diretorio atual; por isso o build deve ser
    # executado na raiz sistemas, mesmo quando este script e chamado de scripts.
    Push-Location $systems
    try {
        & (Join-Path $systems 'mvnw.cmd') -q -DskipTests package
        if ($LASTEXITCODE) { throw 'Falha no build Maven.' }
    } finally {
        Pop-Location
    }
}

Write-Host 'PASSO 6 - Subindo a query-api e executando o Flyway' -ForegroundColor Cyan
# A query-api executa V1 (dominio), V2 (Spring Batch) e V3 (dados ficticios).
# O health check UP confirma que as migracoes terminaram com sucesso.
Start-JavaApplication -Name 'query-api' -Port 8082

Write-Host 'PASSO 7 - Subindo o motor de PDF' -ForegroundColor Cyan
Start-JavaApplication -Name 'pdf-motor-api' -Port 8084

Write-Host 'PASSO 8 - Subindo o worker Spring Batch' -ForegroundColor Cyan
# O worker inicia somente depois que as tabelas BATCH_* existem no MySQL.
Start-JavaApplication -Name 'batch-worker' -Port 8083

Write-Host 'PASSO 9 - Subindo a API de solicitacoes' -ForegroundColor Cyan
Start-JavaApplication -Name 'request-api' -Port 8081

Write-Host 'PASSO 10 - Subindo o BFF' -ForegroundColor Cyan
Start-JavaApplication -Name 'bff-api' -Port 8080

# Mantem compatibilidade com scripts/stop-local.ps1.
if ($startedJava.Count -gt 0) {
    ConvertTo-Json -InputObject @($startedJava) | Set-Content -Encoding UTF8 $processState
}

if (-not $NoAngular) {
    Write-Host 'PASSO 11 - Instalando dependencias e subindo o Angular' -ForegroundColor Cyan
    if (-not (Test-Path (Join-Path $angularDir 'node_modules'))) {
        Push-Location $angularDir
        try {
            # npm ci usa exatamente as versoes registradas no package-lock.json.
            & npm.cmd ci
            if ($LASTEXITCODE) { throw 'Falha ao instalar as dependencias do Angular.' }
        } finally {
            Pop-Location
        }
    }

    if (-not (Test-Port 4200)) {
        $ng = Start-Process -FilePath $env:ComSpec `
            -ArgumentList @('/c', 'npm.cmd start -- --host 127.0.0.1') `
            -WorkingDirectory $angularDir `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $logDir 'angular.out.log') `
            -RedirectStandardError (Join-Path $logDir 'angular.err.log') `
            -PassThru
        Set-Content -Encoding ASCII -Path $angularPid -Value $ng.Id
    }
    Wait-Port -Port 4200 -TimeoutSeconds 120
}

Write-Host ''
Write-Host 'Ambiente iniciado com sucesso.' -ForegroundColor Green
if (-not $NoAngular) {
    $applicationUrl = 'http://127.0.0.1:4200'
    Write-Host "Aplicacao: $applicationUrl" -ForegroundColor Green
    if (-not $NoBrowser) {
        Start-Process $applicationUrl
    }
}
Write-Host 'Metricas:  http://127.0.0.1:8083/actuator/prometheus'
Write-Host "Parar Java: & '$(Join-Path $PSScriptRoot 'stop-local.ps1')'"
Write-Host 'MySQL, Kafka e Azurite permanecem ativos apos parar as aplicacoes.'
