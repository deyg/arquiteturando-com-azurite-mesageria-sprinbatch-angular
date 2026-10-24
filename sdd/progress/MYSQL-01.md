# MYSQL-01 — Runtime MySQL portátil

- Estado: DONE
- Data: 2026-10-23
- Objetivo: disponibilizar e operar MySQL Community Server 8.4.13 LTS no Windows sem Docker ou serviço global.
- Requisitos: MYSQL-RF-06, MYSQL-RF-07.
- Critérios de aceite: MYSQL-AC-01, MYSQL-AC-06.
- Arquivos criados/alterados: `start-mysql.ps1`, `check-prereqs.ps1`, `run-demo.ps1`, `stop-local.ps1`.
- Comandos efetivamente executados: download do ZIP oficial; `mysqld --initialize-insecure`; inicialização na porta 3306; criação de database/usuário; `SELECT VERSION()`; `check-prereqs.ps1`.
- Resultado real: MySQL `8.4.13` ativo em `127.0.0.1:3306`; preflight reconheceu binários e processo esperados.
- Evidência objetiva: `mysqld.exe --version` e SQL retornaram `8.4.13`; processo `mysqld` escuta em 3306.
- Limitações/risco residual: credencial `payroll/payroll` é exclusiva da POC local e não serve para produção.
- Commit SHA completo: registrado no histórico Git da branch `mysql`.
- Próximo passo: MYSQL-02 — driver, Flyway e schemas.

