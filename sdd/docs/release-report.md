# Relatório final e rastreabilidade

Data da verificação: 2026-10-23.

## Requisitos, aceite, testes e commits

| Requisitos | Aceite | Evidência principal | Commit |
|---|---|---|---|
| RF-01 a RF-04 | AC-01 | testes Query API/BFF e Angular; smoke de empregados/pagamentos | `a88154e`, `93370e7`, `8773420` |
| RF-05, RF-06 | AC-02, AC-07 | `ExportPublisherTest`, `ExportPublisherUnavailableTest` | `8a20838`, `0e689eb` |
| RF-07, RF-08 | AC-03, AC-08, AC-11 | testes de redelivery e coordenador; retomada no startup | `1e991cc`, `5c39a33`, `0e689eb` |
| RF-09 | AC-05, AC-06 | `PrepareRequestStepTest` com e sem pagamentos | `c7c4185` |
| RF-10, RF-11 | AC-05, AC-10 | testes do PDF Motor e de falha parcial/total | `65dc335`, `d971b69`, `bbdf0c6` |
| RF-12, RF-13 | AC-05, AC-09 | Azurite real e falha SQL injetada após upload | `85ee81c`, `d971b69`, `0e689eb` |
| RF-14 a RF-16 | AC-04, AC-12 | histórico/status/documentos/download e testes Angular | `30c1f39`, `e6e05bc` |
| RF-17 | AC-08 a AC-11 | retry finito, reconciliação e job idempotente | `bbdf0c6`, `0e689eb` |
| RF-18 | AC-13 | scripts, Postman e execução nativa Windows | `2b98060`, `0e689eb` |
| RNF-01 a RNF-09 | AC-02 a AC-13 | preflight, Flyway, health, timeouts, logs, armazenamento Blob | `034c1e2` a `0e689eb` |

## Execução observada

| Verificação | Resultado |
|---|---|
| Preflight Windows | Java, Maven, Node, npm, PostgreSQL, Kafka e Azurite disponíveis |
| Smoke pelo BFF | 5/5 chamadas aprovadas |
| E2E real | solicitação `3225b8f5-762c-4476-8b58-6bdb1cf02882`, `COMPLETED`, 1 documento, download `%PDF-` |
| Reactor Maven | 6/6 módulos, 20/20 testes aprovados |
| Request API | 2/2 testes aprovados, incluindo broker ausente |
| Batch Worker | 8/8 testes aprovados, incluindo redelivery e falhas controladas |
| Angular | 3/3 testes aprovados e build de produção gerado |

Os relatórios detalhados de cada passo estão em `sdd/progress/P-00.md` a `P-20.md`. Os SHAs completos permanecem no histórico Git; cada passo P-00 a P-20 corresponde a um commit dedicado.

## Pendências para produção

- identidade e controle de acesso;
- TLS e gestão externa de segredos;
- observabilidade centralizada e alertas;
- política formal de retenção de PDFs e dados;
- testes de carga, alta disponibilidade e recuperação de desastre.

Avisos não bloqueantes observados: APIs `JobLauncher`/`JobExplorer` marcadas para remoção futura pelo Spring Batch, autoanexação do agente Mockito que mudará em JDK futuro e versões Netty divergentes reportadas pelo SDK Azure. Nenhum deles causou falha funcional ou de build nesta versão.

## Evolução posterior ao release

A [SPEC-002](spec-002-monitor-springbatch.md) adicionou observabilidade técnica ao `batch-worker`, cobrindo RF-19 a RF-28, RNF-10 a RNF-20 e AC-14 a AC-27.

| Faixa | Evidência | Commits |
|---|---|---|
| Contrato e metadados | OpenAPI, DTOs, JobRepository JDBC, paginação, detalhes e correlação | `b958ffc` a `3c75ee4` |
| Performance e vazão | duração, percentis, taxas, backlog e ativos | `6aae1f1`, `3a17dd1` |
| Resiliência | PDF, Blob, SQL, Kafka, redelivery e recuperação | `4de0d29` a `2292bcd` |
| Exposição | Prometheus e endpoints consolidados | `c0e0192`, `f6ea71c` |
| Operação | baseline, Postman e QA | `2c1f35d`, `f030fbe` |

Baseline com 5 solicitações: 2,082 solicitações/s; job médio 188,286 ms; p95 504,589 ms; sucesso 100% e backlog zero. Validação HTTP encontrou 1 job, 24 execuções persistidas, três steps por execução, correlação por UUID e métricas Prometheus. Gate final: 6/6 módulos Maven e 22/22 testes; Angular 3/3 e build de produção.
