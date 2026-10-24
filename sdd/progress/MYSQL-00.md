# MYSQL-00 — Plano da migração MySQL

- Estado: DONE
- Data: 2026-10-23
- Objetivo: especificar, inventariar e validar a substituição do PostgreSQL pelo MySQL 8.4.13 LTS.
- Requisitos: MYSQL-RF-01 a MYSQL-RF-07.
- Critérios de aceite: plano cobre persistência, Batch, scripts, testes e rollback sem alterar contratos públicos.
- Arquivos criados/alterados: `spec-003-mysql.md`, `plan-003-mysql.md`, `ADR-005-mysql-8-4-lts.md`, `tasks.md` e este registro.
- Comandos efetivamente executados: inspeção Git; leitura integral do Playbook, Constituição e SPEC-001; busca de SQL/dialeto; verificação local de executáveis/serviços MySQL; consulta à documentação oficial MySQL.
- Resultado real: versão 8.4.13 LTS e binário Windows confirmados; MySQL não estava presente no ambiente; plano marcado APTO PARA EXECUÇÃO.
- Evidência objetiva: inventário de incompatibilidades e gates em `plan-003-mysql.md`.
- Limitações/risco residual: dados PostgreSQL existentes não fazem parte da migração; validação funcional depende das etapas MYSQL-01 a MYSQL-04.
- Commit SHA completo: registrado no histórico Git da branch `mysql`.
- Próximo passo: MYSQL-01 — runtime portátil e scripts Windows.

