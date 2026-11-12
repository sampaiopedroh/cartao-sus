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

## Endpoints (F2 e F3)

| Método | Rota | Quem chama | Função |
|---|---|---|---|
| POST | `/api/v1/auth/cadastro` | público | E1 — cria conta de cidadão (201) |
| POST | `/api/v1/auth/login` | público | E2 — emite JWT de 30 min (200) |
| GET | `/api/v1/auth/me` | qualquer usuário | Extra — dados do token |
| POST | `/api/v1/acolhimento/validar-nfc` | terminal NFC (`X-API-KEY`) | E3 — handshake com TOTP, bloqueio após 3 falhas em 10 min |
| POST | `/api/v1/telemetria/sinais-vitais` | cidadão (JWT) ou tensiômetro (`X-API-KEY`) | E6 — medição com alerta por limiar |
| GET | `/api/v1/cidadao/{id}/prontuario` | cidadão (próprio) ou profissional com acolhimento recente | E4 |
| GET | `/api/v1/cidadao/{id}/sinais-vitais` | mesmo acima | E5 — filtros `tipo`, `de`, `ate` |
| PUT | `/api/v1/cidadao/{id}/historico/{idHistorico}` | cidadão (próprio) | E7 |
| GET | `/api/v1/cidadao/{id}/alertas` | mesmo acima | E10 — paginado |
| PUT | `/api/v1/cidadao/{id}/alertas/{idAlerta}` | cidadão (próprio) | Extra — marca ENTREGUE (204) |
| GET | `/api/v1/cidadao/{id}/acessos` | mesmo acima | Extra — últimos acessos |
| POST | `/api/v1/documentos` | cidadão | E8 |
| DELETE | `/api/v1/documentos/{idDoc}` | cidadão (dono) | E9 (204) |
| GET | `/api/v1/sus/equipamentos/{cnes}` | cidadão ou profissional | E11 — proxy do CNES |
| GET | `/api/v1/sus/medicamentos?municipio=&catmat=&page=&size=` | cidadão ou profissional | E12 — proxy do BNAFAR |
| GET | `/api/v1/sus/medicamentos/catalogo?nome=` | cidadão ou profissional | Extra — catálogo local |
| GET | `/api/v1/unidades?page=&size=` | cidadão, profissional, admin | E13 |
| GET | `/api/v1/admin/auditoria?status=&de=&ate=&page=&size=` | admin | E14 |

## Dispositivos IoT (chaves X-API-KEY)

As chaves dos dispositivos são geradas pelo `iot/gerar_chaves.py`, na pasta `cartao-sus/`:

```bash
python3 iot/gerar_chaves.py
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/seed_dispositivos.sql
```

O script grava as chaves no `.env` e os hashes SHA-256 em `database/seed_dispositivos.sql`. Os dois arquivos ficam fora do git. O banco guarda só o hash.

## Regras de negócio implementadas

- **TOTP (RFC 6238):** HMAC-SHA1, código de 6 dígitos, janela de ±1 passo de 30 s. Código já usado não é aceito de novo (anti-reuso).
- **Bloqueio:** 3 sessões `REJEITADO` em 10 min bloqueiam novas tentativas do CNS por 10 min (`BLOQUEADO`). A 3ª falha gera alerta `TENTATIVA_INDEVIDA`.
- **Alertas de telemetria:** sistólica acima de 180 ou diastólica acima de 110 gera `PRESSAO_CRITICA`. Glicemia de 300 ou mais gera `GLICEMIA_CRITICA`.
- **Consentimento:** profissional só lê o prontuário se houver acolhimento `AUTORIZADO` do cidadão nos últimos 10 min.

## Limitações conhecidas

- **Anti-reuso do TOTP em memória:** vale para uma única instância da API. Com várias instâncias, o controle precisa ir para o banco.
- **`totalElements` do E12:** a API do BNAFAR não informa o total. O valor devolvido é o número de registros até a página atual.
- **Horário:** o servidor grava os horários no fuso local da máquina, como nos dados de demonstração.

## Endpoints da F2

| Método | Rota | Função |
|---|---|---|
| POST | `/api/v1/auth/cadastro` | E1 — cria conta de cidadão (201) |
| POST | `/api/v1/auth/login` | E2 — emite JWT de 30 minutos (200) |
| GET | `/api/v1/auth/me` | Extra — dados do token (200 / 401) |

Erros seguem o envelope do SDD 5.1: `timestamp`, `status`, `erro`, `mensagem`, `campos`.

## Testes

Dois conjuntos de testes automatizados, sem banco de dados:

```bash
cd backend
dotnet test CartaoSus.slnx
```

- **Unitários** (`tests/CartaoSus.Tests/Unitarios/`): regras de negócio com repositórios simulados (Moq). Cobrem TOTP, bloqueio e alertas, telemetria, cadastro e login, documentos e a falha do SUS.
- **Contrato** (`tests/CartaoSus.Tests/Contrato/`): o formato das respostas é comparado com os snapshots em `Contrato/Snapshots/`. Quebrar um campo faz o teste falhar.

Resultado atual: 23 de 23. Evidência em `docs/evidencia-testes/f4-resumo.md`.

### Testes de integração (F5)

Precisam do MySQL do `docker compose` no ar (`docker compose up -d` na pasta `cartao-sus/`) e do `.env` preenchido:

```bash
cd cartao-sus
sh backend/tests/run-integration.sh
```

O script confere se o MySQL está `healthy`, carrega o `.env` e roda os testes com a categoria `Integracao` (IT-01 a IT-10). Cada teste recria o banco `cartao_sus_test` a partir de `schema.sql` e `seed.sql`, usando o usuário root **só** nessa preparação. A API roda com `cartao_app`. O SUS nunca é chamado: o proxy usa uma resposta simulada.

Para rodar só os unitários e de contrato, sem banco: `dotnet test CartaoSus.slnx --filter "Category!=Integracao"`.

Evidência em `docs/evidencia-testes/f5-resumo.md`. A coleção do Postman está em `docs/postman-collection.json` (gerada por `docs/postman/gerar_colecao.py`): 26 requisições em ordem, com 39 testes. Para rodar com o Newman, defina `senhaDemo`, `segredoTotpMaria`, `chaveTerminalUbs` e `chaveTensiometro`: `npx newman run docs/postman-collection.json --env-var senhaDemo=... --env-var segredoTotpMaria=... --env-var chaveTerminalUbs=... --env-var chaveTensiometro=...`. Evidência em `docs/evidencia-testes/f10-newman.txt`.

## Observação sobre dependências

Em 11/11/2026 o `dotnet list package --vulnerable` apontava `MySqlConnector` 2.5.0 (moderada) e `System.Security.Cryptography.Pkcs` 10.0.9 (alta). Os dois foram atualizados para versões corrigidas (2.6.2 e 10.0.15) e o comando já não mostra alertas. O `MySqlConnector` 2.6.2 é mais novo que o esperado pelo Pomelo 9.0.0; os testes unitários, de contrato e de integração passam. Também foram removidos o pacote `Microsoft.Extensions.Identity.Core` (já vem no framework) e o aviso de membro escondido em `ApiKeyAuthHandler`, e o build ficou sem avisos.
