# ADR-002 — Consistência SQL e Blob

- Estado: ACEITO
- Decisão: usar nome determinístico de blob, transação local PostgreSQL e reconciliação; não afirmar atomicidade distribuída.
- Consequência: upload pode existir antes do commit SQL.
- Mitigação: retry reutiliza o blob e documentos só ficam `COMPLETED` após confirmação SQL.
