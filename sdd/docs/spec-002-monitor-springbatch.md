# SPEC-002 — Monitor Spring Batch

| Campo | Valor |
|---|---|
| Versão | 1.0 |
| Data | 2026-10-23 |
| Estado | IMPLEMENTADO E VERIFICADO |
| Componente | `batch-worker` |
| ADR | [ADR-004](../adr/ADR-004-batch-monitor-observability.md) |

## 1. Objetivo e limites

Adicionar ao `batch-worker` uma feature interna chamada `monitor-springbatch` para observar metadados, performance, vazão e resiliência do processamento. O monitor combina o histórico do `JobRepository` com métricas operacionais Micrometer.

O monitor é somente leitura, usa a porta existente `8083`, não passa pelo BFF e não faz parte da jornada Angular. Iniciar, parar, abandonar ou reiniciar jobs por HTTP fica fora desta versão.

## 2. Fontes de dados

| Fonte | Uso |
|---|---|
| `JobRepository`/metadados `BATCH_*` | instâncias, execuções, parâmetros, steps, contadores, estados e duração histórica |
| Domínio PostgreSQL | correlação com `requestId`, backlog e resultado funcional |
| Micrometer | timers, counters, gauges, taxas, histogramas e percentis operacionais |
| Health contributors | disponibilidade de PostgreSQL, Kafka, PDF Motor e Azurite |

O acesso direto às tabelas `BATCH_*`, quando indispensável, deve ficar isolado em um adapter/repository. Controllers não podem conter SQL.

## 3. Requisitos funcionais

| ID | Requisito | Verificação |
|---|---|---|
| RF-19 | Listar execuções Spring Batch com paginação, ordenação e filtros | Teste de integração |
| RF-20 | Consultar execução, parâmetros identificadores e steps | Teste de contrato |
| RF-21 | Correlacionar `requestId` com `JobInstance`, `JobExecution`, steps e solicitação | Teste de correlação |
| RF-22 | Apresentar duração e contadores por job e step | Comparação com metadados persistidos |
| RF-23 | Apresentar vazão de jobs e documentos por janela UTC | Teste com janela controlada |
| RF-24 | Apresentar sucesso, falha, parcial, skip, retry e rollback | Cenários de falha controlada |
| RF-25 | Medir latência do PDF Motor, Blob e persistência SQL | Timers Micrometer |
| RF-26 | Identificar redelivery Kafka e recuperação de jobs | Contadores e testes de restart |
| RF-27 | Expor métricas por Micrometer e endpoint Prometheus | Scrape automatizado |
| RF-28 | Apresentar backlog e execuções ativas | Teste durante job em andamento |

## 4. Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF-10 | A API do monitor é estritamente somente leitura. |
| RNF-11 | A coleta não altera transações, status ou execução dos jobs. |
| RNF-12 | Listas são paginadas; `size` deve ficar entre 1 e 100. |
| RNF-13 | Intervalos de consulta têm limite máximo configurável e usam UTC. |
| RNF-14 | Agregações respondem em até 2 segundos para a massa da POC. |
| RNF-15 | Métricas e respostas não expõem dados pessoais ou conteúdo de PDFs. |
| RNF-16 | Tags Micrometer não usam `requestId`, `paymentId`, `documentId` nem mensagens de erro. |
| RNF-17 | Falha na instrumentação não interrompe o processamento principal. |
| RNF-18 | Endpoints permanecem internos ao `batch-worker`, sem rota no BFF. |
| RNF-19 | Contadores continuam coerentes após retry, redelivery e restart. |
| RNF-20 | Retenção, histogramas e percentis são configuráveis. |

## 5. Métricas

### Performance

- duração do job, dos steps e ponta a ponta;
- tempo em fila entre criação da solicitação e início do job;
- latência do PDF Motor, upload Blob e confirmação SQL;
- média, mínimo, máximo e percentis p50, p95 e p99;
- execução e step mais lentos na janela.

### Vazão

- jobs iniciados, concluídos e falhos por janela;
- documentos lidos, gravados e falhos;
- documentos por segundo;
- jobs ativos e backlog de solicitações;
- taxas de sucesso, falha e resultado parcial.

### Resiliência

- retries e falhas esgotadas do PDF Motor;
- retries de upload e falhas SQL posteriores ao upload;
- skips e rollbacks por step;
- redeliveries Kafka;
- jobs retomados após startup e tempo de recuperação;
- documentos concluídos preservados durante retomada;
- estado e última observação saudável das dependências.

## 6. Nomes Micrometer

```text
payroll.batch.job.duration
payroll.batch.step.duration
payroll.batch.documents.processed
payroll.batch.documents.failed
payroll.batch.documents.throughput
payroll.batch.pdf.duration
payroll.batch.pdf.retries
payroll.batch.blob.duration
payroll.batch.blob.retries
payroll.batch.sql.duration
payroll.batch.sql.failures_after_upload
payroll.batch.kafka.redeliveries
payroll.batch.jobs.recovered
payroll.batch.backlog
```

Tags permitidas: `job`, `step`, `status`, `dependency`, `operation` e `failure_type`, todas com vocabulário limitado.

## 7. Critérios de aceite

| ID | Cenário | Resultado verificável |
|---|---|---|
| AC-14 | Consultar execuções | Lista paginada, ordenada e filtrável |
| AC-15 | Consultar uma execução | Retorna job, parâmetros, duração, saída e steps |
| AC-16 | Consultar por `requestId` | Retorna a correlação técnica e funcional |
| AC-17 | Consultar performance | Retorna duração, latências e percentis da janela |
| AC-18 | Consultar vazão | Retorna jobs/documentos por tempo, backlog e ativos |
| AC-19 | Falha pontual do PDF Motor | Incrementa retry sem duplicar documento lógico |
| AC-20 | Retry esgotado | Incrementa falha/skip e preserva estado coerente |
| AC-21 | Redelivery Kafka | Incrementa redelivery sem criar outro job lógico |
| AC-22 | Restart do worker | Registra recuperação e preserva itens concluídos |
| AC-23 | Falha SQL pós-upload | Registra falha e posterior recuperação no mesmo blob |
| AC-24 | Execução inexistente ou filtro inválido | Retorna `404` ou `400`, respectivamente |
| AC-25 | Cardinalidade | Nenhum identificador individual aparece como tag |
| AC-26 | Não interferência | Monitor ativo não altera o resultado do job |
| AC-27 | Prometheus | Scrape contém métricas customizadas válidas |

## 8. Rastreabilidade

| Capacidade | Requisitos | Aceites |
|---|---|---|
| Metadados | RF-19 a RF-22 | AC-14 a AC-16, AC-24 |
| Performance | RF-22, RF-25, RF-27 | AC-17, AC-25 a AC-27 |
| Vazão | RF-23, RF-28 | AC-18, AC-26 |
| Resiliência | RF-24, RF-26 | AC-19 a AC-23, AC-26 |

## 9. Fora do escopo

- comandos HTTP para iniciar, parar, abandonar ou reiniciar jobs;
- dashboards externos e alertas centralizados;
- exposição do monitor pelo BFF ou Angular;
- autenticação administrativa, necessária antes de qualquer exposição fora da máquina local.
