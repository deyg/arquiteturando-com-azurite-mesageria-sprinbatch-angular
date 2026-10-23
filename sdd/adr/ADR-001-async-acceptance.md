# ADR-001 — Aceite assíncrono

- Estado: ACEITO
- Decisão: retornar `202` somente após ack do Kafka; a persistência ocorre no worker.
- Consequência: existe uma janela em que o `requestId` ainda não aparece na consulta.
- Mitigação: mensagem neutra e consulta manual posterior; sem polling automático.
