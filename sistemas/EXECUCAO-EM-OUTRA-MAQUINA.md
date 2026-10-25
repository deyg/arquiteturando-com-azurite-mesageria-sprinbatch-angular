# Execucao em outra maquina

Ao preparar e executar o projeto em outra maquina, observe os cuidados abaixo.

1. O ambiente esperado e Windows com PowerShell, pois os scripts utilizam comandos e caminhos especificos do Windows.

2. Instale o Java 21 e confirme a versao:

   ```powershell
   java -version
   ```

3. O MySQL 8.4.13 deve estar instalado em:

   ```text
   %LOCALAPPDATA%\payroll-export-poc\mysql\mysql-8.4.13-winx64
   ```

4. O Kafka 4.3.1, distribuicao Scala 2.13, deve estar instalado em:

   ```text
   %LOCALAPPDATA%\payroll-export-poc\kafka\kafka_2.13-4.3.1
   ```

5. O arquivo de configuracao KRaft deve existir em:

   ```text
   %LOCALAPPDATA%\payroll-export-poc\kafka-server.properties
   ```

6. O Azurite deve estar instalado e o comando `azurite-blob.cmd` precisa estar disponivel no `PATH`.

7. Para o Angular, instale uma versao compativel do Node.js. O projeto preve Node.js 26.12.1 e npm 11.20.0.

8. Quando for usada a instalacao local prevista pelos scripts, o Node.js deve estar em:

   ```text
   %LOCALAPPDATA%\payroll-export-poc\node\node-v26.12.1-win-x64
   ```

9. As portas `3306`, `9092`, `10000`, `8080` a `8084` e `4200` devem estar livres. Conflitos com outros programas impedem ou confundem a inicializacao.

10. Firewall, antivirus ou politicas corporativas podem bloquear Java, Node.js, MySQL ou conexoes locais.

11. A primeira execucao pode precisar de acesso a internet para Maven e npm baixarem dependencias que ainda nao estejam no cache local.

12. O usuario precisa ter permissao de escrita em `%LOCALAPPDATA%\payroll-export-poc` e na pasta do projeto.

13. O banco e seus dados nao sao transferidos pelo Git. Na nova maquina, o MySQL utilizara um novo diretorio local de dados.

14. Na primeira inicializacao, a `query-api` executara automaticamente o Flyway, criara as tabelas de dominio e do Spring Batch e inserira os dados ficticios.

15. O conteudo do Azurite e local e nao sera transferido. PDFs gerados em outra maquina nao estarao disponiveis.

16. O historico e os offsets do Kafka tambem sao locais; mensagens antigas nao serao transferidas.

17. Evite executar testes de integracao contra o banco usado para demonstracao. Alguns testes criam registros de falha proposital, como `injected PDF failure`.

18. Antes de iniciar o ambiente, entre na pasta `sistemas` e verifique os pre-requisitos:

   ```powershell
   cd C:\caminho\do\projeto\sistemas
   .\scripts\check-prereqs.ps1
   ```

19. Para iniciar todo o ambiente a partir da pasta `sistemas`, execute:

   ```powershell
   .\scripts\start-ordered.ps1
   ```

   Se o PowerShell ja estiver na pasta `sistemas\scripts`, execute:

   ```powershell
   .\start-ordered.ps1
   ```

20. As configuracoes padrao usam `127.0.0.1`. Para permitir acesso por outra maquina da rede, sera necessario revisar os hosts de bind, URLs anunciadas, CORS, firewall e a configuracao `advertised.listeners` do Kafka.

## Ordem automatizada

O `start-ordered.ps1` executa a seguinte sequencia:

1. valida as ferramentas;
2. inicia o MySQL;
3. inicia o Kafka em modo KRaft;
4. inicia o Azurite Blob;
5. compila as aplicacoes Java;
6. inicia a `query-api` e aguarda as migracoes Flyway;
7. inicia a `pdf-motor-api`;
8. inicia o `batch-worker`;
9. inicia a `request-api`;
10. inicia o `bff-api`;
11. instala, quando necessario, as dependencias e inicia o Angular.

Ao final, a aplicacao estara disponivel em `http://127.0.0.1:4200` e as metricas em `http://127.0.0.1:8083/actuator/prometheus`.
