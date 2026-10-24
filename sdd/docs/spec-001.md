# SPEC-001 — Exportação Assíncrona de Comprovantes

| Campo | Valor |
|---|---|
| Versão | 0.1 |
| Data | 2026-10-23 |
| Estado | APROVADO em 2026-10-23 |
| Constituição | [CONSTITUTION-001](constitution.md) |
| Nome do sistema | Payroll Export POC |

## 1. Problema e resultado esperado

Uma pessoa precisa listar empregados com pagamentos de salário/benefícios, escolher um subconjunto, solicitar a emissão de seus comprovantes como PDFs e consultar/baixar os resultados sem bloquear a interface. O sistema emite uma solicitação HTTP 202 e executa o processamento em segundo plano.

A POC deverá ser inteiramente local e usar dados fictícios. A geração é **um PDF por registro de pagamento** encontrado pelos filtros; um empregado pode produzir múltiplos PDFs.

## 2. Atores e jornadas

**Ator:** usuário da tela Angular (sem login na versão inicial).

**J-01 — Consulta:** filtra período/tipo, vê apenas empregados com comprovantes encontrados e pode expandir os pagamentos.

**J-02 — Solicitação:** seleciona empregados, solicita emissão e recebe identificador e link para acompanhar.

**J-03 — Processamento:** worker consome Kafka, persiste solicitação, abre job Batch, consulta os pagamentos, solicita PDF por item, sobe no Azurite e registra referência.

**J-04 — Acompanhamento:** em uma consulta manual, inclusive após fechar e reabrir a aplicação, o usuário pesquisa o histórico por filtros sem precisar preservar o `requestId`, vê contagens/status e baixa os arquivos confirmados.

**J-05 — Recuperação:** uma falha temporária não cria novo documento lógico; retry/restart continua itens pendentes ou falhos.

## 3. Requisitos funcionais

| ID | Requisito | Verificação |
|---|---|---|
| RF-01 | Listar empregados que possuam >=1 pagamento correspondente ao período/tipo | GET filtrado |
| RF-02 | Detalhar pagamentos de um empregado com mês, tipo, valor e descrição | GET de pagamentos |
| RF-03 | Permitir seleção múltipla de empregados, meses inclusive e tipos | Validação da UI |
| RF-04 | BFF expor uma API única ao Angular | Chamadas somente ao BFF |
| RF-05 | Request API publicar mensagem no tópico Kafka e retornar HTTP 202 após confirmação do broker | Teste de producer |
| RF-06 | Evento conter `eventVersion`, `requestId`, `createdAt`, filtros e empregados | Validação do contrato |
| RF-07 | Worker consumir evento, deduplicar e registrar solicitação em MySQL | Teste de redelivery |
| RF-08 | Worker iniciar `Job` Spring Batch com parâmetros estáveis e persistir metadados | Inspeção JobRepository |
| RF-09 | Reader consultar registros de pagamentos correspondentes aos filtros | Teste SQL/Batch |
| RF-10 | Processor chamar PDF Motor **uma vez logicamente por comprovante**, com retry físico permitido | Teste de integração |
| RF-11 | PDF Motor receber dados e devolver `application/pdf` em HTTP | Teste do contrato |
| RF-12 | Worker gravar PDF em Azurite com chave determinística | Verificação container/blob |
| RF-13 | Writer atualizar atomica e localmente metadados SQL do documento, sem coluna binária | Teste de rollback SQL |
| RF-14 | Query API listar solicitações por filtros e paginação, consultar uma solicitação por `requestId` e listar seus documentos | GET de histórico/status/documentos |
| RF-15 | Query API fornecer download de PDFs confirmados por stream do Azurite | Teste HTTP PDF |
| RF-16 | UI mostrar estados `aceito`, `processando`, `concluído`, `parcial`, `falha` e permitir consulta manual sem polling automático | Teste de jornada |
| RF-17 | Permitir recuperação de jobs não lançados, falhos ou interrompidos | Testes de restart |
| RF-18 | Entregar cURLs, Postman Collection e environment local | Teste Newman |

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN-01 | Apenas pagamentos de empregados selecionados podem ser exportados. |
| RN-02 | `startPeriod <= endPeriod`; intervalo de meses inclusivo. |
| RN-03 | `paymentTypes` aceita `SALARY` e/ou `BENEFIT` (pelo menos um). |
| RN-04 | Uma requisição deve selecionar pelo menos um empregado. |
| RN-05 | Cada registro de pagamento corresponde a no máximo um documento lógico por `requestId`. |
| RN-06 | Se consulta não recuperar comprovantes, solicitação termina `COMPLETED` com `totalDocuments=0`, mensagem explicativa. |
| RN-07 | `COMPLETED` só quando todos os itens forem concluídos, inclusive o caso zero documentos. |
| RN-08 | `PARTIAL` quando existe ao menos um documento concluído e ao menos um que esgotou as tentativas. |
| RN-09 | `FAILED` quando a solicitação não puder ser processada ou todos os itens terminarem em falha. |
| RN-10 | `blob_name` é a referência lógica estável e não deve conter dados pessoais identificáveis. |
| RN-11 | Redelivery Kafka e restart do Job não criam linhas lógicas duplicadas. |
| RN-12 | A tela não presume que `202` implique PDF pronto ou solicitação já visível na Query API. |
| RN-13 | O histórico deve ser pesquisável sem depender do `requestId` ou de dados preservados na sessão anterior do navegador. |

## 5. Requisitos não funcionais

| ID | Requisito / meta do MVP |
|---|---|
| RNF-01 | Execução nativa Windows, sem Docker, sem conta ou credencial Azure real. |
| RNF-02 | Teste demonstrável com seed mínima de 5 empregados e 20 pagamentos. |
| RNF-03 | Logs permitem correlacionar `requestId`, `paymentId` e o `JobExecutionId` interno quando aplicável. |
| RNF-04 | Clientes HTTP e Kafka usam timeouts configurados e retries limitados. |
| RNF-05 | Persistência e schema por Flyway; schema do Spring Batch em MySQL. |
| RNF-06 | Portas e connection strings configuráveis por variáveis de ambiente. |
| RNF-07 | Não armazenar bytes de PDFs em MySQL. |
| RNF-08 | Serviços têm `/actuator/health`; testes independentes por serviço. |
| RNF-09 | Não fazer chamadas externas de rede para gerar ou armazenar dados da POC. |

## 6. Modelo de domínio (proposto)

| Entidade | Campos de referência | Restrições |
|---|---|---|
| `empregado` | `id`, `matricula`, `nome_ficticio`, `ativo` | matrícula única |
| `comprovante_pagamento` | `id`, `empregado_id`, `competencia`, `tipo`, `descricao`, `valor_bruto`, `descontos`, `valor_liquido`, `data_pagamento` | FK empregado; tipo enum |
| `solicitacao` | `id` (UUID/requestId), `status`, `start_period`, `end_period`, `types`, `created_at`, `started_at`, `completed_at`, `total`, `completed`, `failed` | UUID único |
| `solicitacao_empregado` | `solicitacao_id`, `empregado_id` | PK composta |
| `documento_gerado` | `id`, `solicitacao_id`, `comprovante_id`, `status`, `container_name`, `blob_name`, `checksum_sha256`, `error_message` | UNIQUE (`solicitacao_id`, `comprovante_id`) |
| `BATCH_*` | metadados oficiais do JobRepository | schema definido pelo Spring Batch |

**Dinheiro:** `DECIMAL(15,2)` no MySQL e `BigDecimal` no Java. **Competência:** `YYYY-MM` no contrato REST, persistência com mês/ano normalizados. **Sem CPF ou dado bancário real.**

## 7. Contratos REST públicos via BFF

**Base URL:** `http://localhost:8080` (proposta).

| ID | Método | Endpoint | Status sucesso |
|---|---|---|---|
| API-01 | GET | `/api/v1/employees?startPeriod=2026-01&endPeriod=2026-06&paymentTypes=SALARY,BENEFIT` | 200 |
| API-02 | GET | `/api/v1/employees/{employeeId}/payments?startPeriod=2026-01&endPeriod=2026-06` | 200 |
| API-03 | POST | `/api/v1/exports` | 202 |
| API-04A | GET | `/api/v1/exports?employeeId=101&startPeriod=2026-01&endPeriod=2026-06&page=0&size=20` | 200; histórico filtrável e paginado |
| API-04B | GET | `/api/v1/exports/{requestId}` | 200 / 404 temporário antes da ingestão |
| API-05 | GET | `/api/v1/exports/{requestId}/documents` | 200 |
| API-06 | GET | `/api/v1/documents/{documentId}/download` | 200 `application/pdf` |

**API interna motor PDF**: `POST http://localhost:8084/internal/v1/pdf/render` com JSON de um comprovante; responde `200 application/pdf` e `Content-Disposition: attachment`. Apenas o worker chama.

### POST /api/v1/exports — Request

```json
{
  "employeeIds": [101, 102],
  "startPeriod": "2026-01",
  "endPeriod": "2026-06",
  "paymentTypes": ["SALARY", "BENEFIT"]
}
```

### HTTP 202 — Response

```json
{
  "requestId": "dcd4480e-9f94-4c2e-8be0-4511c34c4018",
  "status": "ACCEPTED",
  "statusUrl": "/api/v1/exports/dcd4480e-9f94-4c2e-8be0-4511c34c4018"
}
```

Headers: `Location: /api/v1/exports/{requestId}` e `X-Correlation-Id`.

### GET /api/v1/exports/{requestId} — Exemplo

```json
{
  "requestId": "dcd4480e-9f94-4c2e-8be0-4511c34c4018",
  "status": "PROCESSING",
  "totalDocuments": 5,
  "completedDocuments": 3,
  "failedDocuments": 0
}
```

### GET /api/v1/exports/{requestId}/documents — Exemplo

```json
{
  "requestId": "dcd4480e-9f94-4c2e-8be0-4511c34c4018",
  "documents": [
    {
      "documentId": "7e919770-d712-4812-814b-abfc9d1c3d66",
      "paymentId": 501,
      "employeeId": 101,
      "status": "COMPLETED",
      "fileName": "comprovante-501.pdf",
      "downloadUrl": "/api/v1/documents/7e919770-d712-4812-814b-abfc9d1c3d66/download"
    }
  ]
}
```

**Erros:** `400` payload inválido, `404` recurso inexistente (ou ainda não ingerido, vide observação abaixo), `409` estado incompatível, `502/503` falha na dependência. Padronizar corpo `application/problem+json`.

**Consulta histórica:** `GET /api/v1/exports` aceita filtros opcionais `requestId`, `employeeId`/`employeeIds`, `startPeriod`, `endPeriod`, `paymentTypes`, `status`, `page` e `size`. O resultado é ordenado dos mais novos para os mais antigos, com desempate determinístico pelo ID.

**Consistência de leitura:** como a API de publicação não grava a solicitação, a Query API pode responder `404` nos instantes após `202` ou retornar uma lista ainda sem a solicitação recém-aceita. A UI não repete consultas automaticamente: informa o aceite e permite que o usuário consulte novamente por ação explícita, na mesma sessão ou em uma sessão futura. Registro antecipado ou Outbox permanece como evolução arquitetural.

## 8. Contrato de evento Kafka

**Tópico:** `payroll.exports.requested.v1`; **key:** `requestId`; **consumer group:** `payroll-batch-worker`.

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

Entrega pelo Kafka será tratada como *at least once*; o worker usa `requestId` para idempotência. Offset só é confirmado após persistência SQL durável. Falhas antes disso permitem redelivery. Interrupção após a persistência e antes do Batch é recuperada pela varredura de solicitações pendentes.

## 9. Processamento Spring Batch

**Nome do job:** `exportPayrollDocumentsJob`.

- `jobParameter`: `requestId` estável; instância lógica de job identificada pelo mesmo ID.
- `Step 1: prepareRequestStep` — carrega e valida solicitação, materializa/reconcilia os itens pendentes no SQL.
- `Step 2: generateDocumentsStep` — Reader JDBC consulta comprovantes ainda não concluídos; Processor monta payload e recebe PDF do `pdf-motor-api`; Writer faz upload para Azurite e confirma os metadados SQL (chunk inicial = 1).
- Ao final, reconciliar contadores e definir `COMPLETED`, `PARTIAL` ou `FAILED`.

**Nota de implementação:** upload é efeito externo; seu sucesso não participa de rollback SQL. Nome de blob determinístico e reconciliação são obrigatórios. Se o desenho técnico mover o upload para `ItemProcessor` ou serviço auxiliar, os mesmos invariantes precisam ser preservados.

**Reinício:** um job já `COMPLETED` não é executado de novo para mesmo requestId. Jobs falhos devem ter caminho explícito de restart usando a mesma instância e recuperando somente itens pendentes/falhos. Critérios de `JobInstance` e parâmetros identificadores serão validados na versão exata do Spring Batch.

## 10. Infraestrutura local e portas propostas

| Processo | Porta | Observação |
|---|---:|---|
| Angular dev server | 4200 | Apenas BFF será chamado pelo navegador |
| BFF | 8080 | API pública |
| Request API | 8081 | Privada para o BFF |
| Query API | 8082 | Privada para o BFF |
| Batch worker actuator | 8083 | Processo de consumer + JobLauncher |
| PDF Motor API | 8084 | `application/pdf` |
| Kafka | 9092 | Broker KRaft local |
| MySQL | 3306 | DB `payroll`, versão 8.4.13 LTS |
| Azurite Blob | 10000 | Container `payroll-pdfs` |

A distribuição Kafka para Windows e o boot Spring Batch 6 deverão passar smoke test antes da primeira feature. Scripts `start-local.ps1` e `stop-local.ps1` previstos como entregáveis.

## 11. Cenários de aceite

| ID | Cenário | Resultado verificável |
|---|---|---|
| AC-01 | Seleção por competência e tipo | Apenas empregados com pagamentos elegíveis são exibidos. |
| AC-02 | POST assíncrono | Kafka recebe o evento e a API retorna `202` após o ack, sem esperar PDFs. |
| AC-03 | Fechar imediatamente o Angular | O worker conclui o processamento sem sessão ativa. |
| AC-04 | Voltar em uma sessão futura | O histórico é localizado por filtros, sem exigir `requestId` ou estado do navegador. |
| AC-05 | PDF por pagamento | Azurite contém N PDFs válidos e MySQL contém N referências, sem binários. |
| AC-06 | Nenhum comprovante elegível | A solicitação termina `COMPLETED`, com zero documentos e mensagem clara. |
| AC-07 | Kafka indisponível | A Request API não responde `202` sem confirmação do broker. |
| AC-08 | Redelivery Kafka | Não há duplicação lógica de solicitação, vínculo ou PDF. |
| AC-09 | Falha SQL após upload | O item não aparece como concluído e retry/reconciliação reutiliza a identidade do blob. |
| AC-10 | Falha pontual do motor PDF | Retry é finito e o estado final fica coerente como `PARTIAL` ou `FAILED`. |
| AC-11 | Reinício do worker | Itens pendentes são retomados e os concluídos não são duplicados. |
| AC-12 | Download | O BFF retorna um PDF válido armazenado no Azurite. |
| AC-13 | Windows sem Docker/nuvem | O sistema e os testes executam localmente sem containers ou credenciais reais. |

## 12. Rastreabilidade inicial

| Jornada | Requisitos | Aceites | Componentes |
|---|---|---|---|
| J-01 Consulta | RF-01, RF-02, RF-03, RF-04 | AC-01 | Angular, BFF, Query API |
| J-02 Solicitar | RF-05, RF-06 | AC-02, AC-07 | BFF, Request API, Kafka |
| J-03 Processar | RF-07 a RF-13 | AC-03, AC-05, AC-06, AC-08, AC-09, AC-10 | Kafka, Batch, Motor, SQL, Azurite |
| J-04 Acompanhar | RF-14 a RF-16 | AC-04, AC-12 | Query API, BFF, Angular |
| J-05 Recuperar | RF-17 | AC-08 a AC-11 | Batch, MySQL, Azurite |
| Infra/API tests | RF-18 | AC-13 | Postman, scripts, infra |

## 13. Entregáveis SDD e ordem proposta

1. Revisar e aprovar CONSTITUTION-001 e SPEC-001.
2. DOMAIN-001: detalhar entidades, regras, diagrama lógico e esquema SQL.
3. ARCH-001 + ADR-001: componentes, sequência, decisões de consistência e bootstrap Windows.
4. API-001: OpenAPI completo e schema do evento Kafka.
5. PLAN-001 + TASKS-001: passos independentes, critérios, commits, testes e evidências.
6. Implementar o monorepo em baby steps, mantendo execução estritamente local.
7. Verificar cURLs, collection Postman, falhas, restart e jornada de ponta a ponta.

**Governança:** documento aprovado em 2026-10-23. A execução permanece supervisionada; cada etapa posterior depende de autorização explícita.

## 14. Pontos de decisão para a revisão

- **P01 (decidido por DEC-11):** manter Kafka `publish + ack -> 202`, aceitar a janela eventual antes da ingestão, não fazer polling automático e oferecer consulta histórica por filtros.
- **P02:** confirmar motor de PDF por registro de comprovante (não um PDF consolidado por empregado).
- **P03:** confirmar que autenticação, ZIP e dados reais estão fora do MVP.
- **P04:** confirmar `chunk(1)` e responsabilidade de recuperação entre Azure Blob emulado e MySQL.
- **P05:** validar Kafka KRaft em Windows nativo antes de construir os serviços.
