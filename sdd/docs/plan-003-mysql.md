# PLAN-003 — Migração para MySQL 8.4.13 LTS

| Campo | Valor |
|---|---|
| Especificação | [SPEC-003](spec-003-mysql.md) |
| Decisão | [ADR-005](../adr/ADR-005-mysql-8-4-lts.md) |
| Estado | VALIDADO em 2026-10-23 |
| Branch | `mysql` |

## Estratégia

A migração preserva contratos e regras de negócio. Primeiro troca o dialeto e o bootstrap da infraestrutura; depois adapta o JDBC e as consultas; por fim executa regressão completa em uma instância MySQL 8.4.13 nova. O PostgreSQL não é mantido como fallback nesta branch, evitando uma matriz de dois dialetos sem necessidade para a POC.

## Etapas e gates

| Etapa | Entrega | Gate |
|---|---|---|
| MYSQL-00 | SPEC, ADR, plano e rastreabilidade | versão confirmada; escopo, riscos e rollback documentados |
| MYSQL-01 | Runtime portátil e scripts Windows | `mysqld --version`, porta 3306 e `SELECT VERSION()` = 8.4.13 |
| MYSQL-02 | Driver, Flyway e migrations MySQL | banco vazio migrado; 5 empregados e 20 pagamentos |
| MYSQL-03 | SQL de aplicação e mapeamento UUID | testes Query/Worker/Monitor verdes |
| MYSQL-04 | Regressão e documentação | Maven, Angular, smoke e E2E verdes; ausência de dependência operacional PostgreSQL |

## Inventário técnico validado

| Incompatibilidade PostgreSQL | Tratamento MySQL |
|---|---|
| `UUID` | `CHAR(36)` |
| `TIMESTAMPTZ` | `TIMESTAMP(6)` em sessão UTC |
| regex com `~` | `REGEXP` |
| `ON CONFLICT DO NOTHING` | `INSERT IGNORE` |
| parâmetro `?::uuid` | parâmetro string/UUID sem cast PostgreSQL |
| `JOIN ... USING(...)` | `JOIN ... ON ...` explícito |
| schema Batch com sequences | schema oficial MySQL com tabelas de sequência |
| driver/Flyway PostgreSQL | Connector/J e módulo Flyway MySQL |
| PostgreSQL local :5432 | MySQL portátil :3306 |

## Validação do plano

- Cobertura: migrations, dois consumidores JDBC, SQL embutido, Spring Batch, monitor, scripts, documentação e E2E foram inventariados.
- Reversibilidade: a branch `main` permanece PostgreSQL; a branch `mysql` isola a substituição.
- Dados: não há migração destrutiva; o runtime MySQL usa diretório próprio e seed fictícia.
- Segurança: credencial local exclusiva da POC; nenhum segredo real é versionado.
- Compatibilidade: MySQL 8.4.13 LTS e binário Windows x64 confirmados na documentação oficial em 2026-10-23.
- Risco principal: conversão UUID e SQL do repositório do monitor; mitigação por testes reais e E2E.
- Resultado da validação: **APTO PARA EXECUÇÃO**.

## Rollback

Trocar de volta para `main` restaura integralmente a variante PostgreSQL. Nenhum dado PostgreSQL será apagado ou transformado durante a execução desta branch.

