# API-001 — Contratos

As fontes executáveis são:

- `sistemas/packages/contracts/openapi/payroll-api.yaml`
- `sistemas/packages/contracts/events/payroll-export-requested-v1.schema.json`

Somente o BFF expõe rotas públicas. O evento usa `requestId` como chave Kafka e `eventVersion=1`. Identificadores Spring Batch não fazem parte do contrato público.

## Paginação

`GET /api/v1/exports` usa `page` baseado em zero e `size` entre 1 e 100. A ordenação padrão é `createdAt DESC, requestId DESC`.

## Erros

Erros HTTP usam `application/problem+json`. Falha de publicação não retorna `202`; usa `502` ou `503`. Uma consulta imediata pode não encontrar uma solicitação ainda não ingerida.

## API técnica interna planejada

O `batch-worker` terá endpoints somente leitura sob `/internal/v1/batch` para metadados, performance, vazão e resiliência. Eles não integram o contrato público do BFF e não devem ser consumidos pelo Angular.

O contrato aprovado está descrito em [monitor-springbatch-api.md](monitor-springbatch-api.md), e sua fonte OpenAPI executável está em `sistemas/packages/contracts/openapi/batch-monitor-api.yaml`. A implementação deve também publicar métricas Micrometer em `/actuator/metrics` e formato Prometheus em `/actuator/prometheus`, com exposição configurável.
