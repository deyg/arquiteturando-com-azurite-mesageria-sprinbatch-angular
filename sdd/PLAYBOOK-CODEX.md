# Playbook Codex — Payroll Export POC

> **Versão:** 1.0-rc1
> **Data:** 2026-10-23
> **Status:** APROVADO em 2026-10-23 — execução permanece no modo supervisionado
> **Objetivo:** orientar o Codex na construção incremental, verificável e rastreável de uma POC local de exportação assíncrona de comprovantes salariais e benefícios.

## 0. Instrução para o agente Codex

Você é um engenheiro de software sênior responsável pela implementação desta POC.

1. Leia integralmente `PLAYBOOK-CODEX.md`, `docs/constitution.md` e `docs/spec-001.md` antes de propor alterações.
2. **Não inicie a implementação só porque o Playbook foi fornecido.** Aguarde instrução explícita para executar a etapa `P-00`.
3. Trabalhe em **baby steps**. Execute **uma etapa por vez** e aguarde autorização humana para avançar. Não presuma aprovação a partir de ausência de resposta.
4. Em cada etapa: inspecione estado Git → planeje mudança mínima → implemente → execute verificações → registre evidências → faça **um único commit da etapa** → reporte SHA → pare e aguarde aprovação.
5. Antes de qualquer commit, todos os critérios de aceite e testes aplicáveis à etapa devem passar. Se falharem, não commite; registre o impedimento e solicite decisão. Não faça commit com testes desabilitados para ocultar erros.
6. Não apague dados, não sobrescreva trabalho de terceiros e não execute comandos destrutivos (`git reset --hard`, exclusão indiscriminada, limpeza do banco ou Azurite) sem autorização específica.
7. Evite alterações fora do escopo da etapa; mudanças de contrato, regras de negócio e arquitetura exigem atualização do SDD e justificativa em ADR quando cabível.
8. Não instalar software do sistema operacional sem apresentar necessidade, alternativas e obter autorização. Não usar Docker, WSL, serviços Azure reais ou serviços externos como atalhos.
9. Nunca invente saída de testes, logs, commits ou cobertura. Se o ambiente não estiver disponível, registre **BLOCKED**, com comando exato de validação pendente.
10. Ao encerrar cada passo, apresente: objetivo, arquivos alterados, testes e resultados, riscos, evidência, commit SHA, próxima etapa — sem executá-la.

### Modo de supervisão

`SUPERVISIONADO` (padrão). Somente o humano pode autorizar a próxima etapa. A execução automática contínua **não está autorizada** por este documento.

### Hierarquia da verdade

1. Decisões explicitamente aprovadas pelo usuário na conversa e registradas na documentação versionada.
2. `docs/constitution.md` e `docs/spec-001.md` atualizados e aprovados.
3. ADRs aceitos.
4. OpenAPI e contrato de evento aprovados.
5. Este Playbook e tarefas.
6. Código implementado e testes.

Em caso de divergência, **pare e sinalize**. A decisão assíncrona definida na seção 2 deve ser reconciliada com a SPEC antes de qualquer funcionalidade.

## 1. Objetivo de negócio e demonstração final

Construir um sistema de demonstração em que o usuário:

1. Acessa o Angular, consulta empregados fictícios com comprovantes de salário (`SALARY`) e/ou benefício (`BENEFIT`) em determinado período.
2. Seleciona um ou mais empregados e solicita a exportação.
3. Angular → BFF → Request API → Kafka. A Request API espera **apenas o acknowledgment de publicação do broker** e devolve `HTTP 202 Accepted`, com `requestId` e `Location`.
4. O usuário **pode fechar imediatamente o navegador**. Não há dependência de sessão ativa, SSE, WebSocket, polling obrigatório ou notificação.
5. Kafka Consumer → grava solicitação e deduplica → dispara Spring Batch → lê comprovantes no PostgreSQL → chama PDF Motor para **cada pagamento** → grava PDF no Azurite → grava referência e status no PostgreSQL.
6. Horas ou dias depois, o usuário reabre a aplicação, consulta solicitações/resultados por filtros, visualiza a situação e baixa PDFs concluídos. **Não precisa preservar o `requestId`**, embora possa buscá-lo diretamente.
7. Se nada estiver pronto, a UI apresenta mensagem clara: “Sua solicitação ainda está sendo processada. Consulte novamente mais tarde.” Se não há solicitação ou comprovante correspondente, informa “Nenhum resultado encontrado para os filtros informados.”

**Não objetivo:** transformar o `202` em processamento síncrono ou manter a tela observando automaticamente o progresso.

## 2. Decisão de arquitetura obrigatória — DEC-11

**DEC-11: Fire-and-forget do ponto de vista da UI, com confirmação de publicação no Kafka.**

- O retorno `202` só acontece após **ack de publicação** do Kafka. Se não houver ack, responder erro apropriado (`503`/`502`) e não declarar aceite.
- `202` não significa solicitação já persistida ou PDFs prontos; a persistência da solicitação ocorre no **worker**, após o consumo.
- A UI **não faz polling automático** após o POST. Um clique explícito em “Consultar” ou o acesso a uma tela de consulta pode disparar uma chamada única.
- O usuário reabre a tela no futuro e filtra histórico pelo empregado, período, tipo e/ou `requestId`, sem depender de armazenamento no navegador.
- A Query API precisa oferecer **listagem pesquisável de solicitações** (novo endpoint a incluir em SPEC/OpenAPI): `GET /api/v1/exports` com filtros e paginação.
- Até a ingestão Kafka, uma solicitação recém-aceita pode estar ausente da Query API. Não mascarar esse comportamento; exibir uma mensagem neutra, sem loops automáticos.
- Não implementar Outbox no MVP; o risco de aceite sem persistência no banco fica reduzido pela confirmação do broker e persistência Kafka configurada, não eliminado. Documentar risco em ADR.

**Tarefa obrigatória P-00:** corrigir o trecho da `docs/spec-001.md` que prevê repetição de consultas por polling, adicionar o requisito de busca histórica sem `requestId` e refletir DEC-11 na Constitution, sem mudar outras escolhas por conta própria.

## 3. Stack e limites

| Área | Tecnologia / diretriz |
|---|---|
| Backend | Java 21, Spring Boot 4.1.x, Maven multi-módulo |
| Processamento | Spring Batch 6.0.x, `JobRepository` persistente em PostgreSQL; `chunk(1)` para começar |
| Mensageria | Apache Kafka 4.3.1, broker local KRaft e grupo de consumidores |
| Persistência | PostgreSQL nativo Windows, Flyway, JDBC/JPA somente onde fizer sentido |
| Storage | Azurite Blob Storage via npm, Azure Storage Blob SDK; arquivo apenas no storage |
| PDF | Motor Spring Boot independente com Apache PDFBox ou biblioteca equivalente validada |
| UI | Angular 22.x standalone, TypeScript, consumo exclusivo do BFF |
| Contratos | OpenAPI 3.x, evento Kafka versionado JSON; Postman Collection + Environment; cURLs |
| Segurança POC | Somente dados fictícios; não incluir CPF real, dados bancários ou credenciais no Git |
| Infra | Windows 11, PowerShell, sem Docker, sem Azure, sem WSL obrigatório |

**Versões:** validar compatibilidade no preflight e fixar versões exatas em arquivos gerenciados (`pom.xml`, `package-lock.json`). Não atualizar major versions por iniciativa própria. A matriz Angular 22 pede Node.js `^22.22.3 || ^24.15.0 || ^26.0.0`.

Referências: https://angular.dev/reference/versions ; https://docs.spring.io/spring-batch/reference/ ; https://kafka.apache.org/quickstart/ ; https://learn.microsoft.com/en-us/azure/storage/common/storage-install-azurite

## 4. Arquitetura-alvo

```text
┌─────────────────────────────────────────────────────────┐
│ Angular web-angular :4200                               │
│  - lista empregados / seleciona filtros                 │
│  - POST solicitação; recebe 202; pode fechar            │
│  - abre depois; consulta histórico e PDFs               │
└─────────────────────────┬───────────────────────────────┘
                          │ HTTP (única entrada da UI)
                          v
                  BFF bff-api :8080
                       │       │
            POST/GET    │       │ GET
                       v       v
             request-api :8081     query-api :8082
                     │                  │     │
                   Kafka                │     └─ Azurite :10000 (download)
             payroll.exports           PostgreSQL :5432
               .requested.v1            │
                     │                  │
                     v                  │
            batch-worker :8083 ─────────┘
                │  └─ Spring Batch com JobRepository SQL
                │
                ├──(HTTP)--> pdf-motor-api :8084 -> PDF bytes
                └──(SDK)---> Azurite Blob :10000
                          └── PostgreSQL: container + blob_name + status
```

### Fronteiras dos seis projetos

- `sistemas/apps/web-angular`: componentes, formulários, feedback, listagem e download, sem acesso direto a outros backends.
- `sistemas/apps/bff-api`: rotas públicas, validação de entrada/erro HTTP e encaminhamento para Request e Query; **não** acessa Kafka, banco ou motor PDF.
- `sistemas/apps/request-api`: valida solicitação, publica uma mensagem Kafka com chave `requestId`, espera ack e retorna 202; **não** grava na base nem executa Batch.
- `sistemas/apps/query-api`: SQL read-only para dados de negócio e metadados, lista histórico, mostra status e fornece download via SDK Azurite; pode detectar arquivo ausente e retornar erro explícito.
- `sistemas/apps/batch-worker`: `@KafkaListener` → ingestão SQL idempotente → `JobLauncher`/recuperação; Spring Batch processa comprovantes e persiste status/paths.
- `sistemas/apps/pdf-motor-api`: recebe dados de **um** pagamento por chamada e retorna `application/pdf`, sem acesso à base, sem gravar blob.

## 5. Organização do monorepo

```text
payroll-export-poc/
├── README.md
├── .gitignore
├── sdd/
│   ├── PLAYBOOK-CODEX.md
│   ├── README.md
│   ├── docs/
│   │   ├── constitution.md
│   │   ├── spec-001.md
│   │   ├── domain.md
│   │   ├── architecture.md
│   │   ├── api-contract.md
│   │   ├── plan.md
│   │   └── tasks.md
│   ├── adr/
│   └── progress/
└── sistemas/
    ├── pom.xml                    # agregador Maven dos projetos Java
    ├── apps/
    │   ├── web-angular/           # Angular + package-lock.json
    │   ├── bff-api/               # app Spring Boot independente
    │   ├── request-api/
    │   ├── query-api/
    │   ├── batch-worker/
    │   └── pdf-motor-api/
    ├── packages/contracts/        # OpenAPI, evento Kafka e schemas JSON
    ├── database/
    │   ├── migrations/
    │   └── seed/
    ├── infra/
    │   ├── kafka/
    │   ├── postgres/
    │   └── azurite/
    ├── scripts/
    │   ├── check-prereqs.ps1
    │   ├── start-infra.ps1
    │   ├── start-apps.ps1
    │   ├── stop-local.ps1
    │   ├── smoke.ps1
    │   └── e2e.ps1
    └── postman/
        ├── payroll-export-poc.postman_collection.json
        └── local.postman_environment.json
```

Todo serviço deve possuir build, configuração, teste e healthcheck próprios. `sistemas/packages/contracts` é fonte contratual executável; não criar biblioteca compartilhada com entidades JPA para acoplar os serviços.

## 6. Contratos mínimos a implementar

### 6.1 REST externo: apenas BFF `http://localhost:8080`

| ID | Verbo | Endpoint | Resposta/propósito |
|---|---|---|---|
| API-01 | GET | `/api/v1/employees?startPeriod=2026-01&endPeriod=2026-06&paymentTypes=SALARY,BENEFIT` | 200: empregados elegíveis |
| API-02 | GET | `/api/v1/employees/{employeeId}/payments` | 200: pagamentos do empregado, com filtros |
| API-03 | POST | `/api/v1/exports` | 202: `requestId`, `status=ACCEPTED`, `statusUrl`, `Location` |
| **API-04A** | **GET** | **`/api/v1/exports?employeeId=101&startPeriod=2026-01&endPeriod=2026-06&page=0&size=20`** | **200: histórico filtrável; novo contrato DEC-11** |
| API-04B | GET | `/api/v1/exports/{requestId}` | 200: status, contadores; 404 se ainda não ingerido/inexistente |
| API-05 | GET | `/api/v1/exports/{requestId}/documents` | 200: documentos, inclusive parciais |
| API-06 | GET | `/api/v1/documents/{documentId}/download` | 200: stream `application/pdf` |

A API-04A pode acrescentar filtros opcionais `requestId`, `status`, `paymentTypes`, `employeeIds`, além da paginação, conforme contrato aprovado em P-02. Ordenar de forma determinística (novos primeiro, desempate por ID).

#### POST de referência

```json
{
  "employeeIds": [101, 102],
  "startPeriod": "2026-01",
  "endPeriod": "2026-06",
  "paymentTypes": ["SALARY", "BENEFIT"]
}
```

#### HTTP 202

```json
{
  "requestId": "dcd4480e-9f94-4c2e-8be0-4511c34c4018",
  "status": "ACCEPTED",
  "statusUrl": "/api/v1/exports/dcd4480e-9f94-4c2e-8be0-4511c34c4018"
}
```

**Sem job interno no contrato público.** O UUID `requestId` identifica a solicitação de domínio. `JobInstance`/`JobExecution` são identificadores internos do Spring Batch.

### 6.2 Evento Kafka

Tópico `payroll.exports.requested.v1`; key `requestId`; group `payroll-batch-worker`; entrega assumida **at least once**.

```json
{
  "eventVersion": 1,
  "requestId": "dcd4480e-9f94-4c2e-8be0-4511c34c4018",
  "createdAt": "2026-10-23T17:00:00Z",
  "employeeIds": [101, 102],
  "startPeriod": "2026-01",
  "endPeriod": "2026-06",
  "paymentTypes": ["SALARY", "BENEFIT"]
}
```

O consumer confirma offset **apenas após commit da ingestão SQL**. Falha entre persistir e lançar Batch deve ser reparada por varredura local de pendências; redelivery não pode criar duplicatas. Uma chamada assíncrona a `JobLauncher` não equivale a concluir o job.

### 6.3 API privada do motor PDF

`POST http://localhost:8084/internal/v1/pdf/render` recebe JSON tipado com nome fictício, matrícula, competência, tipo, proventos, descontos e líquido de **um pagamento**; responde `200 application/pdf`. O PDF precisa conter dados legíveis e corretos, não um placeholder vazio. Testar cabeçalho `%PDF-` e extração de campos-chave.

## 7. Modelo de dados e invariantes

Tabelas mínimas:

| Tabela | Papel | Restrições indispensáveis |
|---|---|---|
| `empregado` | Cadastro fictício | `id`, matrícula única |
| `comprovante_pagamento` | Pagamento individual | FK empregado, competência, tipo (`SALARY`, `BENEFIT`), valores `NUMERIC(15,2)` |
| `solicitacao` | Histórico durável | `id UUID`, status, filtros, timestamps, contadores |
| `solicitacao_empregado` | Seleção | PK `(solicitacao_id, empregado_id)` |
| `documento_gerado` | Um PDF lógico por pagamento/requisição | UNIQUE `(solicitacao_id, comprovante_id)`, status, `container_name`, `blob_name`, `checksum_sha256` |
| `BATCH_*` | Controle oficial de execução | Usar schema compatível com Spring Batch 6, validado |

Estados da solicitação: `PENDING`, `PROCESSING`, `COMPLETED`, `PARTIAL`, `FAILED`. Documentos: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`. Ausência de linha imediatamente após `202` significa possível janela de ingestão e **não deve ser convertida em status persistido fictício**.

Invariantes: um pagamento → um PDF por solicitação; sem PDFs fora dos filtros; reentrega e restart não criam outro documento lógico; JSON event versionado; `BigDecimal` para valores; `blob_name` estável sem dados pessoais (ex.: `exports/{requestId}/{paymentId}.pdf`); PDF **nunca** vai para coluna bytea/LOB do PostgreSQL.

### 7.1 Consistência PostgreSQL ↔ Azurite

**Proibido afirmar atomicidade ACID distribuída entre PostgreSQL, HTTP e Azurite.**

1. Registrar item como pendente no PostgreSQL.
2. Buscar os dados do pagamento e chamar PDF Motor com timeout.
3. Upload para Azurite com chave determinística, sem gerar nomes aleatórios por tentativa.
4. Em transação **apenas PostgreSQL**, gravar metadados e status `COMPLETED`.
5. Se upload teve sucesso e SQL falhou: no retry verificar/usar o mesmo blob; reconciliar órfãos; nunca expor item como concluído sem registro SQL confirmado.
6. Se o banco diz concluído mas o blob está ausente: download não responde PDF inexistente; registrar condição inconsistente para diagnóstico.
7. Testar falhas antes/depois do upload, antes/depois do commit e após restart. Sem retry ilimitado; definir política finita e registrar causa.

O `chunk(1)` define a granularidade inicial das transações Batch, **não** atomicidade com storage. Controlar efeitos externos, timeouts e evitar manter transação SQL aberta por muito tempo; qualquer simplificação deliberada da POC deve constar do ADR.

## 8. Regras de desenvolvimento e qualidade

- **Sem mocks no happy path E2E**: usar Kafka, PostgreSQL, PDF Motor e Azurite reais, todos locais. Unitários podem usar mocks.
- **Sem dependência de frontend ativo**: reiniciar Angular após o processamento e recuperar arquivos pela Query API.
- Evitar dependências desnecessárias, integrações SaaS e engenharia excessiva (Kubernetes, autenticação, Redis, monitoramento externo, ZIP, DLT/Outbox no MVP).
- Testar lógica de negócio com unitários, fluxos HTTP com integração, persistência com PostgreSQL local separado para testes quando possível e E2E com infraestrutura real. Não exigir Testcontainers/Docker.
- `spring.batch.job.enabled=false` no worker para impedir execução automática não desejada na inicialização; jobs lançados de maneira controlada.
- Migrações idempotentes via Flyway; seed reexecutável. Não usar `hibernate.ddl-auto=create` para recriar banco em startup.
- Portas e credenciais configuráveis por variáveis de ambiente, valores locais não sensíveis no `.env.example` ou README.
- Healthchecks mínimos por serviço; logs estruturados/correlacionáveis por `requestId`, `paymentId` e `JobExecutionId`.
- `requestId` é emitido mesmo se usuário fechar a UI; nunca vincular solicitação à sessão HTTP.
- Browser não deve chamar serviços 8081/8082/8084 nem Kafka diretamente.

## 9. Plano de implementação — passos com aceites e commits

> A sequência abaixo define **P-00 a P-20**. Cada passo resulta em um único commit após validação e registro de evidências. Passos somente documentais usam lint/link/verificações estruturais; passos funcionais exigem testes automatizados ou scripts verificáveis.

| Passo | Escopo de implementação | Evidência/critério obrigatório | Commit sugerido |
|---|---|---|---|
| **P-00** | Reconciliar `constitution.md` e `spec-001.md` com DEC-11; remover polling; documentar consulta futura por filtros. Manter estado PROPOSTO até aprovação humana. | Diff de requisitos/decisões, sem contradições; aprovação explícita dos docs antes de P-01 | `docs(sdd): align async offline consultation` |
| **P-01** | Preflight Windows: detectar Java, Maven, Node, npm, Angular CLI, PostgreSQL, Kafka KRaft, Azurite, portas; smoke de subida nativa, sem Docker. | `sistemas/scripts/check-prereqs.ps1` e relatório real `sdd/progress/P-01.md` com OK/BLOCKED e comandos | `chore(infra): validate local prerequisites` |
| **P-02** | DOMAIN-001 + ARCH-001 + ADRs (consistência, 202, no polling); OpenAPI + evento JSON schema; `sdd/docs/plan.md`, `sdd/docs/tasks.md`, decisões explícitas. | Contracts e rastreabilidade revisados/aprovados; nenhum endpoint fora do BFF | `docs(sdd): specify domain architecture and contracts` |
| **P-03** | Estrutura monorepo, Maven parent e cinco módulos Spring Boot + app Angular mínimo; wrappers; configs por serviço. | `mvnw.cmd -q -DskipTests package` e `npm run build` passam; apps sobem individualmente | `build(repo): scaffold six independent apps` |
| **P-04** | Schema PostgreSQL, Flyway, tabelas Batch oficiais, constraints e seed de 5 empregados/20 pagamentos. | SQL conta dados, unicidade, migrations repetíveis; PostgreSQL reinicia sem perda | `feat(database): add payroll schema and fixtures` |
| **P-05** | Query API: empregados elegíveis e pagamentos por período/tipo. | Integração SQL com 0, 1 e N pagamentos; filtros inclusivos e valores corretos | `feat(query): list employees and payments` |
| **P-06** | PDF Motor: recebe pagamento e responde um PDF válido em memória. | Teste PDF `%PDF-`, `Content-Type` e conteúdo textual extraído | `feat(pdf): render payroll payment receipts` |
| **P-07** | Adaptador Azurite no worker: container, upload/download/exists, key determinística. | Smoke grava e lê PDF no Azurite sem conta Azure | `feat(storage): integrate local azurite` |
| **P-08** | Request API: validar POST, produzir evento JSON versionado, esperar ack e retornar HTTP 202/Location. | Testes de falha de publicação (não retorna 202) e evento Kafka real | `feat(request): publish export requests to kafka` |
| **P-09** | Worker consumer: ingestão SQL idempotente, ack de offset depois do commit, registro de pendências. | Reenviar mesmo evento duas vezes → 1 solicitação, N vínculos únicos | `feat(worker): ingest kafka requests idempotently` |
| **P-10** | `JobRepository` JDBC + JobLauncher + job com parâmetros estáveis + recuperação de job não iniciado. | Executar job, reiniciar processo e inspecionar `BATCH_*`; não lançar em duplicidade | `feat(batch): bootstrap restartable export job` |
| **P-11** | `prepareRequestStep`: materialização/reconciliação dos comprovantes e `documento_gerado` pendentes. | Sem pagamentos → `COMPLETED` com 0; com N → N itens sem duplicação | `feat(batch): prepare export items` |
| **P-12** | `generateDocumentsStep`: reader JDBC, processor HTTP PDF, writer Azurite+SQL, `chunk(1)`. | N pagamentos → N PDFs reais, paths e checksum; SQL contém somente referências | `feat(batch): generate and store pdf documents` |
| **P-13** | Retry finito, tratamento de item com erro, contadores `PARTIAL`/`FAILED`, idempotência e reinício. | Falha injetada motor/Azurite/SQL; compensação/reconciliação; sem documento duplicado | `feat(batch): recover from partial failures` |
| **P-14** | Query API: histórico filtrável/paginado, status, documentos, download por stream Azurite. | Consulta por empregado/período após nova sessão; PDF válido; sem dados → mensagem contratual | `feat(query): expose export history and downloads` |
| **P-15** | BFF: todas as rotas públicas, validação de erros, HTTP 202 preservado e headers. | Testes de contrato: UI precisa apenas de porta 8080 | `feat(bff): expose unified payroll api` |
| **P-16** | Angular: lista empregados, filtros, seleção, POST e 202, estado de aceitação; sem polling. | Teste componente comprova chamada única após click; fechar/reabrir sem perda backend | `feat(web): submit export requests` |
| **P-17** | Angular: consulta histórica com filtros, status, vazio, parcial, download de PDFs; busca manual. | Abre em nova sessão, acha solicitação sem requestId, baixa um PDF | `feat(web): browse completed payroll exports` |
| **P-18** | Postman Collection/environment, tabela de cURLs, scripts start/stop e smoke local. | Newman/cURLs com variáveis corretas; start/stop documentados, não destrutivos | `test(api): provide reproducible local collection` |
| **P-19** | E2E real e cenários de falha: navegador fechado, broker desconectado, redelivery, falha SQL pós-upload, restart. | Evidências de AC-01 a AC-11, diagnósticos e commit dos scripts de teste | `test(e2e): validate async export recovery` |
| **P-20** | README final, diagrama, matriz RF→AC→testes→commits, relatório de execução e limitações. | Tudo rodando local; inventário de portas; lista de pendências; relatório com SHAs | `docs(release): close poc verification` |

**Observação:** executar P-00 sem aprovação documental pode **apenas preparar alterações propostas**; não marcar Constituição/SPEC como APROVADAS de forma autônoma. O marco de aprovação é uma ação do usuário.

## 10. Critérios de aceite E2E

| ID | Cenário obrigatório | Resultado verificável |
|---|---|---|
| AC-01 | Seleção por competência/tipo | Exibe apenas empregados com pagamentos elegíveis |
| AC-02 | POST assíncrono | Kafka recebe evento e HTTP 202 sem esperar PDF |
| AC-03 | Fechar imediatamente Angular | Worker conclui processamento sem sessão ativa |
| AC-04 | Voltar dias depois | Query API e Angular acham histórico por filtros sem `requestId` |
| AC-05 | PDF por pagamento | Azurite tem N PDFs válidos e banco N referências |
| AC-06 | Nenhum comprovante elegível | Solicitação `COMPLETED`, zero documentos, mensagem clara |
| AC-07 | Kafka fora do ar | Request API não responde 202 falsamente |
| AC-08 | Kafka redelivery | Sem duplicação lógica de solicitação, vínculo ou PDF |
| AC-09 | SQL falha após upload | Documento não figura como completo; retry/reconciliação recupera |
| AC-10 | Motor PDF falha pontualmente | Retry finito; solicitação `PARTIAL` ou `FAILED` coerente |
| AC-11 | Restart worker | Retoma pendências e preserva itens já concluídos |
| AC-12 | Download | Retorna stream PDF do Azurite (sem `bytea` no banco) |
| AC-13 | Windows sem Docker/nuvem | Scripts e aplicações executam sem containers nem credenciais reais |

A automação **de testes** pode consultar com tentativas limitadas para aguardar processamento e verificar estados; isto não altera a regra de **não fazer polling na UI**.

## 11. Rastreabilidade obrigatória

Para cada passo `P-xx`, criar `sdd/progress/P-xx.md` com:

```md
# P-xx — <título>
- Estado: DONE | BLOCKED
- Data:
- Objetivo:
- Requisitos: RF-xx, RN-xx, RNF-xx, DEC-xx
- Critérios de aceite: AC-xx
- Arquivos criados/alterados:
- Comandos efetivamente executados:
- Resultado real dos testes (quantidade, sucesso, falha):
- Evidência objetiva (logs resumidos, consulta SQL, saída de teste):
- Limitações/risco residual:
- Commit SHA completo:
- Próximo passo (NÃO iniciado):
```

Nota prática: incluir `sdd/progress/P-xx.md` **no mesmo commit da etapa**. O SHA final não é conhecido até criar o commit; registrar o SHA no relatório final ao usuário e em um **índice de execução** de forma que não exija um segundo commit para cada passo (ex.: índice atualizado na etapa seguinte ou release final). Não criar hash fictício nem alterar retroativamente arquivo versionado sem commit.

Comandos base (adaptar à estrutura concretamente implementada):

```powershell
# Build Java
cd sistemas
.\mvnw.cmd -B clean verify

# Build da interface
cd apps\web-angular
npm ci
npm run build
npm test -- --watch=false

# Verificações locais
cd ..\..
.\scripts\check-prereqs.ps1
.\scripts\smoke.ps1
.\scripts\e2e.ps1

# Retornar à raiz do repositório
cd ..

# Git: somente após critérios verdes
git status --short
git diff --check
git add <arquivos-da-etapa>
git commit -m "<mensagem-da-etapa>"
git rev-parse HEAD
```

`git add <arquivos-da-etapa>` é **placeholder de instrução**, não comando executável até substituir por caminhos reais. Nunca `git add .` sem inspecionar o diff/estado.

## 12. Infra e execução sem Docker

| Serviço | Porta de referência | Forma local |
|---|---:|---|
| Angular | 4200 | `npm start` / Angular CLI |
| BFF | 8080 | Maven/Java |
| Request API | 8081 | Maven/Java |
| Query API | 8082 | Maven/Java |
| Worker | 8083 | Maven/Java |
| Motor PDF | 8084 | Maven/Java |
| Kafka KRaft | 9092 | distribuição binária, scripts nativos quando compatíveis |
| PostgreSQL | 5432 | instalação local Windows |
| Azurite Blob | 10000 | `azurite-blob --location <dir-local>` |

**Gate crítico P-01:** verificar se a distribuição Kafka selecionada opera de fato no Windows do desenvolvedor; capturar saída dos scripts `bin\windows\kafka-storage.bat` e `kafka-server-start.bat`, testar `kafka-topics.bat`. Não inferir sucesso por presença de arquivos. Se houver incompatibilidade, **parar** e pedir decisão de alternativa sem Docker; não trocar tecnologia unilateralmente.

### Ordem de inicialização sugerida

PostgreSQL → Kafka → Azurite → PDF Motor → Batch Worker → Request API → Query API → BFF → Angular. Scripts devem preservar logs, informar PID/porta, detectar serviço já ativo e parar **somente processos iniciados por eles**, sem apagar diretórios de dados.

## 13. Lista de cURLs a entregar

O README/Postman final deve ter exemplo reproduzível para todos:

```bash
# Empregados elegíveis
curl "http://localhost:8080/api/v1/employees?startPeriod=2026-01&endPeriod=2026-06&paymentTypes=SALARY,BENEFIT"

# Pagamentos de um empregado
curl "http://localhost:8080/api/v1/employees/101/payments?startPeriod=2026-01&endPeriod=2026-06"

# Solicitar exportação (deve retornar 202)
curl -i -X POST "http://localhost:8080/api/v1/exports" -H "Content-Type: application/json" -d '{"employeeIds":[101,102],"startPeriod":"2026-01","endPeriod":"2026-06","paymentTypes":["SALARY","BENEFIT"]}'

# Histórico: permite consultar dias depois sem guardar o requestId
curl "http://localhost:8080/api/v1/exports?employeeId=101&startPeriod=2026-01&endPeriod=2026-06&page=0&size=20"

# Status específico
curl "http://localhost:8080/api/v1/exports/{requestId}"

# Documentos associados
curl "http://localhost:8080/api/v1/exports/{requestId}/documents"

# Download
curl -L "http://localhost:8080/api/v1/documents/{documentId}/download" -o comprovante.pdf
```

**Nota Windows PowerShell:** onde `curl` for alias, usar `curl.exe` para execução compatível; documentar aspas/escape específicos. A coleção Postman deve salvar `requestId` da resposta de POST e permitir execução por filtro sem essa variável.

## 14. Definição de pronto (Definition of Done)

A POC só está finalizada se **todas** estas condições forem demonstradas:

- Seis aplicações independentes compilam/iniciam; todos os processos reais rodam localmente sem Docker e Azure.
- Dados fictícios estão no PostgreSQL, tópico Kafka e container Azurite existem.
- `POST /exports` retorna `202` após ack e independentemente da conclusão do Batch.
- Spring Batch produz PDFs reais para múltiplos registros, guarda em Azurite e persiste apenas a referência no banco.
- Reabrir a UI e buscar histórico **sem `requestId`** funciona e não precisa da sessão anterior.
- Ausência, processamento pendente, sucesso, parcial e erro têm mensagens diferenciadas.
- Replay, restart, falha de upload, falha SQL pós-upload e indisponibilidade Kafka têm evidência verificável.
- OpenAPI, Kafka event schema, Postman, cURLs, migrations, scripts, README, ADRs, matriz de rastreabilidade e commits por passo estão completos.

## 15. Como iniciar e como prosseguir com o Codex

**Para uma revisão sem criar código:**

> Leia `PLAYBOOK-CODEX.md`, `docs/constitution.md` e `docs/spec-001.md`. Faça uma análise de consistência, liste riscos e apresente o plano de P-00. Não altere arquivos, não execute etapas e não commite.

**Para iniciar a construção, após aprovação humana:**

> Playbook Codex v1.0 aprovado. Autorize **somente P-00**. Siga o modo SUPERVISIONADO. Atualize os artefatos SDD com DEC-11, apresente diff, validações e evidências. Não inicie P-01 antes de eu aprovar os documentos e autorizar a próxima etapa.

**Para a etapa seguinte:**

> Autorizo P-01. Execute somente esse passo, valide, registre evidências, faça um commit se estiver verde e pare. Aguarde minha autorização para P-02.

---

**Nota de governança:** Playbook, Constituição, SPEC e DEC-11 foram aprovados explicitamente pelo usuário em 2026-10-23 durante P-00. A execução continua supervisionada e cada etapa posterior exige autorização própria.
