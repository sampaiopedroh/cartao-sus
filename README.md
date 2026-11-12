# Cartão SUS Inteligente — Saúde Conectada

Projeto da disciplina **Global Solution 2026/2 — 3ESOA (FIAP)**, tema CitizenTech. Grupo **GateMinds**.

Um cartão digital do cidadão para o SUS: consulta de disponibilidade de equipamentos e remédios, carteira de documentos e sinais vitais no aparelho, acolhimento presencial com código TOTP e alertas de saúde gerados por sensores simulados.

Documento de referência: [`sdd-v3.md`](docs/sdd/sdd-v3.md) (blueprint completo). Plano de cada fase: arquivos `F*-plano-de-acao.md` na pasta `gs-2026/`.

## Estrutura

| Pasta | Conteúdo | Leia mais |
|---|---|---|
| `database/` | Schema, carga inicial, 7 consultas e DER | [database/README.md](database/README.md) |
| `backend/` | API ASP.NET Core (.NET 10) com testes unitários, de contrato e de integração | [backend/README.md](backend/README.md) |
| `mobile/` | App Android (Kotlin, Jetpack Compose) | [mobile/README.md](mobile/README.md) |
| `iot/` | Sensores simulados (terminal NFC e tensiômetro) e gerador de chaves | [iot/README.md](iot/README.md) |
| `docker/` | MySQL 8 em Docker | — |
| `docs/` | Evidências dos testes, relatório consolidado, coleção Postman e Swagger | [docs/evidencia-testes/README.md](docs/evidencia-testes/README.md) |

## Pré-requisitos

- **Docker** (Docker Desktop ou Docker Engine com Compose): banco de dados.
- **.NET SDK 10** e **Python 3.11+**: API, testes e sensores.
- **JDK 17+ e Android SDK (plataforma 36)**, ou o Android Studio: aplicativo. Opcional se você só quiser rodar a API.

## Como executar (ordem completa)

Todos os comandos rodam na pasta `cartao-sus/`, salvo quando indicado.

**1. Crie o arquivo de variáveis**

```bash
cp .env.example .env
```

Preencha no `.env`: `MYSQL_ROOT_PASSWORD`, `MYSQL_APP_PASSWORD`, `JWT_SECRET` (32 caracteres ou mais) e `SENHA_DEMO` (8 caracteres ou mais). Os valores são seus, não vão para o git.

**2. Suba o banco**

```bash
docker compose up -d
```

Espere a coluna `STATUS` mostrar `healthy` em `docker compose ps` (cerca de 30 segundos).

**3. Crie as tabelas e os dados de demonstração**

```bash
docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/schema.sql
docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/seed.sql
```

**4. Gere as chaves dos sensores e cadastre os dispositivos**

```bash
python3 iot/gerar_chaves.py
docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/seed_dispositivos.sql
```

**5. Rode a API**

```bash
set -a && . ./.env && set +a
cd backend/src/CartaoSus.Api
dotnet run
```

Na primeira execução, o console mostra os **segredos TOTP** dos cidadãos de demonstração. Guarde-os: são necessários para o código do aplicativo (ver o README do mobile). A API responde em `http://localhost:8080`, e o Swagger em `http://localhost:8080/swagger`.

**6. Rode os testes** (em outro terminal, sem precisar do banco para os dois primeiros)

```bash
cd backend
dotnet test CartaoSus.slnx --filter "Category!=Integracao"   # unitários e de contrato
sh tests/run-integration.sh                                    # integração (precisa do MySQL no ar)
```

**7. Rode os sensores** (com a API no ar)

```bash
python3 -m venv iot/.venv && iot/.venv/bin/pip install -r iot/requirements.txt
iot/.venv/bin/python iot/tensiometro.py --cenario demo
```

**8. Rode o aplicativo** (opcional): abra a pasta `mobile/` no Android Studio ou rode `./gradlew installDebug`, com um emulador iniciado. Detalhes em [mobile/README.md](mobile/README.md).

**Parar o ambiente** (mantém os dados): `docker compose down`. **Recomeçar do zero** (apaga o banco): `docker compose down -v`, e repita os passos 2 a 4.

### Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker parado | Abra o Docker Desktop |
| `port is already allocated` | Porta 3306 em uso | Defina `MYSQL_PORT` no `.env` e suba de novo |
| `JWT_SECRET não definida` ao rodar a API | Variáveis não carregadas | Rode o `set -a && . ./.env && set +a` antes do `dotnet run` |
| Código TOTP recusado no terminal | Relógio desalinhado ou código já usado | Gere um código novo; o código vale por 30 segundos |
| Aplicativo não conecta | API fora do ar ou emulador sem `10.0.2.2` | Suba a API; use um emulador Android |

## Segurança

- Nenhum segredo é versionado: senhas, chaves e segredos ficam no `.env` e nos arquivos gerados, todos no `.gitignore`.
- O `.env.example` traz apenas os nomes das variáveis.
- O segredo TOTP no aparelho fica sem criptografia; a limitação está registrada no SDD (seção 7.4).

## Declaração de uso de IA

*Em preparação. Será preenchida com as ferramentas usadas e para quê, após confirmação do grupo.*

## Vídeo pitch

*Em preparação. O link público será incluído aqui e no arquivo `.txt` de entrega.*

## Resultados dos testes

Consolidados em [docs/evidencia-testes/RELATORIO.md](docs/evidencia-testes/RELATORIO.md): 76 casos aprovados (12 unitários, 8 de contrato, 12 de integração, 14 manuais e 30 do aplicativo).
