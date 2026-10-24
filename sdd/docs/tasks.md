# TASKS-001

| Faixa | Entrega |
|---|---|
| P-00–P-02 | Governança, preflight, domínio, arquitetura e contratos |
| P-03–P-07 | Estrutura, banco, consultas, PDF e storage |
| P-08–P-13 | Publicação Kafka, ingestão e processamento Batch recuperável |
| P-14–P-17 | APIs de consulta, BFF e jornadas Angular |
| P-18–P-20 | Operação, E2E, rastreabilidade e release |

## Evolução aprovada — SPEC-002 Monitor Spring Batch

Plano detalhado: [PLAN-002](plan-002-monitor-springbatch.md).

| Passo | Entrega | Validação |
|---|---|---|
| M-00 | SDD, ADR e limites da feature | revisão RF-19–RF-28, RNF-10–RNF-20 e AC-14–AC-27 |
| M-01 | OpenAPI interno e DTOs | lint e testes de contrato |
| M-02 | Lista paginada de jobs e execuções | filtros, ordenação e limites |
| M-03 | Detalhes, parâmetros e steps | jobs completos, falhos e ativos |
| M-04 | Correlação por `requestId` | vínculo técnico e funcional |
| M-05 | Performance histórica | durações e contadores conferidos no JobRepository |
| M-06 | Vazão, taxas, backlog e ativos | testes com janela UTC controlada |
| M-07 | Instrumentação do PDF Motor | timers, histogramas, retry e falha |
| M-08 | Instrumentação Blob e SQL | latência, retry e falha pós-upload |
| M-09 | Kafka e recuperação | redelivery, retomada e tempo de recuperação |
| M-10 | Micrometer e Prometheus | scrape, nomes e cardinalidade |
| M-11 | Endpoints consolidados | performance, throughput, resilience e dependencies |
| M-12 | Carga controlada | baseline de vazão, latência e overhead |
| M-13 | Postman e QA manual | execução reproduzível |
| M-14 | Regressão e relatório | Maven, E2E e matriz de evidências |

Cada passo M produziu evidência em `sdd/progress/` e commit dedicado. A implementação foi concluída e verificada em 2026-10-23.

## Evolução aprovada — SPEC-003 MySQL 8.4.13 LTS

Plano detalhado: [PLAN-003](plan-003-mysql.md).

| Passo | Entrega | Validação |
|---|---|---|
| MYSQL-00 | SPEC, ADR, plano e inventário | plano APTO PARA EXECUÇÃO |
| MYSQL-01 | Runtime MySQL portátil e scripts | versão 8.4.13, porta 3306 e conexão SQL |
| MYSQL-02 | Driver, Flyway e schemas | migrations e seed em banco vazio |
| MYSQL-03 | Consultas e UUID | testes Query, Worker e Monitor |
| MYSQL-04 | Regressão e documentação | Maven, Angular, smoke e E2E |
