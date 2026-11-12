# O que falta para concluir o trabalho

Situação em 11/11/2026. Prazo de entrega na plataforma: **19/11/2026, até 23h55, sem prorrogação**.

## Já concluído

- MVP completo: banco de dados, API, aplicativo Android, sensores simulados e documentação (fases F0 a F9).
- 76 casos de teste aprovados (unitários, contrato, integração, manuais e do aplicativo), registrados em `docs/evidencia-testes/RELATORIO.md`.
- README final com a ordem completa de execução, verificada com um clone limpo do repositório.
- Documentação de decisões (ADR) em `docs/`.

## Pendências obrigatórias para a entrega

| # | Item | Responsável | Prazo sugerido |
|---|---|---|---|
| 1 | Nome do grupo: GateMinds (concluído) | Grupo | Concluído |
| 2 | Nome e RM dos cinco integrantes (concluído; e-mails opcionais) | Grupo | Concluído |
| 3 | Gravar o vídeo pitch de até 3 minutos, com o protótipo rodando (roteiro em `docs/planos/F10-plano-de-acao.md`) | Grupo | 16/11 |
| 4 | Publicar o vídeo com acesso por link (YouTube não listado ou Google Drive) e testar em aba anônima | Grupo | 17/11 |
| 5 | Confirmar a regra de uso de IA com o professor e preencher a declaração no `README.md` | Grupo | 17/11 |
| 6 | Preencher o link do vídeo no `README.md` e no arquivo de entrega `.txt` | Grupo | 17/11 |
| 7 | Gerar o PDF com as seis partes do edital (DER, API, testes, telas, segurança e IoT) | Grupo | 18/11 |
| 8 | Montar o `.zip` de entrega, sem arquivos ignorados e sem segredos | Grupo | 18/11 |
| 9 | Conferir o checklist do edital (seção 14 do SDD v3) | Grupo | 18/11 |
| 10 | Enviar o `.zip`, o PDF e o `.txt` na plataforma | Grupo | 18/11, sem esperar o último minuto |

## Pendências técnicas (não impedem a entrega)

- **Histórico do git com um único autor.** O edital valoriza commits de todos os integrantes. Cada pessoa do grupo deve fazer commits com o próprio nome, se possível.
- **Coleção do Postman não testada com o Newman.** A coleção tem testes de status, mas a validação foi só de estrutura.
- **Teste do aplicativo só no emulador.** Não houve teste em aparelho físico.
- **Avisos de vulnerabilidade** em `MySqlConnector` e `System.Security.Cryptography.Pkcs`, sem correção publicada nas versões usadas. Registrado no `backend/README.md`.
- **Estoque do BNAFAR** pode mudar com o tempo. Se algum medicamento do catálogo ficar sem estoque, a demonstração mostra a lista vazia.

## Decisões já tomadas que o professor pode questionar

- **TOTP:** o professor confirmou que pode ser usado.
- **"Em SP":** a seleção de unidades e medicamentos usa o município de São Paulo, não o estado.
- **Stack:** ASP.NET Core em vez de Spring, por estar no conteúdo do curso.
- **NFC:** a ideia é compartilhar as informações por NFC, mas, por limitações da simulação, o compartilhamento de documentos usa o seletor do Android depois da validação do código (`docs/adr/ADR-nfc-compartilhamento.md`). NFC real (HCE) fica como trabalho futuro.
- **Segredo TOTP no aparelho:** guardado sem criptografia, com a limitação registrada no SDD.

## Arquivos de entrega

- `ENTREGA-TXT.txt`: nome do grupo, integrantes e link do vídeo. Hoje está com nome do grupo e link pendentes.
- `docs/evidencia-testes/RELATORIO.md`: relatório consolidado de testes.
- PDF das seis partes: ainda não gerado (item 7).
- `.zip` de entrega: ainda não montado (item 8).
