# Checklist de QA manual

1. Abra um PowerShell na raiz do projeto:

   ```powershell
   cd C:\29-arquiteturando-com-azurite-mesageria-sprinbatch-angular
   ```

2. Confira os pré-requisitos:

   ```powershell
   cd sistemas
   .\scripts\check-prereqs.ps1
   ```

3. Suba a infraestrutura local:

   - PostgreSQL na porta `5432`;
   - Kafka na porta `9092`;
   - Azurite Blob na porta `10000`.

4. Confirme novamente que a infraestrutura está disponível:

   ```powershell
   .\scripts\check-prereqs.ps1
   ```

   As portas `5432`, `9092` e `10000` devem aparecer com os serviços esperados.

5. Faça o build/deploy local dos cinco aplicativos Java:

   ```powershell
   .\mvnw.cmd clean package
   ```

6. Suba os aplicativos Java:

   ```powershell
   .\scripts\start-local.ps1 -SkipBuild
   ```

7. Aguarde aproximadamente 30 segundos.

8. Execute o smoke test:

   ```powershell
   .\scripts\smoke.ps1
   ```

   Resultado esperado: consultas com HTTP `200` e solicitação de exportação com HTTP `202`.

9. Confira os serviços individualmente no navegador:

   - BFF: <http://localhost:8080/actuator/health>
   - Request API: <http://localhost:8081/actuator/health>
   - Query API: <http://localhost:8082/actuator/health>
   - Batch Worker: <http://localhost:8083/actuator/health>
   - PDF Motor: <http://localhost:8084/actuator/health>

   Resultado esperado: `"status":"UP"`.

10. Abra outro PowerShell e entre no Angular:

    ```powershell
    cd C:\29-arquiteturando-com-azurite-mesageria-sprinbatch-angular\sistemas\apps\web-angular
    ```

11. Instale as dependências da tela, se necessário:

    ```powershell
    npm install
    ```

12. Suba a tela:

    ```powershell
    npm start
    ```

13. Abra no navegador: <http://localhost:4200>.

14. Na tela, informe o período inicial e final.

15. Selecione um ou mais tipos de pagamento.

16. Clique para pesquisar os empregados elegíveis.

17. Selecione um ou mais empregados.

18. Confira os pagamentos apresentados.

19. Solicite a exportação.

    Resultado esperado: mensagem de solicitação aceita, sem precisar esperar a geração do PDF.

20. Feche ou atualize a página para confirmar que o processamento não depende do navegador.

21. Volte à tela e consulte manualmente o histórico usando os filtros.

22. Confira o estado da solicitação:

    - `PROCESSING`: ainda processando;
    - `COMPLETED`: concluída;
    - `PARTIAL`: alguns documentos falharam;
    - `FAILED`: processamento sem documentos concluídos.

23. Abra os documentos da solicitação concluída.

24. Clique em download.

    Resultado esperado: arquivo PDF válido baixado pelo BFF a partir do Azurite.

25. Opcionalmente, execute o E2E automatizado:

    ```powershell
    cd C:\29-arquiteturando-com-azurite-mesageria-sprinbatch-angular\sistemas
    .\scripts\e2e.ps1
    ```

26. Ao terminar, interrompa o Angular com `Ctrl+C`.

27. Pare os cinco aplicativos Java:

    ```powershell
    .\scripts\stop-local.ps1
    ```

28. Confirme que as portas `8080` a `8084` foram liberadas:

    ```powershell
    .\scripts\check-prereqs.ps1
    ```

## QA manual do Monitor Spring Batch

1. Com a infraestrutura e os cinco aplicativos ativos, execute uma exportação pela tela ou pelo smoke.
2. Abra <http://localhost:8083/internal/v1/batch/jobs> e confirme `exportPayrollDocumentsJob`.
3. Abra <http://localhost:8083/internal/v1/batch/executions?page=0&size=20> e copie um `jobExecutionId`.
4. Consulte `/internal/v1/batch/executions/{jobExecutionId}` e confirme `requestId`, parâmetros e três steps.
5. Consulte `/internal/v1/batch/requests/{requestId}` e confirme a correlação com a solicitação.
6. Informe uma janela UTC em `/internal/v1/batch/metrics/summary?from=2026-10-23T00:00:00Z&to=2026-10-24T00:00:00Z`.
7. Confira duração, percentis, vazão, sucesso, backlog, retry, skip e rollback.
8. Abra <http://localhost:8083/actuator/prometheus> e procure métricas `payroll_batch_*`.
9. Execute `.\scripts\monitor-baseline.ps1` para uma medição local reproduzível.
10. Importe `postman/batch-monitor.postman_collection.json` e `postman/local.postman_environment.json` para repetir as consultas.

O monitor é interno e somente leitura; não existem endpoints para iniciar, parar ou reiniciar jobs.
