# ARCH-001 — Arquitetura

```text
Angular :4200 -> BFF :8080 -> Request API :8081 -> Kafka :9092
                           -> Query API   :8082 -> MySQL :3306
                                                -> Azurite :10000
Kafka -> Batch Worker :8083 -> PDF Motor :8084
             |              -> Azurite
             +--------------> MySQL
             +--------------> Monitor interno /internal/v1/batch
                              -> JobRepository + Micrometer
```

## Fronteiras

| Componente | Pode acessar | Não pode acessar |
|---|---|---|
| Angular | BFF | APIs internas, Kafka, banco, Azurite |
| BFF | Request API, Query API | Kafka, banco, PDF Motor |
| Request API | Kafka | banco, Azurite, Batch |
| Query API | MySQL, Azurite | Kafka, PDF Motor |
| Batch Worker | Kafka, MySQL, PDF Motor, Azurite | estado do navegador |
| PDF Motor | payload HTTP | banco, Kafka, Azurite |
| Monitor Spring Batch | JobRepository, metadados do domínio e Micrometer | BFF, Angular, comandos de alteração de jobs |

## Fluxo assíncrono

1. Request API cria `requestId`, publica e espera ack do Kafka.
2. BFF devolve `202` sem aguardar ingestão ou PDF.
3. Worker persiste a solicitação idempotentemente e confirma o offset.
4. Spring Batch materializa itens, renderiza e armazena um PDF por pagamento.
5. Consulta posterior recupera histórico e documentos sem estado do navegador.

## Consistência

Não existe transação distribuída. MySQL, Kafka, HTTP e Azurite são reconciliados por IDs estáveis, constraints, retry finito e varredura de pendências.

## Observabilidade técnica planejada

O `batch-worker` hospedará o `monitor-springbatch` na porta `8083`. A feature será interna e somente leitura, combinando:

- `JobRepository` para instâncias, execuções, parâmetros, steps e contadores históricos;
- domínio MySQL para correlação por `requestId` e backlog;
- Micrometer para timers, counters, gauges, histogramas, percentis e scrape Prometheus.

O histórico funcional continua pertencendo à Query API. O monitor técnico não será exposto pelo BFF. Ver [SPEC-002](spec-002-monitor-springbatch.md) e [ADR-004](../adr/ADR-004-batch-monitor-observability.md).
