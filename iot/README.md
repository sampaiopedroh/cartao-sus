# Simulação de IoT — Cartão SUS Inteligente

Dois sensores simulados em Python. Cada um envia dados à API, e a API dispara uma regra de negócio que aparece no app do cidadão.

## Sensores

| Sensor | O que mede | Como se conecta à API | Regra que dispara | O que aparece no app |
|---|---|---|---|---|
| **Terminal NFC** (`terminal_nfc.py`) | Aproximação do cartão do cidadão no balcão (CNS) e o código TOTP digitado pelo atendente | `POST /api/v1/acolhimento/validar-nfc` com `X-API-KEY` do terminal | Três tokens inválidos em 10 minutos bloqueiam o CNS por 10 minutos e geram o alerta `TENTATIVA_INDEVIDA` | Alerta de segurança e histórico de acessos |
| **Tensiômetro** (`tensiometro.py`) | Pressão arterial: sístole e diástole, em mmHg | `POST /api/v1/telemetria/sinais-vitais` com `X-API-KEY` do tensiômetro | Sístole acima de 180 ou diástole acima de 110 geram o alerta `PRESSAO_CRITICA` | Banner "Alerta de Pressão Crítica: Procure a UPA mais próxima" |

## Como o sistema é influenciado

O **Terminal NFC** mede a presença física do cartão do cidadão no balcão e envia à API o par (CNS + código TOTP). Ele influencia o sistema ao orquestrar o consentimento: sem o código válido do app do cidadão, nenhum dado clínico é liberado, e tentativas seguidas acionam bloqueio de segurança.

O **Tensiômetro** mede a pressão arterial em casa e influencia o sistema ao transformar uma leitura isolada em decisão: se a pressão ultrapassa o limiar de crise, a API gera um alerta crítico imediato, e o app orienta o cidadão à UPA mais próxima, evitando deslocamentos inúteis ou atraso no atendimento.

Fluxo: sensor → API → regra de negócio → `TB_ALERTA` → app (consulta `GET /api/v1/cidadao/{id}/alertas`).

## Pré-requisitos

- Python 3.11 ou superior
- Banco e API no ar (ver o [README do backend](../backend/README.md))
- `.env` preenchido na pasta `cartao-sus/`, com as chaves dos dispositivos

## Como executar

Todos os comandos rodam na pasta `cartao-sus/`.

**1. Instale a dependência**

```bash
python3 -m venv iot/.venv
iot/.venv/bin/pip install -r iot/requirements.txt
```

**2. Gere as chaves dos dispositivos** (se ainda não existirem)

```bash
python3 iot/gerar_chaves.py
```

**3. Cadastre os dispositivos no banco** (uma vez, com o banco no ar)

```bash
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/seed_dispositivos.sql
```

**4. Rode o tensiômetro: cenário de demonstração**

```bash
iot/.venv/bin/python iot/tensiometro.py --cenario demo
```

Três leituras: 120/80 (normal), 130/85 (elevada) e 185/115 (crise). Só a última dispara o alerta.

**5. Rode o terminal: tentativas com código errado**

```bash
iot/.venv/bin/python iot/terminal_nfc.py --cns 182345678901234 --cnes 8530378 --token 000000 --repeticoes 3
```

As três tentativas são recusadas. Na terceira, o alerta de tentativa indevida é gravado.

**6. Rode o terminal: quarta tentativa com o código do app**

Com o segredo TOTP do cidadão no ambiente, o script gera o código do momento (simulando o app do cidadão):

```bash
SEGREDO_TOTP=<segredo do cidadão> iot/.venv/bin/python iot/terminal_nfc.py --cns 182345678901234 --cnes 8530378 --app
```

Resposta esperada: `BLOQUEADO`. O segredo é impresso no console da API ao iniciar com `SEED_DEMO=true` e não é gravado em arquivo.

### Opções

| Script | Opção | Efeito |
|---|---|---|
| `terminal_nfc.py` | `--token CODIGO` | Código digitado pelo atendente |
| `terminal_nfc.py` | `--app` | Gera o código do momento a partir de `SEGREDO_TOTP` |
| `terminal_nfc.py` | `--repeticoes N` | Repete a tentativa (use 3 para força bruta) |
| `tensiometro.py` | `--cenario demo` | Roteiro com 120/80, 130/85 e 185/115 |
| `tensiometro.py` | `--sistolica X --diastolica Y` | Uma leitura manual |

Códigos de saída: `0` se a API aceitou tudo, `1` se alguma leitura foi recusada, `2` se a API não respondeu.

Variáveis: `API_URL` (padrão `http://localhost:8080`), `IOT_KEY_TERMINAL_UBS`, `IOT_KEY_TERMINAL_UPA`, `IOT_KEY_TENSIOMETRO`. Os scripts leem essas variáveis do `.env` quando elas não estão no ambiente.

## Limitações

- **Simulação por REST, não por MQTT.** O edital aceita script, JSON ou MQTT simulado. Optamos por REST para usar o mesmo contrato da API.
- **Sem sensor físico.** O cartão é representado pelo CNS digitado, e a leitura NFC é simulada.
- **Um alerta por leitura de pressão.** Sístole e diástole geram cada um o seu registro. Quando as duas passam do limiar, aparecem dois alertas para a mesma medição.
- **Código TOTP no modo `--app`.** A geração usa o relógio da máquina onde o script roda.

## Teste: código de atendimento e compartilhamento de documentos

1. App no emulador, conta da Maria, aba **Atendimento** aberta e com o código secreto salvo.
2. No terminal (com a API no ar):

```bash
SEGREDO_TOTP=<segredo da Maria> iot/.venv/bin/python iot/terminal_nfc.py --cns 898000000000000 --cnes 8530378 --app
```

3. A resposta é `AUTORIZADO`. Em até 5 segundos o app mostra "O atendente validou o seu código" e o botão **Compartilhar documentos**.
4. Escolha os documentos e toque em Compartilhar: o app abre o compartilhamento do Android só com os arquivos marcados. A autorização vale para um envio; para outro é preciso um novo código validado.
5. Com código errado (`--token 000000`) o botão não aparece.

Os arquivos nunca passam pela API: só o app e o Android os enviam.
