# ADR-004 — Observabilidade do Spring Batch

- Estado: APROVADO
- Data: 2026-10-23

## Contexto

O histórico funcional de exportações informa o resultado percebido pelo usuário, mas não explica duração de jobs e steps, vazão, retries, skips, rollbacks, redeliveries ou recuperação. O `JobRepository` contém parte desses dados, enquanto latências e percentis operacionais exigem instrumentação.

## Decisão

O `batch-worker` terá uma API técnica interna e somente leitura em `/internal/v1/batch`. Ela combinará metadados do Spring Batch com métricas Micrometer. Métricas históricas e correlações vêm do banco; timers, counters, gauges, histogramas e percentis vêm do Micrometer.

O monitor não será roteado pelo BFF. Identificadores individuais ficam em logs e respostas detalhadas, nunca em tags de métricas. A primeira versão não terá comandos administrativos.

## Consequências

- separação clara entre histórico funcional e diagnóstico técnico;
- possibilidade de scrape Prometheus e consultas locais amigáveis;
- necessidade de instrumentar limites de dependência e eventos de recuperação;
- necessidade de limitar paginação, janelas e cardinalidade;
- futura exposição remota exigirá autenticação, autorização, TLS e auditoria.

## Alternativas rejeitadas

- usar apenas tabelas `BATCH_*`: não oferece latências externas nem percentis adequados;
- usar apenas Micrometer: não fornece detalhes e correlação histórica suficientes;
- publicar pelo BFF: mistura contrato funcional com operação interna;
- permitir restart/stop na primeira versão: amplia risco e requisitos de segurança.
