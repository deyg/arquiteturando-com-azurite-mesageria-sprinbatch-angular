[CmdletBinding()]
param(
    [string]$BffUrl='http://127.0.0.1:8080',
    [string]$WorkerUrl='http://127.0.0.1:8083',
    [int]$Requests=5,
    [int]$TimeoutSeconds=120
)
$ErrorActionPreference='Stop'
if($Requests -lt 1 -or $Requests -gt 100){throw 'Requests deve estar entre 1 e 100.'}
$from=(Get-Date).ToUniversalTime().AddSeconds(-1)
$ids=@();$watch=[Diagnostics.Stopwatch]::StartNew()
for($i=0;$i -lt $Requests;$i++){
    $body=@{employeeIds=@(101);startPeriod='2026-01';endPeriod='2026-01';paymentTypes=@('SALARY')}|ConvertTo-Json
    $ids+=(Invoke-RestMethod -Method Post -Uri "$BffUrl/api/v1/exports" -ContentType 'application/json' -Body $body).requestId
}
$deadline=(Get-Date).AddSeconds($TimeoutSeconds)
foreach($id in $ids){
    do{Start-Sleep -Milliseconds 250;try{$r=Invoke-RestMethod "$BffUrl/api/v1/exports/$id"}catch{$r=$null}}
    while(($null-eq$r -or $r.status-notin@('COMPLETED','PARTIAL','FAILED'))-and(Get-Date)-lt$deadline)
    if($null-eq$r -or $r.status-ne'COMPLETED'){throw "Solicitação $id não concluiu."}
}
$watch.Stop();$to=(Get-Date).ToUniversalTime().AddSeconds(1)
$query="from=$([uri]::EscapeDataString($from.ToString('o')))&to=$([uri]::EscapeDataString($to.ToString('o')))"
$summary=Invoke-RestMethod "$WorkerUrl/internal/v1/batch/metrics/summary?$query"
[pscustomobject]@{requests=$Requests;elapsedMs=$watch.ElapsedMilliseconds;requestsPerSecond=[math]::Round($Requests/[math]::Max(.001,$watch.Elapsed.TotalSeconds),3);summary=$summary}|ConvertTo-Json -Depth 10
