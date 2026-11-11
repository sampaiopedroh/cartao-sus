# Plano de ação — Fase F4 (Testes unitários e de contrato)

## Context

A F3 entregou o domínio, mas a verificação até agora foi manual, por `curl`. A F4 cria a primeira camada automatizada: testes unitários das regras de negócio e testes de contrato dos formatos de resposta que o app Android e os scripts IoT consomem.

O critério de aceite do SDD v3 (seção 10, F4 e seção 6.6) é: `dotnet test` verde para unitários e contrato, **sem banco de dados**, com pelo menos 12 unitários (UT-01 a UT-12) e 8 contratos (CT-01 a CT-08).

Estimativa do SDD: 5 horas.

## Decisões a confirmar

1. **Refatorar o `AuthService` (recomendado).** Hoje ele depende do `CartaoSusDbContext` direto, para abrir uma transação no cadastro e ler o cidadão no login. Isso impede testar UT-09 e UT-10 sem MySQL. A refatoração troca a transação manual por um único `SaveChanges` (o EF já executa cada `SaveChanges` numa transação, e o `Usuario` entra junto pelo `Cidadao.Usuario`), e troca a consulta direta pelo repositório. O comportamento visível da API não muda. Depois da refatoração, eu rodo de novo a bateria de F3 como regressão.
2. **Versão do Moq (recomendado: 4.18.4).** As versões 4.20.x incluem telemetria (SponsorLink) que foi alvo de polêmica na comunidade. A versão 4.18.4 é estável e sem telemetria. Alternativa: usar a versão mais recente.
3. **Contratos como snapshot de formato, sem banco (recomendado).** A F4 é "sem integração". Os testes de contrato comparam o nome e o tipo de cada campo da resposta com um arquivo JSON de referência. Os testes contra o banco real ficam para a F5.

## Escopo (passo a passo)

### Passo 1 — Projeto de testes
- Criar `backend/tests/CartaoSus.Tests/CartaoSus.Tests.csproj`, com xUnit, `Microsoft.NET.Test.Sdk`, Moq (versão da decisão 2) e referência ao projeto da API.
- Adicionar ao `CartaoSus.slnx`.
- Organização: `Unitarios/`, `Contrato/`, `Contrato/Snapshots/` (arquivos JSON).

### Passo 2 — Refatoração do cadastro e do login (decisão 1)
- `AuthService` deixa de receber `CartaoSusDbContext`.
- Cadastro: cria o `Cidadao` com o `Usuario` na propriedade de navegação, e salva uma vez. O segredo TOTP continua sendo gerado no serviço.
- Login: busca o cidadão pelo repositório (`PorUsuarioAsync`).
- Nenhuma mudança de contrato HTTP.

### Passo 3 — Testes unitários (UT-01 a UT-12)

| ID | Arquivo | O que verifica | Técnica |
|---|---|---|---|
| UT-01 | `TotpServiceTests` | Código válido no passo atual é aceito | Vetor oficial da RFC 6238 (segredo ASCII `12345678901234567890`, instante 59 s → `287082`), além do código gerado para o instante atual |
| UT-02 | `TotpServiceTests` | Código de 5 minutos atrás é recusado | Instante fixo, fora da janela de ±1 passo |
| UT-03 | `TotpServiceTests` | Mesmo código usado duas vezes é recusado | `AntiReusoTotp.Registrar` chamado duas vezes com o mesmo contador |
| UT-04 | `TelemetriaServiceTests` | Sistólica de 185 gera `PRESSAO_CRITICA` | Moq nos repositórios; verifica `AdicionarAsync(Alerta)` com o tipo certo |
| UT-05 | `TelemetriaServiceTests` | Pressão de 120 não gera alerta | Mesmo setup; verifica que nenhum alerta foi criado |
| UT-06 | `TelemetriaServiceTests` | Glicemia de 310 gera `GLICEMIA_CRITICA` | Mesmo setup |
| UT-07 | `AcolhimentoServiceTests` | 3 falhas em 10 min bloqueiam e geram `TENTATIVA_INDEVIDA` | Moq: contagem de rejeições = 3; verifica `BloqueadoException` |
| UT-08 | `AcolhimentoServiceTests` | 2 falhas seguidas de um código válido autorizam | Moq: contagem = 2; código gerado no instante atual; verifica sessão `AUTORIZADO` |
| UT-09 | `AuthServiceTests` | Senha errada lança `CredenciaisInvalidasException` | Moq do repositório com senha hasheada |
| UT-10 | `AuthServiceTests` | Senha é gravada com hash, nunca em texto puro | Verifica que `SenhaHash != senha` e que `PasswordHasher.VerifyHashedPassword` confirma a senha |
| UT-11 | `DocumentoServiceTests` | Excluir documento de outro cidadão lança `SemPermissaoException` | Moq do repositório, com documento de outro dono |
| UT-12 | `SusApiClientTests` | API do SUS retornando 503 vira `FalhaOrigemException` | `HttpMessageHandler` falso que sempre responde 503 |

Regras de qualidade dos testes:
- Cada teste tem um único motivo para falhar.
- Nada de `Thread.Sleep`. O tempo é passado por parâmetro ou calculado no próprio teste.
- Nenhum teste abre conexão com MySQL.

### Passo 4 — Testes de contrato (CT-01 a CT-08)
Os testes serializam as respostas com as mesmas opções JSON da API (camelCase) e comparam, campo a campo, com o arquivo de referência em `Contrato/Snapshots/`. A comparação checa nome e tipo do campo. Valores não são comparados.

| ID | Interação | Snapshot |
|---|---|---|
| CT-01 | Login 200 | `login-200.json`: `token`, `tipo`, `expiraEmMinutos`, `perfil`, `idCidadao`, `nome` |
| CT-02 | Login 401 | `erro-401.json`: envelope `timestamp`, `status`, `erro`, `mensagem`, `campos` |
| CT-03 | Prontuário 200 | `prontuario-200.json`: `historico[]` e `alertasPendentes[]` |
| CT-04 | Prontuário 403 | `erro-403.json`: envelope com `erro = SEM_PERMISSAO` |
| CT-05 | Validar NFC 200 | `validar-nfc-200.json`: `status`, `mensagem`, `paciente` com `alertasCriticos` e `ultimasMedicoes` |
| CT-06 | Validar NFC 403 | `erro-403-token.json`: envelope com `erro = TOKEN_INVALIDO` |
| CT-07 | Telemetria 201 | `telemetria-201.json`: `idSinal`, `alertaDisparado`, `alerta` |
| CT-08 | Excluir documento 204 | Sem corpo: verifica que o controller devolve `NoContent` |

Para CT-02, CT-04 e CT-06, o teste roda o `ErrorHandlingMiddleware` com um `DefaultHttpContext`, para pegar o envelope real.

### Passo 5 — Execução e evidência
- `dotnet test backend/CartaoSus.slnx` com saída salva em `docs/evidencia-testes/f4-dotnet-test.txt`.
- Relatório resumido em `docs/evidencia-testes/f4-resumo.md`: tabela UT e CT com nome do teste, resultado e tempo.

### Passo 6 — Documentação
- `backend/README.md`: seção "Testes", com o comando e a contagem.
- README da raiz: marcar F4 como pronta.

## Verificação

1. `dotnet build` sem erros.
2. `dotnet test` verde para os 12 UT e os 8 CT. Contagem conferida na saída.
3. `dotnet test` roda **sem MySQL ligado**. Parar o container antes e confirmar.
4. Quebrar um contrato de propósito (renomear um campo num snapshot) e confirmar que o teste de contrato falha. Depois desfazer.
5. Regressão: rodar `docs/evidencia-testes/f3-testes.py` contra a API depois da refatoração do `AuthService`, e confirmar 48 de 48 OK.

**Critério de pronto da F4:** os itens de 1 a 5 passam, a evidência está em `docs/evidencia-testes/`, e nenhum teste depende de banco.

## Riscos

| Risco | Mitigação |
|---|---|
| Teste de TOTP com janela na virada do passo | Testes de geração usam o contador calculado no mesmo instante; a janela de ±1 absorve a diferença |
| Mock com comportamento diferente do EF | Testes unitários testam regra de negócio; o comportamento do EF fica para a F5 |
| Refatoração do `AuthService` quebrar o cadastro | Regressão com a bateria da F3 (passo 5) |
| Moq com telemetria | Versão pinada (decisão 2) |

## Arquivos a criar

- `backend/tests/CartaoSus.Tests/CartaoSus.Tests.csproj`
- `backend/tests/CartaoSus.Tests/Unitarios/` (TOTP, acolhimento, telemetria, autenticação, documentos, cliente SUS)
- `backend/tests/CartaoSus.Tests/Contrato/ContratoTests.cs` e `Contrato/Snapshots/*.json`
- `docs/evidencia-testes/f4-dotnet-test.txt` e `f4-resumo.md`

Arquivos existentes que mudam: `Services/Servicos.cs` (acolhimento usa o relógio do teste apenas via parâmetro, se necessário), `Services/AuthService.cs` (refatoração do passo 2), `CartaoSus.slnx` e `backend/README.md`.

## Perguntas em aberto

1. **Refatorar o `AuthService` para tirar a dependência do banco?** Recomendo sim, porque sem isso UT-09 e UT-10 exigem MySQL.
2. **Moq 4.18.4 (sem telemetria) ou a versão mais recente?** Recomendo 4.18.4.
