# MYSQL-04 — Regressão e fechamento

- Estado: DONE
- Data: 2026-10-23
- Objetivo: provar a jornada completa e alinhar documentação ativa com MySQL.
- Requisitos: MYSQL-RF-01 a MYSQL-RF-07.
- Critérios de aceite: MYSQL-AC-01 a MYSQL-AC-06.
- Arquivos criados/alterados: Constituição, SPEC-001/002, arquitetura, READMEs, ADR de consistência, contrato do monitor e documentos MYSQL-00 a MYSQL-04.
- Comandos efetivamente executados: `mvnw.cmd -B clean verify`; `run-demo.ps1 -SkipBuild -NoBrowser`; requisição via Angular `:4200`; endpoints do monitor; `npm test -- --watch=false`; `npm run build`; `git diff --check`.
- Resultado real: Maven 20/20 testes; Angular 3/3; PDF válido gerado; cinco empregados consultados pela UI; monitor e schema Batch funcionais no MySQL 8.4.13.
- Evidência objetiva: PDF `payroll-401003e8-e8ba-430d-8974-79d6047ae83a.pdf`; 5 empregados, 20 pagamentos e 16 execuções Batch no momento da consulta.
- Limitações/risco residual: a distribuição ZIP do MySQL precisa ser provisionada no runtime local; não há migração de dados PostgreSQL nem configuração produtiva.
- Commit SHA completo: registrado no histórico Git da branch `mysql`.
- Próximo passo: nenhum; PLAN-003 concluído.

