# Backend — Cartão SUS Inteligente (ASP.NET Core)

API REST do MVP. Stack: .NET 10, ASP.NET Core Web API, Entity Framework Core (Pomelo / MySQL), JWT Bearer, Swagger.

Desenho em camadas (Controllers → Services → Repositories) conforme o SDD v3, seção 5.4.

## Pré-requisitos

- Banco da F0 rodando e com `schema.sql` e `seed.sql` aplicados (F1). Ver [README da raiz](../README.md).
- .NET SDK 10 (`dotnet --version` deve mostrar 10.x).

## Variáveis de ambiente

Lidas do `.env` da pasta `cartao-sus/` (não versionado). Para o `dotnet run`, carregue o arquivo antes de rodar:

```bash
cd cartao-sus
set -a && . ./.env && set +a
```

| Variável | Obrigatória | Função |
|---|---|---|
| `MYSQL_APP_PASSWORD` | sim | senha do usuário `cartao_app` (permissão mínima) |
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` | não | padrão `127.0.0.1` / `3306` / `cartao_sus` |
| `JWT_SECRET` | sim | chave HS256, 32 caracteres ou mais |
| `SEED_DEMO` | não | `true` aplica os dados de demonstração na primeira execução |
| `SENHA_DEMO` | sim, se `SEED_DEMO=true` | senha dos usuários de demonstração (8 caracteres ou mais) |

Se alguma variável obrigatória faltar, a aplicação encerra com mensagem clara (evidência em `docs/evidencia-testes/f2-configuracao.txt`).

## Como executar

```bash
cd backend/src/CartaoSus.Api
dotnet run
```

A API sobe em `http://localhost:8080`. Swagger: `http://localhost:8080/swagger`.

## Seeder de demonstração

Com `SEED_DEMO=true`, na inicialização o backend troca os placeholders `__SERA_GERADO__` do seed:
- gera o hash da `SENHA_DEMO` para os 4 usuários;
- gera o segredo TOTP dos 2 cidadãos e **imprime no console uma única vez**, para cadastrar no app.

É idempotente: se não houver placeholder, não faz nada.

## Endpoints da F2

| Método | Rota | Função |
|---|---|---|
| POST | `/api/v1/auth/cadastro` | E1 — cria conta de cidadão (201) |
| POST | `/api/v1/auth/login` | E2 — emite JWT de 30 minutos (200) |
| GET | `/api/v1/auth/me` | Extra — dados do token (200 / 401) |

Erros seguem o envelope do SDD 5.1: `timestamp`, `status`, `erro`, `mensagem`, `campos`.

## Testes

Os testes automatizados (xUnit) entram na F4. Nesta fase, a evidência é o `curl` documentado em `docs/evidencia-testes/f2-curl.txt`.

## Observação sobre dependências

O `dotnet list package --vulnerable` aponta avisos para `MySqlConnector` e `System.Security.Cryptography.Pkcs`, puxados pelo Pomelo e pelo JWT Bearer. As versões corrigidas ainda não estão publicadas nos pacotes usados, então o risco foi aceito e registrado. Revisar quando houver atualização.
