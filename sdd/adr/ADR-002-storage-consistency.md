# ADR-002 — Consistência SQL e Blob

- Estado: ACEITO
- Decisão: usar nome determinístico de blob, transação local no banco relacional e reconciliação; não afirmar atomicidade distribuída. Nesta branch, o banco é MySQL 8.4.13 LTS conforme ADR-005.
- Consequência: upload pode existir antes do commit SQL.
- Mitigação: retry reutiliza o blob e documentos só ficam `COMPLETED` após confirmação SQL.
