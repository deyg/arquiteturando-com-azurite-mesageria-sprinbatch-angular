# ADR-005 — MySQL 8.4.13 LTS como persistência

- Estado: ACEITO em 2026-10-23
- Contexto: a POC usa PostgreSQL para domínio e metadados Spring Batch; o usuário autorizou sua substituição por MySQL 8.4.13 LTS.
- Decisão: usar MySQL Community Server 8.4.13 LTS, InnoDB, `utf8mb4`, sessão UTC, UUID em `CHAR(36)`, Flyway e o schema MySQL oficial do Spring Batch.
- Operação local: distribuição ZIP portátil em `%LOCALAPPDATA%\payroll-export-poc`, sem Docker e sem serviço global do Windows.
- Consequências: SQL específico do PostgreSQL será convertido; dados existentes não serão migrados; contratos REST/Kafka e blobs não mudam.
- Rollback: a implementação fica isolada na branch `mysql`; `main` continua sendo a referência PostgreSQL.

