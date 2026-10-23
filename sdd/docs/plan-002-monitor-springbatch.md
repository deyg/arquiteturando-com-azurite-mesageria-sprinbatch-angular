# PLAN-002 — Monitor Spring Batch

| Campo | Valor |
|---|---|
| Especificação | [SPEC-002](spec-002-monitor-springbatch.md) |
| Decisão | [ADR-004](../adr/ADR-004-batch-monitor-observability.md) |
| Estado | APROVADO; implementação não iniciada |

## Estratégia

A evolução será feita dentro do `batch-worker`, sem novo aplicativo e sem rota no BFF. Primeiro será criada a leitura técnica do `JobRepository`; depois serão instrumentadas as fronteiras operacionais. Os endpoints consolidados só serão fechados após as métricas de origem terem testes determinísticos.

## Fases e gates

| Fase | Passos | Saída | Gate |
|---|---|---|---|
| Contrato | M-00–M-01 | SDD e OpenAPI interno | lint, exemplos e revisão de somente leitura |
| Metadados | M-02–M-04 | jobs, execuções, steps e correlação | testes reais no JobRepository e paginação |
| Desempenho e vazão | M-05–M-06 | duração, taxas, backlog e ativos | janelas UTC e resultados reproduzíveis |
| Instrumentação | M-07–M-10 | dependências, retry, recuperação e Prometheus | cardinalidade limitada e falha segura |
| Consolidação | M-11–M-12 | endpoints agregados e baseline | contrato, carga controlada e overhead aceitável |
| Entrega | M-13–M-14 | Postman, QA, regressão e relatório | Maven/E2E verdes e rastreabilidade completa |

## Baseline a registrar

O M-12 deve registrar, na máquina de teste e com massa declarada:

- jobs e documentos processados por minuto;
- documentos por segundo no `generateDocumentsStep`;
- duração média, p50, p95 e p99 de job, step, PDF, Blob e SQL;
- taxa de sucesso, parcial, falha, retry, skip e rollback;
- backlog máximo e tempo para drenagem;
- tempo de retomada após restart;
- consumo de CPU e memória antes e durante a coleta;
- overhead percentual com instrumentação habilitada.

Não será definida meta numérica de produção com base na POC. O baseline local será evidência comparativa para regressões futuras.

## Estratégia de testes

1. Testes unitários para cálculo de taxas, duração e validação de filtros.
2. Integração com PostgreSQL real para `JobRepository` e janelas históricas.
3. Falhas determinísticas para retry, skip, rollback e SQL pós-upload.
4. Redelivery Kafka e restart para métricas de recuperação.
5. Scrape Prometheus validando nomes, tipos e ausência de IDs nas tags.
6. Carga controlada repetível, com warm-up e parâmetros documentados.
7. Regressão integral do fluxo de exportação para provar não interferência.

## Critério de conclusão

A feature estará concluída somente quando AC-14 a AC-27 possuírem evidência, o histórico funcional permanecer inalterado, os endpoints forem somente leitura e a matriz RF→AC→teste→commit estiver atualizada.
