# ADR: compartilhamento de documentos sem NFC real

**Status:** adotada (Plano F10, sessão de UX do app)

## Contexto

O Cartão SUS Inteligente foi pensado para que o cidadão aproxime o celular do terminal no balcão e compartilhe, por **NFC**, as informações e os documentos que o atendente precisa ver, depois de o atendente validar o código TOTP.

No projeto, o NFC é **simulado**: o script `iot/terminal_nfc.py` envia o CNS e o código para a API (`POST /api/v1/acolhimento/validar-nfc`). Não há leitor NFC físico, e o emulador Android não simula troca de dados por NFC (HCE).

## Decisão

A ideia continua sendo usar NFC para compartilhar as informações. Por limitação da simulação, **optamos por uma alternativa**: depois que o atendente valida o código, o app libera o botão "Compartilhar documentos". O cidadão escolhe os documentos e o app abre o compartilhamento do Android com só os arquivos marcados.

- O caminho dos arquivos fica só no celular (nada é enviado à API).
- A liberação vale para um envio; outro envio exige novo código validado.
- A validação é detectada pelo histórico de atendimentos da conta (atendimento `AUTORIZADO` novo).

## Consequências

- Funciona e é testável no emulador e em qualquer celular, sem hardware NFC.
- O atendente recebe os arquivos pelo app que o celular oferecer (e-mail, mensagem, Quick Share), e não por toque.
- Se outro aparelho gerar um atendimento da mesma conta, o botão também aparece.

## Caminho para NFC real (trabalho futuro)

Implementar o celular como cartão (Host Card Emulation, HCE) e o leitor no terminal, trocando o seletor do Android pela troca de dados por NFC. A regra de liberação (código validado) continua a mesma.
