# Roteiro do vídeo pitch (até 3 minutos)

Regras do edital: **até 3 minutos**, mostrar o **protótipo funcionando** (não slides), link **público**, testado em **aba anônima** antes de enviar.

## Antes de gravar (checklist)

- [ ] Docker no ar e banco `healthy` (`docker compose ps`).
- [ ] API rodando (`http://localhost:8080/swagger` abre).
- [ ] Emulador com o app instalado e logado na conta da Maria.
- [ ] Segredo TOTP da Maria salvo no app (aba Atendimento mostra o código de 6 dígitos).
- [ ] No celular: dois arquivos de exemplo anexados em "Meus documentos" (por exemplo RG e receita), sem dados reais de ninguém.
- [ ] Dois terminais abertos: um para o terminal do balcão (`terminal_nfc.py`) e outro para o tensiômetro (`tensiometro.py`), com a fonte grande o bastante para ler.
- [ ] Notificações do computador desligadas. Gravação de tela em resolução 1080p ou mais.
- [ ] Ensaiar uma vez completo, cronometrando.

Comandos prontos (na pasta `cartao-sus/`):

```bash
# balcão: valida o código que está na tela do app
SEGREDO_TOTP=<segredo da Maria> iot/.venv/bin/python iot/terminal_nfc.py --cns 898000000000000 --cnes 8530378 --app

# tensiômetro: pressão crítica
iot/.venv/bin/python iot/tensiometro.py --cenario demo
```

## Roteiro com tempos

| Tempo | Cena | O que mostrar | O que dizer (ideia) |
|---|---|---|---|
| 0:00 a 0:30 | **Problema** | Aba Consultar sem resultado, ou imagem de fila | "Quem usa o SUS perde tempo: não sabe se tem o remédio, leva papéis na mão e não tem como mostrar o que precisa com segurança." |
| 0:30 a 1:00 | **Solução** | Tela Início: cartão digital e atalhos | "O Cartão SUS Inteligente reúne o cartão do cidadão, o código de atendimento, os documentos e os sinais de saúde no celular." |
| 1:00 a 2:00 | **Demonstração** | 1) Login. 2) Aba Atendimento com o código. 3) Terminal do balcão responde `AUTORIZADO`. 4) Aparece "Compartilhar documentos": escolher um documento e compartilhar. 5) Tensiômetro envia pressão crítica; mostrar o aviso no app | "O atendente só vê os dados com o código do cidadão. Depois da validação, o cidadão escolhe quais documentos mostrar. E um sensor de pressão gera um alerta crítico que aparece no app." |
| 2:00 a 2:30 | **Segurança** | Tela Meus documentos (arquivos ficam no celular); terminal com 3 códigos errados e `BLOQUEADO` | "Os documentos ficam só no aparelho. Sem o código, nada é liberado, e três tentativas erradas bloqueiam e geram alerta. O toque por NFC é simulado neste protótipo." |
| 2:30 a 3:00 | **Impacto** | Relatório de testes ou slide final simples | "100 testes aprovados, aderente aos ODS 3, 10 e 11. Próximo passo: NFC real e criptografia do segredo no aparelho." |

## Depois de gravar

- [ ] Duração de 3:00 ou menos.
- [ ] Publicar como **não listado** (YouTube) ou com acesso por link (Drive).
- [ ] Abrir o link em **aba anônima** e conferir áudio e imagem.
- [ ] Colar o link em `ENTREGA-TXT.txt` e no `README.md` (seção Vídeo pitch).
