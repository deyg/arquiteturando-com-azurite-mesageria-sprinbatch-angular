# API local reproduzível

Todos os acessos do navegador e do consumidor da API usam somente o BFF em `http://localhost:8080`.

| Ação | cURL |
|---|---|
| Empregados | `curl "http://localhost:8080/api/v1/employees?startPeriod=2026-01&endPeriod=2026-06&paymentTypes=SALARY,BENEFIT"` |
| Pagamentos | `curl "http://localhost:8080/api/v1/employees/101/payments?startPeriod=2026-01&endPeriod=2026-06"` |
| Solicitar exportação | `curl -i -X POST "http://localhost:8080/api/v1/exports" -H "Content-Type: application/json" -d '{"employeeIds":[101],"startPeriod":"2026-01","endPeriod":"2026-01","paymentTypes":["SALARY"]}'` |
| Histórico sem protocolo | `curl "http://localhost:8080/api/v1/exports?employeeId=101&startPeriod=2026-01&endPeriod=2026-06&page=0&size=20"` |
| Status | `curl "http://localhost:8080/api/v1/exports/{requestId}"` |
| Documentos | `curl "http://localhost:8080/api/v1/exports/{requestId}/documents"` |
| Download | `curl -o comprovante.pdf "http://localhost:8080/api/v1/documents/{documentId}/download"` |

Execução local:

```powershell
cd sistemas
.\scripts\check-prereqs.ps1
.\scripts\start-local.ps1
.\scripts\smoke.ps1
.\scripts\stop-local.ps1
```

O script de parada encerra exclusivamente PIDs gravados pelo `start-local.ps1`; serviços que já ocupavam portas não são registrados nem interrompidos. MySQL, Kafka e Azurite são preservados.
