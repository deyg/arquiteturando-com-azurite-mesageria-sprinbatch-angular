# ARCH-001 — Arquitetura

```text
Angular :4200 -> BFF :8080 -> Request API :8081 -> Kafka :9092
                           -> Query API   :8082 -> PostgreSQL :5432
                                                -> Azurite :10000
Kafka -> Batch Worker :8083 -> PDF Motor :8084
             |              -> Azurite
             +--------------> PostgreSQL
```

## Fronteiras

| Componente | Pode acessar | Não pode acessar |
|---|---|---|
| Angular | BFF | APIs internas, Kafka, banco, Azurite |
| BFF | Request API, Query API | Kafka, banco, PDF Motor |
| Request API | Kafka | banco, Azurite, Batch |
| Query API | PostgreSQL, Azurite | Kafka, PDF Motor |
| Batch Worker | Kafka, PostgreSQL, PDF Motor, Azurite | estado do navegador |
| PDF Motor | payload HTTP | banco, Kafka, Azurite |

## Fluxo assíncrono

1. Request API cria `requestId`, publica e espera ack do Kafka.
2. BFF devolve `202` sem aguardar ingestão ou PDF.
3. Worker persiste a solicitação idempotentemente e confirma o offset.
4. Spring Batch materializa itens, renderiza e armazena um PDF por pagamento.
5. Consulta posterior recupera histórico e documentos sem estado do navegador.

## Consistência

Não existe transação distribuída. PostgreSQL, Kafka, HTTP e Azurite são reconciliados por IDs estáveis, constraints, retry finito e varredura de pendências.
