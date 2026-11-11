# Plano de ação — Fase F0 (Setup do monorepo)

## Context

O SDD v3 (`sdd-v3.md`, seção 10) define a Fase F0 como o alicerce do projeto: monorepo, MySQL 8 em Docker, `.env.example`, `.gitignore` e README skeleton. Critério de aceite: `docker compose up -d` sobe o MySQL e a estrutura de pastas segue a seção 3.3.

A F0 é onde nascem as regras que as fases seguintes herdam: nenhum segredo versionado (seção 8.5), banco de testes separado (seção 6.4) e README que o avaliador consegue seguir (edital: "parte que não roda seguindo o README não pontua"). Errar aqui custa caro depois, então a F0 precisa ser pequena e verificável.

Estimativa do SDD: 2 horas.

## Decisões propostas (a confirmar)

1. **Local do monorepo:** criar em `gs-2026/cartao-sus/` (subpasta). O SDD trata `cartao-sus/` como raiz do `.zip`, e a pasta `gs-2026` continua guardando documentos de apoio (PDF do edital, SDDs, APIs).
2. **Git:** `git init` na F0 e commit só depois da sua aprovação (regra de não commitar sem pedido). Sem remoto por enquanto.

## Escopo da F0 (passo a passo)

### Passo 1 — Estrutura de pastas
Criar a árvore da seção 3.3 do SDD, com `.gitkeep` onde ainda não há conteúdo:

```
cartao-sus/
├── README.md
├── docker-compose.yml
├── .env.example
├── .gitignore
├── docker/mysql/init/01-create-test-db.sql
├── database/            (vazio; schema entra na F1)
├── backend/             (vazio; entra na F2)
├── mobile/              (vazio; entra na F7)
├── iot/                 (vazio; entra na F6)
└── docs/
    └── evidencia-testes/
```

### Passo 2 — `docker-compose.yml`
- Serviço `mysql` com imagem `mysql:8.0` (fixa a major, nunca `latest`).
- Variáveis vindas do `.env`: `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE=cartao_sus`, porta `${MYSQL_PORT:-3306}:3306`.
- Charset do servidor: `utf8mb4` e collation `utf8mb4_unicode_ci` (o `schema.sql` da F1 depende disso).
- Volume nomeado para o dado persistir entre `up` e `down`.
- `healthcheck` com `mysqladmin ping`, para o README mandar esperar o banco ficar `healthy`.
- `restart: unless-stopped`.
- Montar `./docker/mysql/init` em `/docker-entrypoint-initdb.d`. Isso cria o banco `cartao_sus_test` que os testes de integração usam (seção 6.4).

Observação: scripts de `initdb.d` só rodam com volume vazio. O README precisa dizer isso (`docker compose down -v` recria).

### Passo 3 — `docker/mysql/init/01-create-test-db.sql`
- `CREATE DATABASE IF NOT EXISTS cartao_sus_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
- Nada de dados. Os testes criam o próprio conteúdo.

### Passo 4 — `.env.example`
Modelo sem valores reais. Cada variável com comentário em português:

| Variável | Valor no exemplo | Uso |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | vazio | senha do MySQL (obrigatória) |
| `MYSQL_PORT` | `3306` | porta exposta no host |
| `JWT_SECRET` | vazio | chave HS256, mínimo 256 bits (F2) |
| `SENHA_DEMO` | vazio | senha dos usuários demo (F2, seeder) |
| `SEED_DEMO` | `true` | liga o seeder na primeira execução (F2) |
| `API_URL` | `http://localhost:8080` | usada pelos scripts IoT (F6) |
| `CNES_UBS` | `9186492` | código CNES da unidade da demo (já validado) |
| `IOT_KEY_TERMINAL_UBS`, `IOT_KEY_TERMINAL_UPA`, `IOT_KEY_TENSIOMETRO` | vazio | chaves dos dispositivos, geradas por `iot/gerar_chaves.py` (F6) |

Variáveis de F2 e F6 entram já no exemplo para o time não precisar mexer neste arquivo depois. Ficam vazias ou com valor não sensível.

### Passo 5 — `.gitignore`
Regras obrigatórias da seção 8.5 e 10 (regra 6):
- Segredos: `.env`, `database/seed_dispositivos.sql`.
- .NET: `bin/`, `obj/`, `*.user`, `.vs/`, `TestResults/`.
- Android: `build/`, `.gradle/`, `local.properties`, `.idea/`, `*.iml`, `captures/`.
- Python: `__pycache__/`, `venv/`, `.venv/`, `*.pyc`.
- Sistema: `.DS_Store`.

Não ignorar `docs/evidencia-testes/` (evidência vai no entregável).

### Passo 6 — `README.md` (skeleton)
Seções na ordem do edital, cada uma com texto ou `TODO (Fase Fx)`:
1. Visão geral (link para o SDD).
2. Estrutura do repositório.
3. Pré-requisitos: Docker Desktop, Git. (.NET 8, JDK e Android Studio entram nas fases que precisam deles.)
4. **Como executar (F0):** `cp .env.example .env`, preencher `MYSQL_ROOT_PASSWORD`, `docker compose up -d`, aguardar `healthy`, verificar com o comando de checagem do passo 8.
5. Segurança: "nenhum segredo é versionado; valores reais ficam no `.env`".
6. Declaração de uso de IA: placeholder. A regra do edital ainda não foi definida (ver SDD §12.1, item 7).
7. Link do vídeo pitch: placeholder `TODO (F10)`.

### Passo 7 — Git
- `git init` com branch `main`.
- Sem commit até aprovação. Quando aprovar, commit único da F0 com mensagem descritiva e os trailers de atribuição definidos para este projeto.
- Sem remoto. Não há `push`.

### Passo 8 — Verificação
Rodar só depois da implementação, nesta ordem:

1. `docker compose config` valida o YAML sem erro.
2. `cp .env.example .env` e preencher `MYSQL_ROOT_PASSWORD` com um valor de teste local.
3. `docker compose up -d` e `docker compose ps` mostrando o `mysql` como `healthy`.
4. `docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;"` listando `cartao_sus` e `cartao_sus_test`.
5. Conferir charset: `SELECT @@character_set_server, @@collation_server;` retorna `utf8mb4` / `utf8mb4_unicode_ci`.
6. `git check-ignore -v .env` confirma que o `.env` está ignorado.
7. `git grep -n` nas chaves e senhas conhecidas não retorna nada fora do `.env`.
8. `docker compose down` (sem `-v`) preserva o volume; `up -d` de novo mantém os bancos.
9. Simular um clone limpo (outra pasta, só os arquivos versionados): seguir o README do zero. Se algum passo falhar, o README é corrigido antes de fechar a fase.

**Critério de pronto da F0:** os passos 1 a 9 passam, o README foi seguido do zero e o `.env` não aparece em nenhum arquivo versionado.

## Riscos desta fase

| Risco | Mitigação |
|---|---|
| Porta 3306 já ocupada na máquina | `MYSQL_PORT` configurável no `.env` |
| Volume antigo sem o `cartao_sus_test` | README explica `docker compose down -v` |
| Senha do root vazando no `docker-compose.yml` | Só `${VAR}` no compose, nenhum valor literal |
| Docker Desktop parado | Passo 3 do README inclui checar `docker info` |

## Arquivos a criar (todos novos)

- `cartao-sus/README.md`
- `cartao-sus/docker-compose.yml`
- `cartao-sus/.env.example`
- `cartao-sus/.gitignore`
- `cartao-sus/docker/mysql/init/01-create-test-db.sql`
- `.gitkeep` nas pastas vazias (`database/`, `backend/`, `mobile/`, `iot/`, `docs/evidencia-testes/`)

Nada existente é modificado. Os documentos em `gs-2026/` (SDDs, PDF, APIs) continuam como estão.

## Verificação end-to-end

Resumida no passo 8. O teste principal é o clone limpo seguindo só o README, porque é isso que o avaliador vai fazer.
