# Plano de ação — Fase F2 (Backend base)

## Context

A F2 cria o esqueleto da API: projeto ASP.NET Core, acesso ao MySQL com EF Core, autenticação JWT, hash de senha, Swagger, tratamento de erro padronizado e o seeder de demonstração. O critério de aceite do SDD v3 (seção 10) é: `dotnet run` sobe, o Swagger responde, e os endpoints E1 (cadastro) e E2 (login) funcionam via `curl`.

A F2 não implementa o domínio (E3 a E14, que são a F3). Ela entrega a fundação que a F3 e os testes da F4 e F5 vão usar. Por isso tudo aqui precisa estar testável sem subir o front.

Estimativa do SDD: 10 horas.

## Pré-requisitos e decisões a confirmar

1. **Versão do .NET.** A máquina só tem o SDK e o runtime do .NET 10. O material do curso usa .NET 8, mas o .NET 8 chega ao fim de suporte em novembro de 2026, que é agora. **Recomendado: .NET 10 (LTS)**, já instalado, com a mesma API do ASP.NET Core. Alternativa: instalar o .NET 8 (`brew install --cask dotnet-sdk@8`), o que exige sua ação.
2. **Usuário do banco.** Hoje o backend conectaria como `root`. **Recomendado:** criar um usuário `cartao_app` com permissão só em `cartao_sus` e `cartao_sus_test`, por um script em `docker/mysql/init/`. Isso muda a F0, então entra como um commit de ajuste.
3. **Endpoint de teste da autenticação.** Para provar que o JWT protege rotas antes da F3, proponho `GET /api/v1/auth/me` (devolve o usuário do token). Não está na matriz E1 a E14, então fica documentado como endpoint extra, como já foi feito com os endpoints do app.

## Escopo (passo a passo)

### Passo 1 — Solução e projeto
- Criar `backend/CartaoSus.sln` e o projeto `backend/src/CartaoSus.Api/` (Web API, controllers).
- Estrutura de pastas da SDD 5.4: `Controllers/`, `Services/`, `Repositories/`, `Models/`, `Dtos/`, `Security/`, `Data/`, `Seed/`, `Exceptions/`.
- `Program.cs` enxuto: registra serviços, autenticação, Swagger e middleware de erro. Nenhuma regra de negócio ali.
- O projeto de testes `backend/tests/CartaoSus.Tests/` **não** entra aqui (é a F4).

### Passo 2 — Pacotes NuGet
| Pacote | Uso |
|---|---|
| `Pomelo.EntityFrameworkCore.MySql` | Provedor MySQL do EF Core (`MySql.EntityFrameworkCore` também aparece no curso; Pomelo é o mais usado) |
| `Microsoft.AspNetCore.Authentication.JwtBearer` | Validação do JWT |
| `Microsoft.AspNetCore.Identity.Core` | Só o `PasswordHasher<T>` (sem o banco do Identity) |
| `Swashbuckle.AspNetCore` | Swagger UI e OpenAPI |

Versões fixadas no `.csproj` na versão estável mais recente compatível com o .NET escolhido. Nada de pacote fora dessa lista sem nova decisão.

### Passo 3 — Configuração e variáveis
- `appsettings.json` **sem valores secretos**. Só estrutura e padrões não sensíveis.
- Variáveis lidas do ambiente (o `.env` da raiz do projeto é carregado pelo `docker compose`; para o `dotnet run`, exportar as variáveis ou usar `dotnet user-secrets` apenas em desenvolvimento):
  - `ConnectionStrings__Default` (montada a partir de `MYSQL_*`)
  - `JWT_SECRET`, `SENHA_DEMO`, `SEED_DEMO`
- **Falha rápida:** se `JWT_SECRET` faltar ou tiver menos de 32 caracteres, a aplicação não sobe e mostra mensagem clara. Mesmo critério para a connection string.

### Passo 4 — Persistência (EF Core)
- `CartaoSusDbContext` com `DbSet` das 9 entidades do `schema.sql`.
- Entidades em `Models/` mapeadas com `ToTable`, `HasColumnName` e `HasConversion<string>()` nos `enum`. Os nomes batem com o SQL da F1.
- **Sem migrations.** O schema é o `database/schema.sql`. O EF não cria tabelas (`EnsureCreated` fica desligado).
- Repositórios de E1 e E2 (`IUsuarioRepository`, `ICidadaoRepository`): só consultas, sem regra de negócio. Retornam `Optional`-like (`null` ou entidade).

### Passo 5 — Autenticação e hash de senha
- `AuthService`:
  - Cadastro: valida, gera `SEGREDO_TOTP` (Base32, 20 bytes aleatórios), grava `CPF_HASH` (SHA-256 do CPF), senha com `PasswordHasher<Usuario>`.
  - Login: busca o usuário; se não existir ou a senha não bater, devolve **a mesma mensagem** (anti-enumeração, SDD 8.1).
- `JwtService`: emite HS256 com `sub`, `nome`, `perfil`, `idCidadao`, `exp` de 30 minutos. Lê a chave de `JWT_SECRET`.
- `Program.cs` configura `AddJwtBearer` com validação de emissor, audiência e assinatura.
- Usuário com `ATIVO = 0` não consegue logar (teste IT-02 da F5).

### Passo 6 — Erros padronizados
- Exceções de domínio em `Exceptions/`: `ValidacaoException`, `CredenciaisInvalidasException`, `ConflitoException`, `NaoEncontradoException`, `SemPermissaoException`.
- Um middleware (`ErrorHandlingMiddleware`) converte cada exceção no envelope do SDD 5.1: `timestamp`, `status`, `erro`, `mensagem`, `campos`.
- Validação de DTO (DataAnnotations) também cai no mesmo envelope, com `erro = VALIDACAO` e `campos` preenchido. Sem a página de erro padrão do ASP.NET (ProblemDetails desligado).
- Mapa de códigos: `VALIDACAO` 400, `NAO_AUTORIZADO` 401, `SEM_PERMISSAO` 403, `NAO_ENCONTRADO` 404, `CONFLITO` 409, `ERRO_INTERNO` 500.

### Passo 7 — Endpoints da F2
| Rota | Função | Status |
|---|---|---|
| `POST /api/v1/auth/cadastro` (E1) | Cria usuário e cidadão; devolve `segredoTotp` **uma vez** | 201, 400, 409 |
| `POST /api/v1/auth/login` (E2) | Autentica e emite JWT de 30 min | 200, 400, 401 |
| `GET /api/v1/auth/me` (extra) | Devolve os dados do token; prova a proteção por JWT | 200, 401 |

Regras de E1:
- `login` é e-mail válido; `senha` com 8 caracteres ou mais; `cpf` com 11 dígitos; `numeroCns` com 15 dígitos; `nomeCompleto` com 3 ou mais caracteres; `dataNascimento` no passado.
- CPF ou CNS já cadastrados → 409 com `CONFLITO`.

### Passo 8 — Swagger
- Swagger UI em `/swagger`, com o esquema de segurança `Bearer` (JWT) configurado.
- Título "Cartão SUS Inteligente API", versão 1.0 e descrição curta.
- Os demais endpoints entram na F3, e a documentação acompanha.

### Passo 9 — Seeder de demonstração (`SeedDemoService`)
Roda na inicialização se `SEED_DEMO=true`. É **idempotente**: só troca valores que ainda são `__SERA_GERADO__`.
- Gera o hash de `SENHA_DEMO` para os 4 usuários do seed.
- Gera `SEGREDO_TOTP` para os 2 cidadãos.
- Imprime os segredos TOTP **uma única vez** no console, para cadastrar no app depois. Não grava em arquivo.
- Se não houver nenhum `__SERA_GERADO__`, não faz nada.
- Se `SENHA_DEMO` estiver vazia com `SEED_DEMO=true`, a aplicação para com mensagem clara.

### Passo 10 — Documentação mínima
- `backend/README.md`: como rodar (`dotnet run`), variáveis obrigatórias, endereço do Swagger e como funciona o seeder.
- Atualizar o README da raiz: tabela de estrutura (`backend/` → Pronto F2) e seção "Como executar a API".

## Verificação

Com o MySQL da F0 rodando e o `schema.sql` e o `seed.sql` da F1 aplicados:

1. `dotnet build backend/CartaoSus.sln` sem erros e sem avisos de nulabilidade novos.
2. Subir sem `JWT_SECRET` → a aplicação para com mensagem clara. Mesmo teste sem `SENHA_DEMO` com `SEED_DEMO=true`.
3. `dotnet run` → `curl -i http://localhost:8080/swagger/index.html` retorna 200.
4. **E1:** cadastro novo → 201 com `segredoTotp`. Repetir o mesmo CPF → 409. Enviar `senha` curta → 400 com `campos.senha`.
5. **E2:** login com senha certa → 200 com `token`. Senha errada → 401. Usuário inexistente → 401 com **a mesma mensagem**.
6. **JWT:** `GET /api/v1/auth/me` sem token → 401; com token válido → 200; com token adulterado → 401.
7. **Seeder:** primeira execução preenche placeholders e imprime segredos. Segunda execução não altera nada (conferir com `SELECT` que os hashes não mudaram).
8. Confirmar que nenhum segredo entrou no repositório: `git grep` por `JWT_SECRET=`, senha demo e segredos TOTP impressos.
9. **Evidência:** salvar a saída dos `curl` em `docs/evidencia-testes/f2-*.txt`.

**Critério de pronto da F2:** itens 1 a 8 passam, o Swagger lista E1, E2 e `/me`, e a saída dos `curl` está em `docs/evidencia-testes/`.

## Riscos

| Risco | Mitigação |
|---|---|
| Versão do .NET fora do curso | Decisão 1 acima, com justificativa registrada no README |
| Pomelo incompatível com a versão do EF | Usar a versão do Pomelo que corresponde ao EF da mesma major |
| Hash de senha lento na demo | PBKDF2 do Identity usa 10 mil iterações por padrão, aceitável para o MVP |
| Segredo TOTP vazar no log | Impressão única no console, sem gravação; nada no `appsettings` |
| Porta 8080 ocupada | Configurar `ASPNETCORE_URLS` com porta livre e documentar no README |

## Arquivos a criar

- `cartao-sus/backend/CartaoSus.sln`
- `cartao-sus/backend/src/CartaoSus.Api/` (`.csproj`, `Program.cs`, `appsettings.json`, `appsettings.Development.json` sem segredos, e as pastas da passo 1)
- `cartao-sus/backend/README.md`
- `cartao-sus/docs/evidencia-testes/f2-*.txt`
- Se a decisão 2 for aprovada: `cartao-sus/docker/mysql/init/02-usuario-app.sql` (usuário com permissão mínima)

Arquivos existentes que mudam: `cartao-sus/README.md` (tabela de estrutura e seção de execução da API) e, se a decisão 2 for aprovada, `cartao-sus/docker-compose.yml` (variável de conexão do app).

## Perguntas em aberto

1. **.NET 10 (recomendado) ou instalar .NET 8?**
2. **Criar usuário de banco com permissão mínima (recomendado) ou manter root na F2?**
3. **Incluir `GET /api/v1/auth/me` como endpoint extra?**
