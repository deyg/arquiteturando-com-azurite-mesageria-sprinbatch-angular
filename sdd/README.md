# Payroll Export POC

**Estado: implementação e verificação concluídas em 2026-10-23.**

A POC exporta comprovantes de pagamento de forma assíncrona. O navegador apenas solicita e consulta; Kafka, Spring Batch, PostgreSQL e Azurite preservam o trabalho depois que a tela é fechada.

## Componentes e portas

| Componente | Porta | Responsabilidade |
|---|---:|---|
| Angular | 4200 | seleção, solicitação e histórico manual |
| BFF | 8080 | única API consumida pelo Angular |
| Request API | 8081 | validação, publicação Kafka e resposta `202` |
| Query API | 8082 | consultas e stream de downloads |
| Batch Worker | 8083 | consumo idempotente, job e recuperação |
| PDF Motor | 8084 | renderização HTTP de PDF |
| Kafka | 9092 | evento versionado de solicitação |
| PostgreSQL | 5432 | domínio, estado e metadados Spring Batch |
| Azurite Blob | 10000 | conteúdo binário dos PDFs |

```text
Angular -> BFF -> Request API -> Kafka -> Batch Worker -> PDF Motor
             \-> Query API                 |  |  |
                    |                       |  |  +-> Azurite
                    +-> PostgreSQL/Azurite  |  +----> PostgreSQL
                                            +-------> recuperação no restart
```

## Execução local no Windows

Pré-requisitos: Java 21, Maven Wrapper, PostgreSQL, Kafka KRaft, Azurite e Node.js compatível com Angular 22. Não há Docker nem credenciais Azure.

```powershell
cd sistemas
.\scripts\check-prereqs.ps1
.\scripts\start-local.ps1
.\scripts\smoke.ps1
.\scripts\e2e.ps1
```

Para a UI:

```powershell
cd sistemas\apps\web-angular
npm install
npm start
```

Ao terminar, pare somente os processos registrados pelo iniciador:

```powershell
cd sistemas
.\scripts\stop-local.ps1
```

## Operação e testes

- API pública: `http://127.0.0.1:8080`.
- Health checks: `/actuator/health` em cada aplicação Java.
- Coleção Postman: `sistemas/postman/payroll-export-poc.postman_collection.json`.
- Contratos e cURLs: [docs/local-api.md](docs/local-api.md).
- Arquitetura detalhada: [docs/architecture.md](docs/architecture.md).
- Rastreabilidade e resultado final: [docs/release-report.md](docs/release-report.md).

## Limitações conhecidas

- POC local e monoestação; autenticação, autorização e TLS não foram incluídos.
- O Angular não faz polling automático por decisão arquitetural; a atualização do histórico é manual.
- Não há transação distribuída entre Kafka, PostgreSQL, HTTP e Blob. A consistência é obtida por IDs determinísticos, constraints, retry finito e reconciliação.
- O script de start pressupõe que PostgreSQL, Kafka e Azurite já estejam ativos; o preflight informa o que falta.
