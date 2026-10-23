# ADR-003 — Consulta manual

- Estado: ACEITO
- Decisão: a UI não consulta automaticamente depois do POST.
- Consequência: o usuário atualiza a busca explicitamente.
- Mitigação: histórico filtrável e persistente, independente de `requestId` salvo no navegador.
