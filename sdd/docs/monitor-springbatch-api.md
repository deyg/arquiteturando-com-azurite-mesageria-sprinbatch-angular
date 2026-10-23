# API interna — Monitor Spring Batch

Base local: `http://localhost:8083/internal/v1/batch`.

## Endpoints

| Método | Endpoint | Finalidade |
|---|---|---|
| GET | `/jobs` | nomes dos jobs conhecidos |
| GET | `/executions` | execuções paginadas e filtradas |
| GET | `/executions/{executionId}` | execução, parâmetros e saída |
| GET | `/executions/{executionId}/steps` | métricas e estado dos steps |
| GET | `/requests/{requestId}` | correlação da solicitação com Batch |
| GET | `/metrics/summary` | visão consolidada da janela |
| GET | `/metrics/performance` | durações, latências e percentis |
| GET | `/metrics/throughput` | vazão, taxas, backlog e ativos |
| GET | `/metrics/resilience` | retry, skip, rollback, redelivery e recuperação |
| GET | `/metrics/dependencies` | estado observado das dependências |

`/executions` aceita `jobName`, `status`, `requestId`, `startedFrom`, `startedTo`, `page` e `size`. Endpoints de métricas aceitam `from`, `to`, `jobName`, `stepName` e `status`. Datas usam ISO-8601 UTC; `page` começa em zero; `size` varia de 1 a 100.

## Resposta resumida de referência

```json
{
  "window": {"from": "2026-10-23T00:00:00Z", "to": "2026-10-24T00:00:00Z"},
  "jobs": {"started": 120, "completed": 112, "failed": 3, "partial": 5, "running": 0, "successRate": 0.9333},
  "documents": {"read": 780, "completed": 752, "failed": 28, "documentsPerSecond": 4.7},
  "performance": {"averageJobDurationMs": 5230, "p95JobDurationMs": 8100, "averagePdfLatencyMs": 240, "p95PdfLatencyMs": 510},
  "resilience": {"pdfRetries": 16, "skippedItems": 8, "rollbacks": 2, "kafkaRedeliveries": 3, "recoveredJobs": 1}
}
```

## Erros e segurança

- `400`: filtros, paginação ou janela inválidos;
- `404`: execução ou solicitação inexistente;
- `500`: falha inesperada de leitura;
- respostas não incluem payload Kafka, PDF ou dados pessoais;
- não existem métodos mutáveis (`POST`, `PUT`, `PATCH`, `DELETE`) nesta versão.

## Actuator

O plano inclui `/actuator/health`, `/actuator/metrics` e `/actuator/prometheus`. A exposição deve ser restrita ao ambiente interno e configurável.
