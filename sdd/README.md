# Payroll Export POC — Pacote SDD v0.1

**Estado: documentação aprovada em 2026-10-23; implementação ainda não iniciada.**

Conteúdo:

- `docs/constitution.md`: missão, princípios vinculantes, stack, limites, transações, governança.
- `docs/spec-001.md`: regras de negócio, requisitos rastreáveis, fluxos, APIs preliminares, eventos e Gherkin.
- `../sistemas/postman/payroll-export-poc.postman_collection.json`: Collection Postman preliminar dos endpoints propostos.
- `../sistemas/postman/local.postman_environment.json`: Variáveis locais de API e IDs de exemplo.

## Como usar no SDD/Codex

1. Revise primeiro `docs/constitution.md` e `docs/spec-001.md`.
2. A documentação foi aprovada; preserve o modo supervisionado e execute somente a etapa autorizada.
3. Após autorização específica, avance para `DOMAIN-001`, `ARCH-001`, `API-001`, `PLAN-001` e `TASKS-001`.
4. Implemente apenas após aprovar o Playbook, um passo rastreável por vez.

## Recursos locais previstos

Java 21 · Spring Boot 4.1.x · Spring Batch 6.0.x · Angular 22 · Kafka 4.3.1 KRaft · PostgreSQL · Azurite Blob · Maven · Node.js compatível.

> Nenhuma aplicação está implementada neste pacote. cURLs e Collection definem os contratos alvo, portanto só funcionarão depois dos respectivos serviços estarem em execução.

## Tabela rápida de cURL (contratos previstos)

| Ação | cURL |
|---|---|
| Empregados | `curl "http://localhost:8080/api/v1/employees?startPeriod=2026-01&endPeriod=2026-06&paymentTypes=SALARY,BENEFIT"` |
| Pagamentos | `curl "http://localhost:8080/api/v1/employees/101/payments?startPeriod=2026-01&endPeriod=2026-06"` |
| Exportar | `curl -X POST http://localhost:8080/api/v1/exports -H "Content-Type: application/json" -d '{"employeeIds":[101,102],"startPeriod":"2026-01","endPeriod":"2026-06","paymentTypes":["SALARY","BENEFIT"]}'` (Git Bash) |
| Histórico | `curl "http://localhost:8080/api/v1/exports?employeeId=101&startPeriod=2026-01&endPeriod=2026-06&page=0&size=20"` |
| Status | `curl "http://localhost:8080/api/v1/exports/{requestId}"` |
| Listar documentos | `curl "http://localhost:8080/api/v1/exports/{requestId}/documents"` |
| PDF | `curl -L -o comprovante.pdf "http://localhost:8080/api/v1/documents/{documentId}/download"` |

No Postman, `Consultar histórico por filtros` pode ser executado sem `requestId`. Para as consultas de status e documentos, execute antes `POST Solicitar exportação`; o script de teste captura o `requestId` do retorno 202. Os IDs dos exemplos são variáveis substituíveis.

## Documentação oficial

- https://docs.spring.io/spring-boot/reference/
- https://docs.spring.io/spring-batch/reference/
- https://angular.dev/reference/versions
- https://kafka.apache.org/community/downloads/
- https://learn.microsoft.com/en-us/azure/storage/common/storage-install-azurite
