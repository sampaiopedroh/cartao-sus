# O que falta para concluir o trabalho

Situação em 11/11/2026. Prazo de entrega na plataforma: **19/11/2026, até 23h55, sem prorrogação**.

## Já concluído

- MVP completo: banco de dados, API, aplicativo Android, sensores simulados e documentação (fases F0 a F9).
- 100 casos de teste aprovados (12 unitários, 8 de contrato, 12 de integração, 18 manuais e 50 do aplicativo), registrados em `docs/evidencia-testes/RELATORIO.md`.
- README final com a ordem completa de execução, verificada com um clone limpo do repositório.
- Documentação de decisões (ADR) em `docs/`.
- Revisão de UX do app (abas Início, Atendimento, Consultar e Saúde, fontes maiores, botão Início corrigido), tela **Meus documentos** e compartilhamento de documentos depois da validação do código.
- Coleção do Postman reescrita como fluxo executável (26 requisições, 39 testes) e executada no Newman sem falhas, duas vezes seguidas (`docs/evidencia-testes/f10-newman.txt`).
- Vulnerabilidades de dependências corrigidas (`MySqlConnector` 2.6.2 e `Pkcs` 10.0.15), build sem avisos e testes verdes.
- Compartilhamento testado também com PDF e imagem juntos (`ux-compartilhar-pdf-e-imagem.png`).
- Teste de clone limpo feito em 11/11/2026 (banco, API, testes, sensores e app), em `docs/evidencia-testes/f10-clone-limpo.txt`.
- Declaração de uso de IA escrita no `README.md` (falta a confirmação do grupo).
- PDF com as seis partes (`docs/entrega/GateMinds-Cartao-SUS-Entrega.pdf`, 20 páginas), roteiro do vídeo (`docs/roteiro-pitch.md`) e checklist do edital (`docs/entrega/checklist-edital.md`).
- `.zip` de entrega gerado por `sh docs/entrega/montar_zip.sh` (sem segredos), em `gs-2026/GateMinds-Cartao-SUS-GS2026.zip`.

## Pendências obrigatórias para a entrega

| # | Item | Responsável | Prazo sugerido |
|---|---|---|---|
| 1 | Nome do grupo: GateMinds (concluído) | Grupo | Concluído |
| 2 | Nome e RM dos cinco integrantes (concluído; e-mails opcionais) | Grupo | Concluído |
| 3 | Gravar o vídeo pitch de até 3 minutos, com o protótipo rodando (roteiro e checklist em `docs/roteiro-pitch.md`) | Grupo | 16/11 |
| 4 | Publicar o vídeo com acesso por link (YouTube não listado ou Google Drive) e testar em aba anônima | Grupo | 17/11 |
| 5 | Confirmar a declaração de IA já escrita no `README.md` e a regra da disciplina com o professor | Grupo | 17/11 |
| 6 | Preencher o link do vídeo no `README.md` e no `ENTREGA-TXT.txt` | Grupo | 17/11 |
| 7 | Gerar o PDF final de novo, com o link do vídeo (`python3 docs/entrega/gerar_entrega.py`, precisa do pacote Python `markdown`) | Grupo | 18/11 |
| 8 | Fazer commit e gerar o `.zip` final (`sh docs/entrega/montar_zip.sh`, só leva o que está no git) | Grupo | 18/11 |
| 9 | Conferir o checklist do edital (seção 14 do SDD v3) | Grupo | 18/11 |
| 10 | Enviar o `.zip`, o PDF e o `.txt` na plataforma | Grupo | 18/11, sem esperar o último minuto |

## Pendências técnicas (não impedem a entrega)

- **Histórico do git com um único autor.** O edital valoriza commits de todos os integrantes. Cada pessoa do grupo deve fazer commits com o próprio nome, se possível.
- **Teste do aplicativo só no emulador.** Não houve teste em aparelho físico (o compartilhamento já foi testado no emulador com imagem e PDF).
- **Compartilhamento liberado pela conta, não pelo aparelho:** limitação aceita. Explicação, motivo de não mudar agora e caminho futuro em `docs/adr/ADR-limitacoes-aceitas.md`.
- **Teste de clone limpo no Android** exige o `mobile/local.properties` com o caminho do SDK (já descrito no README).
- **Estoque do BNAFAR muda com o tempo:** é o comportamento esperado (dados reais do governo). Explicação em `docs/adr/ADR-limitacoes-aceitas.md`. Na gravação do vídeo, escolha na hora um remédio com estoque.

## Decisões já tomadas que o professor pode questionar

- **TOTP:** o professor confirmou que pode ser usado.
- **"Em SP":** a seleção de unidades e medicamentos usa o município de São Paulo, não o estado.
- **Stack:** ASP.NET Core em vez de Spring, por estar no conteúdo do curso.
- **NFC:** a ideia é compartilhar as informações por NFC, mas, por limitações da simulação, o compartilhamento de documentos usa o seletor do Android depois da validação do código (`docs/adr/ADR-nfc-compartilhamento.md`). NFC real (HCE) fica como trabalho futuro.
- **Segredo TOTP no aparelho:** guardado sem criptografia, com a limitação registrada no SDD.

## Arquivos de entrega

- `ENTREGA-TXT.txt`: nome do grupo e integrantes prontos; falta o link do vídeo.
- `docs/evidencia-testes/RELATORIO.md`: relatório consolidado de testes.
- `docs/entrega/GateMinds-Cartao-SUS-Entrega.pdf`: pronto, com o link do vídeo ainda pendente.
- `gs-2026/GateMinds-Cartao-SUS-GS2026.zip`: gerado, mas precisa ser refeito depois que o link do vídeo entrar (item 8).
