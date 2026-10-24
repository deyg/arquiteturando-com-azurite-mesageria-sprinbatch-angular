# MYSQL-02 — Persistência e schemas MySQL

- Estado: DONE
- Data: 2026-10-23
- Objetivo: converter dependências, conexão JDBC, migrations de domínio, seed e schema Spring Batch.
- Requisitos: MYSQL-RF-01, MYSQL-RF-02, MYSQL-RF-03.
- Critérios de aceite: MYSQL-AC-01, MYSQL-AC-02.
- Arquivos criados/alterados: POMs de Query/Worker, `application.yml`, migrations V1 a V3 e testes de fixtures UUID.
- Comandos efetivamente executados: Flyway via testes Spring Boot; consultas de contagem pelo cliente MySQL; Maven Query API.
- Resultado real: três migrations aplicadas; 5 empregados, 20 pagamentos e tabelas oficiais `BATCH_*`; Query API 7/7 testes.
- Evidência objetiva: Flyway validou três migrations no MySQL 8.4; Connector/J abriu pool JDBC; UUID `CHAR(36)` fez round-trip.
- Limitações/risco residual: não migra dados de uma instância PostgreSQL anterior.
- Commit SHA completo: registrado no histórico Git da branch `mysql`.
- Próximo passo: MYSQL-03 — SQL de aplicação, monitor e Batch.

