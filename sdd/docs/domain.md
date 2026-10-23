# DOMAIN-001 — Payroll Export POC

## Agregados

| Agregado | Identidade | Responsabilidade |
|---|---|---|
| Empregado | `employeeId` | Cadastro fictício e vínculo com pagamentos. |
| Pagamento | `paymentId` | Fato financeiro imutável exportável como `SALARY` ou `BENEFIT`. |
| Solicitação | `requestId` UUID | Filtros, empregados, estado e contadores do processamento assíncrono. |
| Documento | `documentId` UUID | Um PDF lógico por `(requestId, paymentId)` e referência ao Azurite. |

## Estados

- Solicitação: `PENDING → PROCESSING → COMPLETED | PARTIAL | FAILED`.
- Documento: `PENDING → PROCESSING → COMPLETED | FAILED`.
- `COMPLETED` só é publicado após upload e commit dos metadados SQL.
- Redelivery e restart preservam a unicidade `(request_id, payment_id)`.

## Valores e filtros

- Competência: `YYYY-MM`, intervalo inclusivo.
- Dinheiro: `NUMERIC(15,2)`/`BigDecimal`.
- Tipos: `SALARY`, `BENEFIT`.
- Histórico: filtros opcionais por solicitação, empregado, período, tipo e estado, com paginação e ordenação determinística.

## Mensagens da interface

| Condição | Mensagem |
|---|---|
| Aceite assíncrono | Solicitação aceita. Consulte o histórico mais tarde. |
| Ainda processando | Sua solicitação ainda está sendo processada. Consulte novamente mais tarde. |
| Sem correspondência | Nenhum resultado encontrado para os filtros informados. |
