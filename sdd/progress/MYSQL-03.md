# MYSQL-03 — Consultas, UUID e monitor

- Estado: DONE
- Data: 2026-10-23
- Objetivo: adaptar ingestão, steps, histórico e monitor ao dialeto MySQL.
- Requisitos: MYSQL-RF-03, MYSQL-RF-04, MYSQL-RF-05.
- Critérios de aceite: MYSQL-AC-02, MYSQL-AC-04, MYSQL-AC-05.
- Arquivos criados/alterados: serviços Query/Worker, repositório do monitor, DTO de dependência, OpenAPI e testes JDBC.
- Comandos efetivamente executados: `mvnw.cmd -B test -rf :query-api`; `mvnw.cmd -B clean verify`; chamadas aos endpoints internos do monitor.
- Resultado real: Batch Worker 10/10 testes; build completo 20/20; idempotência e metadados Batch validados no MySQL.
- Evidência objetiva: `/internal/v1/batch/jobs` retornou o job; `/metrics/dependencies` retornou `MYSQL`; tabelas Batch possuem execuções.
- Limitações/risco residual: percentis históricos são calculados em Java porque MySQL 8.4 não oferece `percentile_cont` equivalente ao PostgreSQL.
- Commit SHA completo: registrado no histórico Git da branch `mysql`.
- Próximo passo: MYSQL-04 — regressão E2E e documentação.

