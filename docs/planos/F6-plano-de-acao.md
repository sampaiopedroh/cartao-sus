# Plano de ação — Fase F6 (Simulação de IoT)

## Context

A F3 já entregou a API que os sensores chamam, e a F5 provou que o handshake e o tensiômetro funcionam por integração. Falta o lado do dispositivo: dois scripts Python que simulam sensores reais e disparam as regras da API, mais a explicação que vai para o PDF.

O edital (Parte 6, 2 pontos) exige: 2 ou mais sensores simulados por script (1 pt), dados que disparam algo na aplicação (0,5 pt) e explicação de qual sensor mede o quê e como influencia o sistema (0,5 pt).

O critério do SDD v3 (seção 10, F6): os scripts disparam alertas reais na API, verificáveis pelo E10.

Estimativa do SDD: 3 horas.

## Decisões a confirmar

1. **De onde vem o código TOTP no terminal (recomendado: digitado, com opção de simular o app).** Na vida real, o atendente lê o código que aparece no app do cidadão e o digita no terminal. Por padrão, o script recebe o código por `--token`. Para a demonstração, uma opção `--app` lê o segredo de uma variável de ambiente e gera o código do momento, simulando o app. O segredo nunca fica no código nem no git.
2. **Explicação para o PDF (recomendado: texto em `iot/README.md`).** A seção 9.5 do SDD já tem o texto pronto. Ele vai no README dos sensores, e a F10 copia para o PDF.

## Escopo (passo a passo)

### Passo 1 — Biblioteca comum dos sensores
- `iot/sensor_comum.py`: carregamento do `.env` (quando a variável não está no ambiente), leitura da URL da API, envio com `requests`, timeout de 10 s e impressão padronizada com horário, código HTTP e corpo.
- Não há lógica de negócio aqui. Os sensores só enviam dados.

### Passo 2 — `iot/terminal_nfc.py` (sensor 1)
- Argumentos: `--cns`, `--cnes` (unidade do terminal), `--token` ou `--app`, e `--repeticoes` (padrão 1).
- Usa a chave `IOT_KEY_TERMINAL_UBS` (ou a da UPA, conforme `--cnes`), enviada no header `X-API-KEY`, com `X-Device-Type: TERMINAL_NFC`.
- Chama `POST /api/v1/acolhimento/validar-nfc`.
- Cenário de demonstração de força bruta: `--repeticoes 3` com token errado. A API responde `TOKEN_INVALIDO` três vezes e, na terceira, grava o alerta `TENTATIVA_INDEVIDA`. A quarta tentativa recebe `BLOQUEADO`.
- Sai com código 0 quando a API responde 200, e código 1 caso contrário, para servir de teste automatizado.

### Passo 3 — `iot/tensiometro.py` (sensor 2)
- Argumentos: `--sistolica`, `--diastolica` ou `--cenario`.
- `--cenario demo` envia, em sequência, os três valores do SDD 6: 120/80 (normal), 130/85 (elevada) e 185/115 (crise hipertensiva).
- Usa `IOT_KEY_TENSIOMETRO` no header `X-API-KEY`, com `X-Device-Type: TENSIOMETRO`. O cidadão vem do vínculo do dispositivo, então não é preciso enviar CNS.
- Para cada leitura, mostra se a API disparou o alerta (`alertaDisparado`).

### Passo 4 — `iot/requirements.txt`
- `requests` com versão fixada (versão estável atual).
- Nada além disso. Os scripts usam só a biblioteca padrão, além do `requests`.

### Passo 5 — `iot/README.md` (explicação para o PDF)
- Tabela com cada sensor: o que mede, como se conecta à API, qual regra dispara e o que aparece no app.
- Diagrama do fluxo: sensor → API → regra de negócio → `TB_ALERTA` → app (texto do SDD 9.1).
- Texto de explicação do SDD 9.5, sem alterações.
- Como executar, com os comandos exatos e a ordem: `gerar_chaves.py`, seed dos dispositivos, API no ar, sensores.
- Limitações: REST em vez de MQTT, e sensores simulados por script.

### Passo 6 — Verificação com a API real
- Subir MySQL e API com o `.env` preenchido, e confirmar que os dispositivos existem.
- Rodar `terminal_nfc.py --repeticoes 3` com token errado. Conferir: três `TOKEN_INVALIDO`, uma linha de `TB_ALERTA` do tipo `TENTATIVA_INDEVIDA`, e a quarta tentativa `BLOQUEADO`.
- Rodar `tensiometro.py --cenario demo`. Conferir: 120/80 sem alerta, 130/85 sem alerta, 185/115 com `PRESSAO_CRITICA`.
- Conferir pelo E10, com o login do João, que os alertas aparecem na API.
- Os alertas de teste aparecem no banco da demo. Vale rodar a sequência num banco limpo, para o avaliador ver o efeito do zero.

### Passo 7 — Evidência
- `docs/evidencia-testes/f6-iot.txt`: saída dos dois scripts e a resposta do E10 depois dos disparos.
- Teste automatizado simples dos scripts (opcional, fora do escopo do edital): verificar que `--help` roda sem erro e que um argumento inválido sai com código de erro.

### Passo 8 — Documentação
- README da raiz: marcar F6 como pronta, com link para `iot/README.md`.

## Verificação

1. `python3 iot/terminal_nfc.py --help` e `python3 iot/tensiometro.py --help` rodam sem erro.
2. Sem chave configurada, o script para com mensagem clara, sem traceback.
3. Com a API fora do ar, o script mostra erro de conexão e sai com código diferente de zero.
4. Cenário do terminal: três tentativas erradas, quarta bloqueada, alerta gravado. Conferido no banco e no E10.
5. Cenário do tensiômetro: só a leitura 185/115 dispara alerta. Conferido no banco e no E10.
6. Modo `--app` do terminal: gera um código válido e a API aceita (quando houver janela de tempo e sem reuso).
7. Nenhuma chave aparece no código nem na evidência (`git grep`).

**Critério de pronto da F6:** itens de 1 a 7 passam, `iot/README.md` explica os dois sensores, e `f6-iot.txt` mostra os alertas disparados.

## Riscos

| Risco | Mitigação |
|---|---|
| Código TOTP expirando durante a demonstração | Modo `--app` gera o código no momento do envio; o modo digitado tem ±1 passo de tolerância |
| Reuso do código dentro da mesma janela | O cenário usa um código novo a cada leitura; a repetição do terminal usa códigos errados |
| Chaves vazando no git | Lidas do `.env` (ignorado); nenhuma no código |
| Banco da demo com alertas antigos confundindo a verificação | Sequência de verificação em banco limpo, com contagem antes e depois |
| API de outra porta | `API_URL` configurável, com padrão `http://localhost:8080` |

## Arquivos a criar

- `cartao-sus/iot/sensor_comum.py`
- `cartao-sus/iot/terminal_nfc.py`
- `cartao-sus/iot/tensiometro.py`
- `cartao-sus/iot/requirements.txt`
- `cartao-sus/iot/README.md`
- `cartao-sus/docs/evidencia-testes/f6-iot.txt`

Arquivos existentes que mudam: `README.md` (status da F6). O `iot/gerar_chaves.py` (F3) não muda.

Nenhuma mudança na API é prevista.

## Perguntas em aberto

1. **O código TOTP do terminal é digitado (recomendado), com o modo `--app` como simulação do celular do cidadão?**
2. **O texto de explicação para o PDF fica no `iot/README.md` (recomendado)?**
