# SPEC-003 — Migração de PostgreSQL para MySQL

| Campo | Valor |
|---|---|
| Estado | APROVADO pelo usuário em 2026-10-23 |
| Versão alvo | MySQL Community Server 8.4.13 LTS |
| Escopo | Substituição integral do PostgreSQL no ambiente local |
| Contratos públicos | Inalterados |

## Objetivo

Substituir o PostgreSQL pelo MySQL 8.4.13 LTS como banco do domínio e do `JobRepository` do Spring Batch, mantendo as jornadas, contratos HTTP/Kafka, regras de idempotência, estados e armazenamento de PDFs no Azurite.

## Requisitos

| ID | Requisito | Verificação |
|---|---|---|
| MYSQL-RF-01 | Query API e Batch Worker usam exclusivamente MySQL | driver, JDBC URL e processos inspecionados |
| MYSQL-RF-02 | Flyway cria domínio, seed e metadados Spring Batch em banco vazio | migrations aplicadas e contagens SQL |
| MYSQL-RF-03 | UUIDs permanecem UUIDs no Java e são persistidos como `CHAR(36)` | round-trip JDBC e testes de integração |
| MYSQL-RF-04 | Ingestão e preparação continuam idempotentes | redelivery/restart sem duplicação |
| MYSQL-RF-05 | Monitor Spring Batch mantém filtros, paginação e métricas | testes do monitor e endpoints reais |
| MYSQL-RF-06 | Execução local usa MySQL 8.4.13 LTS portátil no Windows | preflight e `SELECT VERSION()` |
| MYSQL-RF-07 | PostgreSQL deixa de ser requisito operacional | busca estrutural, scripts e documentação |

## Decisões de compatibilidade

- UUID de domínio: `CHAR(36)`, priorizando legibilidade e conversão JDBC previsível nesta POC.
- Instantes: `TIMESTAMP(6)` em UTC; a JDBC URL fixa `connectionTimeZone=UTC` e `forceConnectionTimeZoneToSession=true`.
- Valores monetários: `DECIMAL(15,2)` e `BigDecimal`.
- Idempotência: `INSERT IGNORE` apoiado pelas mesmas chaves primárias e restrições únicas.
- Paginação: `LIMIT :size OFFSET :offset`, suportada pelo MySQL 8.4.
- Metadados Batch: schema MySQL oficial compatível com a versão do Spring Batch usada pelo projeto.
- Dados existentes no PostgreSQL não serão migrados; esta branch cria um runtime MySQL local novo com a massa fictícia versionada.

## Critérios de aceite

| ID | Critério |
|---|---|
| MYSQL-AC-01 | MySQL responde exatamente como versão `8.4.13` e o schema Flyway inicia do zero. |
| MYSQL-AC-02 | Build Maven e todos os testes Java passam contra MySQL. |
| MYSQL-AC-03 | Smoke e E2E geram e baixam PDF válido usando MySQL. |
| MYSQL-AC-04 | Redelivery mantém uma solicitação e um documento lógico por comprovante. |
| MYSQL-AC-05 | Endpoints do monitor consultam metadados Batch persistidos no MySQL. |
| MYSQL-AC-06 | Scripts de preflight/start/stop não dependem de PostgreSQL. |

## Fora do escopo

- Migração de dados de uma instância PostgreSQL existente.
- Alta disponibilidade, replicação, backup produtivo e tuning de produção.
- Alteração dos contratos REST, Kafka, Angular, PDF Motor ou Azurite.

