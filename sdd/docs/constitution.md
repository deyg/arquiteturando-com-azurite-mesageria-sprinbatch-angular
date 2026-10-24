# CONSTITUTION-001 — Payroll Export POC

| Campo | Valor |
|---|---|
| Versão | 0.1 |
| Data | 2026-10-23 |
| Estado | APROVADO em 2026-10-23 |
| Objetivo | POC local de exportação assíncrona de comprovantes de salário e benefícios |
| Documento relacionado | `spec-001.md` |

## 1. Missão

Demonstrar, de ponta a ponta, uma exportação de comprovantes de pagamento: seleção de empregados no Angular; aceite HTTP 202; evento Kafka; execução Spring Batch; renderização por um motor PDF REST; armazenamento dos binários em Azurite; metadados e referências em MySQL; consulta e download via BFF.

O propósito principal é validar integrações, rastreabilidade, retentativas, reinício de jobs e consistência entre recursos distintos, não construir um sistema de folha de pagamento real.

## 2. Constituição técnica — princípios obrigatórios

| ID | Princípio | Regra verificável |
|---|---|---|
| C-01 | Simplicidade demonstrável | Aplicação executável em notebook Windows, sem Docker nem conta em nuvem. |
| C-02 | Independência de runtime | Seis projetos executáveis separadamente: `web-angular`, `bff-api`, `request-api`, `query-api`, `batch-worker`, `pdf-motor-api`. |
| C-03 | Contrato antes de código | Endpoints REST documentados em OpenAPI; mensagem Kafka versionada. |
| C-04 | Responsabilidade única | BFF agrega, Request API publica, Query API consulta, Batch processa, PDF Motor renderiza, Angular apresenta. |
| C-05 | Backend unificado | Java 21 + Spring Boot 4.1.x + Maven para todos os cinco projetos Java; Spring Batch 6.0.x no worker. |
| C-06 | Dados persistidos corretamente | MySQL guarda entidades e metadados; Azurite guarda o binário; `blob_name`/`container` no banco, nunca caminhos físicos da máquina. |
| C-07 | Operações recuperáveis | IDs estáveis, constraints de unicidade, retry limitado e reconciliação para uploads sem commit SQL. |
| C-08 | Transparência transacional | Não prometer transação ACID entre HTTP, MySQL e Azurite. Transação SQL é local; storage exige idempotência e compensação. |
| C-09 | Batch real e reiniciável | Utilizar `JobRepository` persistido no MySQL, `Job`, `Step`, reader/processor/writer, controle de status e testes de restart. |
| C-10 | Mensageria confiável | Retornar HTTP 202 somente após confirmação de publicação no Kafka; consumo *at least once* com deduplicação pelo `requestId`. |
| C-11 | Observabilidade mínima | `requestId`, `paymentId` e `JobExecutionId` propagados em logs quando aplicáveis e registrados nos seus contextos internos. |
| C-12 | Privacidade da POC | Apenas dados fictícios, sem autenticação real e sem informações bancárias ou pessoais verdadeiras. |
| C-13 | Testabilidade | Unitários, integração MySQL/Azurite/Kafka, contrato REST e cenário end-to-end documentados. |
| C-14 | Desenvolvimento rastreável | Requisito -> critério de aceite -> teste -> tarefa -> commit -> evidência. Um passo implementado = validação + commit. |
| C-15 | Governança de mudanças | Alterações em regra/contrato precisam atualizar documento e testes; decisões arquiteturais relevantes terão ADR. |

## 3. Stack e restrições

- **Java:** 21.
- **Spring Boot:** linha estável 4.1.x, referência inicial 4.1.1.
- **Spring Batch:** linha estável 6.0.x, referência inicial 6.0.6.
- **Kafka:** Apache Kafka 4.3.1 com KRaft e apenas um broker local (ambiente de desenvolvimento).
- **MySQL:** Community Server 8.4.13 LTS, distribuição ZIP portátil Windows x64.
- **Angular:** 22.x, componentes standalone; Node.js conforme matriz oficial do Angular 22 (`^22.22.3 || ^24.15.0 || ^26.0.0`).
- **PDF:** biblioteca open-source Java (preferência inicial por Apache PDFBox); motor HTTP independente.
- **Azurite:** serviço Blob local por npm/VS Code; connection string `UseDevelopmentStorage=true` apenas no ambiente local.
- **Contrato REST:** OpenAPI 3.x; **migrações:** Flyway; **testes de API:** Postman + Newman.
- **Sistema-alvo:** Windows 11, PowerShell; sem dependência obrigatória de Docker/WSL/conta Azure.
- **Verificação obrigatória:** Kafka KRaft em Windows nativo e compatibilidades Maven/dependências devem passar smoke test antes da implementação funcional. Um bloqueio deverá ser documentado, nunca mascarado.

## 4. Domínios e invariantes

1. Um `empregado` possui zero ou mais `comprovante_pagamento`.
2. Um `comprovante_pagamento` pertence a exatamente um empregado e representa um pagamento de **SALARY** ou **BENEFIT**.
3. Apenas empregados com comprovantes correspondentes aos filtros podem aparecer na lista elegível do frontend.
4. Uma solicitação identifica empregados, período (meses inclusive) e tipos de pagamento; processamento assíncrono cria um documento por comprovante selecionado.
5. Não há PDF gerado para um comprovante fora do conjunto filtrado; cada `(request_id, comprovante_id)` só pode originar **um** documento lógico.
6. `documento_gerado` contém `container_name` e `blob_name`, nunca binário PDF nem segredo de acesso.
7. `COMPLETED` exige PDF armazenado e metadados confirmados em SQL. A perda posterior do blob deve ser detectável por consulta/diagnóstico.
8. Mensagens redeliveradas e reinícios não podem causar documentos duplicados.
9. Falha em um comprovante não descarta documentos já confirmados; solicitação pode terminar `PARTIAL`.
10. Dados dos pagamentos são a fonte da verdade; PDF é um artefato derivado e identificável por referência.

## 5. Políticas de execução

### Publicação e confirmação
- `request-api` gera apenas o `requestId` público, publica a mensagem, espera confirmação do broker e retorna `202` com endereço de consulta. Identificadores do Spring Batch são internos ao worker.
- O worker faz *insert idempotente* da requisição e de seus itens em transação SQL e só confirma o offset depois da persistência durável.
- O disparo do Batch ocorre após a persistência. Um processo de recuperação local no worker varre solicitações pendentes para cobrir desligamento entre confirmação Kafka e lançamento do job.
- A resposta 202 **não** significa que o worker já inseriu a solicitação no banco. A UI informa que a solicitação foi aceita e não inicia polling automático. Uma consulta manual posterior pode ainda não encontrar a solicitação durante a janela de ingestão.
- O usuário pode fechar a aplicação e, em outra sessão, consultar o histórico por empregado, período, tipo, status e/ou `requestId`, sem depender de estado mantido no navegador.

### Processamento dos documentos
- `requestId` e `comprovanteId` compõem a identidade idempotente do arquivo. Ex.: `exports/{requestId}/{comprovanteId}.pdf`.
- Ler comprovante -> chamar motor PDF -> upload com nome determinístico -> transação SQL que registra metadados e marca o documento como `COMPLETED`.
- Se upload funcionar e a transação SQL falhar, o retry pode verificar/substituir o mesmo blob; mecanismo de reconciliação deve detectar órfãos.
- `chunk(1)` na POC; **isso não cria atomicidade com o Azurite**. Chamadas remotas exigem timeouts e evitarão operações de longa duração em transações SQL quando houver refino da implementação.
- Jobs e seus itens possuem status separados: `PENDING`, `PROCESSING`, `COMPLETED`, `PARTIAL`, `FAILED` (conforme aplicável).

## 6. Escopo do MVP e não objetivos

### Incluído
- Dados fictícios para 5 empregados e 20 comprovantes distribuídos entre salários e benefícios.
- Lista e seleção de empregados por filtros de período e tipo.
- Solicitação assíncrona, Kafka, Spring Batch, PDF, Azurite, rastreamento e download.
- Simulações de falha de motor PDF, persistência e armazenamento; reprocessamento sem duplicidade.
- cURLs e Postman; documentação, migrations, scripts locais e evidências.

### Fora do escopo
- Autenticação real, RBAC, LGPD aplicada a dados reais, assinaturas digitais, folha de pagamento real, infraestrutura de produção, alta disponibilidade, multi-tenancy, ZIP e armazenamento de documentos no MySQL.
- Outbox transacional e DLQ/DLT são **evoluções arquiteturais** avaliáveis após a primeira entrega; retries idempotentes são obrigatórios desde o MVP.
- Dispositivos móveis nativos e operação em Azure real.

## 7. Decisões sujeitas a validação

| ID | Decisão | Estado |
|---|---|---|
| DEC-01 | Monorepo, seis apps, Maven multi-módulo para Java | Proposta |
| DEC-02 | Java 21 + Boot 4.1.x + Batch 6.0.x | Proposta |
| DEC-03 | Apache Kafka KRaft nativo Windows, sem Docker | Requer smoke test |
| DEC-04 | Azurite + paths lógicos no banco relacional | Proposta |
| DEC-12 | MySQL 8.4.13 LTS substitui PostgreSQL no domínio e no JobRepository | Aprovada — ADR-005 |
| DEC-05 | At-least-once + idempotência + compensação, não transação distribuída | Proposta |
| DEC-06 | Motor PDF stateless, um PDF por pagamento | Proposta |
| DEC-07 | Envio Kafka confirmado antes do 202, com possível atraso de consulta | Proposta |
| DEC-08 | Sem outbox na primeira iteração; registrar trade-off | Proposta |
| DEC-11 | Fire-and-forget na UI: `202` após ack do Kafka, sem polling automático e com consulta histórica filtrável sem exigir `requestId` | Aprovada |

## 8. Critério de conclusão da documentação e início do código

A Constituição e a SPEC deverão ser **aprovadas explicitamente** antes de gerar e executar `PLAN-001`/`TASKS-001`. Nenhum artefato deste pacote autoriza implementar ou commitar código automaticamente. Depois de aprovar o Playbook, cada passo terá critério objetivo de aceite, testes e evidências.

## 9. Referências oficiais (verificadas em 2026-10-23)

- Spring Boot: https://docs.spring.io/spring-boot/reference/
- Spring Batch: https://docs.spring.io/spring-batch/reference/
- Angular: https://angular.dev/reference/versions
- Kafka: https://kafka.apache.org/community/downloads/
- Azurite: https://learn.microsoft.com/en-us/azure/storage/common/storage-install-azurite
