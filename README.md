# Cartão SUS Inteligente — Saúde Conectada

Projeto da disciplina **Global Solution 2026/2 — 3ESOA (FIAP)**, tema CitizenTech.

Documento de referência: [`sdd-v3.md`](../sdd-v3.md) (blueprint completo do sistema).

## Estrutura do repositório

| Pasta | Conteúdo | Status |
|---|---|---|
| `database/` | `schema.sql`, `seed.sql`, `consultas.sql`, DER | Pronto (F1) |
| `backend/` | API ASP.NET Core (.NET 10): domínio, JWT, X-API-KEY e testes (unitários, contrato e integração) | Pronto (F2 a F5) |
| `mobile/` | App Android (Kotlin + Jetpack Compose) | TODO (Fase F7) |
| `iot/` | Gerador de chaves (F3); scripts dos sensores simulados (F6) | Parcial |
| `docker/` | Configuração do MySQL em Docker | Pronto |
| `docs/` | Evidências de testes e documentação de API | TODO (Fases F3 e F9) |

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (ou Docker Engine com Compose)
- Git

Outras ferramentas (.NET 8, JDK, Android Studio, Python) serão exigidas nas fases que as usam, e cada uma será listada aqui quando entrar.

## Como executar (F0 — banco de dados)

Todos os comandos rodam dentro da pasta `cartao-sus/`.

**1. Confirme que o Docker está rodando**

```bash
docker info
```

Se o comando falhar, abra o Docker Desktop e espere ele iniciar.

**2. Crie o arquivo de variáveis**

```bash
cp .env.example .env
```

Abra o `.env` e defina `MYSQL_ROOT_PASSWORD` e `MYSQL_APP_PASSWORD` com senhas de sua escolha. Não coloque essa senha em nenhum outro arquivo.

**3. Suba o MySQL**

```bash
docker compose up -d
```

**4. Espere o banco ficar pronto**

```bash
docker compose ps
```

A coluna `STATUS` deve mostrar `healthy`. Pode levar cerca de 30 segundos na primeira vez.

**5. Confira os bancos criados**

```bash
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;"'
```

Devem aparecer `cartao_sus` e `cartao_sus_test`.

**Parar o ambiente** (mantém os dados):

```bash
docker compose down
```

**Recomeçar do zero** (apaga todos os dados do banco):

```bash
docker compose down -v
```

Os scripts em `docker/mysql/init/` só rodam quando o volume é criado. Se você trocar algo nesses scripts, use `docker compose down -v` antes de subir de novo.

### Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop parado | Abra o Docker Desktop e aguarde |
| `port is already allocated` | Porta 3306 em uso | Defina `MYSQL_PORT` no `.env` (ex.: `3307`) e rode `docker compose up -d` de novo |
| `Defina MYSQL_ROOT_PASSWORD` | Senha não preenchida no `.env` | Preencha a variável e rode o comando de novo |
| Status `unhealthy` após 1 minuto | Senha trocada depois da criação do volume | Rode `docker compose down -v` e suba de novo |

## API (F2)

Com o banco pronto, a API sobe com `dotnet run` na pasta `backend/src/CartaoSus.Api`. Antes, carregue o `.env` e preencha `JWT_SECRET` e `SENHA_DEMO`. O passo a passo, as variáveis e os endpoints estão em [`backend/README.md`](backend/README.md).

## Banco de dados (F1)

Com o MySQL da F0 rodando, crie as tabelas e carregue os dados de demonstração. Os comandos e a ordem estão em [`database/README.md`](database/README.md). O diagrama do modelo está em [`database/DER.md`](database/DER.md).

## Segurança

- Nenhum segredo é versionado. Senhas, chaves e segredos ficam no `.env`, que está no `.gitignore`.
- O arquivo `.env.example` tem apenas os nomes das variáveis, sem valores reais.

## Declaração de uso de IA

TODO — preencher com as ferramentas usadas e para quê, conforme a regra da disciplina (ainda não definida pelo professor).

## Vídeo pitch

TODO (Fase F10) — link público do vídeo, testado em aba anônima.
