# Plano de ação — Fase F5 (Testes de integração com MySQL)

## Context

A F4 provou as regras de negócio e os formatos de resposta sem banco. Falta provar que a API funciona de ponta a ponta contra um MySQL de verdade: o EF Core lê e grava certo, as constraints do banco disparam, as regras de bloqueio e de alerta persistem, e a autenticação funciona com dados reais.

O critério de aceite do SDD v3 (seção 6.4 e 10) é: `dotnet test` verde para os 10 casos de integração IT-01 a IT-10, com o MySQL em Docker. O DoD da seção 6.6 pede pelo menos 10 testes de integração.

Estimativa do SDD: 5 horas.

## Decisões a confirmar

1. **Infraestrutura do banco de testes (recomendado: o MySQL do compose).** O SDD cita Testcontainers. Como a F0 já sobe o MySQL com o banco `cartao_sus_test`, os testes usam esse mesmo container. Vantagem: uma única infraestrutura, e o avaliador a conhece do README. Desvantagem: exige `docker compose up -d` antes de rodar os testes de integração. A alternativa, Testcontainers, sobe um container próprio por execução, mas adiciona um pacote e um requisito de Docker dentro do .NET.
2. **Quem prepara o banco de testes (recomendado: root só nos testes de preparação).** Os testes recriam o `cartao_sus_test` com `schema.sql` e `seed.sql` antes de cada classe. Esse script precisa de `CREATE` e `DROP`, que o usuário `cartao_app` não tem. Proponho usar o root apenas para essa preparação, e manter `cartao_app` na API, como em produção.
3. **IT-07 ajustado ao que a API expõe (recomendado).** O SDD pede "POST documento → GET prontuário → DELETE", mas o prontuário (E4) não lista documentos. Proponho verificar a inclusão e a exclusão pelo E3, que devolve `documentosDisponiveis`. Assim o teste confirma o efeito real na API, sem criar um endpoint só para o teste.

## Escopo (passo a passo)

### Passo 1 — Infraestrutura de testes
- Pacotes no projeto de testes: `Microsoft.AspNetCore.Mvc.Testing` (WebApplicationFactory) e `MySqlConnector` (já presente via Pomelo).
- `IntegrationFixture`: lê as variáveis do `.env` (ou do ambiente), monta a connection string do `cartao_sus_test` e recria o banco com `schema.sql` e `seed.sql`, executados como root.
- Script de execução `backend/tests/run-integration.sh`: confere se o MySQL está `healthy`, carrega o `.env` e roda `dotnet test --filter Category=Integracao`.
- Testes de integração marcados com `[Trait("Category", "Integracao")]`. Os unitários continuam rodando com `--filter Category!=Integracao`, sem banco.
- Testes de integração rodam **sequencialmente** (`DisableTestParallelization`), porque compartilham o mesmo banco.

### Passo 2 — `WebApplicationFactory` da API
- Subclasse de `WebApplicationFactory<Program>` que sobrescreve a configuração:
  - `ConnectionStrings:Default` apontando para `cartao_sus_test`, com o usuário `cartao_app`.
  - `SEED_DEMO=false` (o seed de demonstração já está no banco de testes).
  - `JWT_SECRET` e `SENHA_DEMO` de teste.
  - `SUS_API_BASE` apontando para um endereço inexistente, para não chamar o SUS real.
- Para o IT-08, substitui o `HttpMessageHandler` do `SusApiClient` por um falso, que responde 200 ou 503 conforme o teste.
- Cada classe de teste recebe um cliente HTTP da mesma fábrica.

### Passo 3 — Dados determinísticos
- Antes de cada classe, o banco volta ao estado do seed.
- Dentro do preparo, o `SEGREDO_TOTP` da Maria e do João recebe um valor conhecido (o do teste), para que o teste calcule o código do momento.
- Dispositivos IoT de teste: chaves conhecidas, com o hash gravado no banco pelo próprio teste (não pelo `gerar_chaves.py`).

### Passo 4 — Casos de integração

| ID | Classe | Fluxo | Verificação |
|---|---|---|---|
| IT-01 | `AuthIntegracaoTests` | Cadastro (E1) → login (E2) → prontuário (E4) do novo cidadão | 201 → 200 → 200 |
| IT-02 | `AuthIntegracaoTests` | Login de usuário com `ATIVO = 0` | 401 |
| IT-03 | `AcolhimentoIntegracaoTests` | Handshake completo com TOTP válido | 200 `AUTORIZADO`; linha em `TB_SESSAO_ACESSO` com status `AUTORIZADO` |
| IT-04 | `AcolhimentoIntegracaoTests` | Três falhas e uma quarta tentativa | 403 `BLOQUEADO` na quarta; linha de `TB_ALERTA` `TENTATIVA_INDEVIDA` |
| IT-05 | `TelemetriaIntegracaoTests` | Tensiômetro envia pressão acima do limiar | 201 com `alertaDisparado = true`; linha em `TB_ALERTA`; `ORIGEM = 'IOT'` |
| IT-06 | `TelemetriaIntegracaoTests` | Cidadão envia medição com JWT | 201; `ORIGEM = 'APP'` no banco |
| IT-07 | `DocumentosIntegracaoTests` | POST documento → E3 lista o tipo → DELETE → E3 não lista mais (decisão 3) | 201 → tipo presente → 204 → tipo ausente |
| IT-08 | `SusIntegracaoTests` | Proxy do CNES com resposta 200 e com 503 | 200 com dados normalizados; 502 `FALHA_ORIGEM` |
| IT-09 | `AuditoriaIntegracaoTests` | Admin lista auditoria com filtro | 200 paginado, com as sessões do seed |
| IT-10 | `SegurancaIntegracaoTests` | Rota protegida sem token; cidadão em rota de admin | 401 e 403 |

Cada teste confere a resposta HTTP **e** o efeito no banco, consultando a tabela diretamente com MySqlConnector. É isso que diferencia integração de contrato.

### Passo 5 — Evidência
- `dotnet test --filter Category=Integracao` com saída em `docs/evidencia-testes/f5-integracao.txt`.
- Resumo em `docs/evidencia-testes/f5-resumo.md`: tabela IT com resultado e tempo, no mesmo formato do resumo da F4.
- Export da coleção Postman (`docs/postman-collection.json`), pendente desde a F3. Gerado a partir do `swagger.json` da API rodando.

### Passo 6 — Documentação
- `backend/README.md`: seção "Testes de integração", com os pré-requisitos (MySQL em Docker) e o comando do `run-integration.sh`.
- README da raiz: marcar F5 como pronta.

## Verificação

1. `docker compose up -d` com o MySQL `healthy`. Sem ele, os testes de integração falham com mensagem clara, não com timeout.
2. `dotnet test --filter Category!=Integracao` roda sem banco (os 23 da F4 continuam passando).
3. `run-integration.sh` roda os 10 IT e termina verde.
4. Rodar a suíte duas vezes seguidas: o resultado é o mesmo, porque cada classe reconstrói o banco.
5. Quebrar um efeito de propósito (por exemplo, comentar a gravação da sessão no acolhimento) e confirmar que o IT-03 falha. Desfazer.
6. Coleção Postman importa no Postman sem erro e tem os 14 endpoints mais os extras.

**Critério de pronto da F5:** itens de 1 a 6 passam, `f5-integracao.txt` e `f5-resumo.md` estão em `docs/evidencia-testes/`, e a coleção Postman está versionada.

## Riscos

| Risco | Mitigação |
|---|---|
| Testes lentos por recriar o banco | Recriação por classe (não por teste); estimativa de cerca de 1 s por classe |
| Código TOTP cruzando a janela de 30 s no meio do teste | Teste calcula o código logo antes de enviar; ±1 passo absorve a diferença |
| Estado vazando entre classes | Sequencial e recriação por classe |
| Root no script de teste | Só para preparação do banco de testes, nunca na API |
| SUS real indisponível no teste | Handler falso no IT-08; nenhum teste chama a API pública |

## Arquivos a criar

- `backend/tests/CartaoSus.Tests/Integracao/` (fixture, fábrica, cliente de SUS falso e as 6 classes de teste)
- `backend/tests/run-integration.sh`
- `docs/evidencia-testes/f5-integracao.txt`, `f5-resumo.md` e `docs/postman-collection.json`

Arquivos existentes que mudam: `CartaoSus.Tests.csproj` (pacotes e configuração de paralelismo), `backend/README.md` e `README.md`.

Nenhuma mudança na API é prevista. Se um teste revelar um bug, a correção entra como commit próprio, separado da F5.

## Perguntas em aberto

1. **MySQL do compose ou Testcontainers?** Recomendo o compose.
2. **Root só para preparar o banco de testes?** Recomendo sim.
3. **IT-07 pelo E3 em vez do prontuário?** Recomendo sim, porque o prontuário não lista documentos.
