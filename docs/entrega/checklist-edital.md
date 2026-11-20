# Checklist final da entrega (SDD seção 14), atualizado em 19/11/2026

| Item | Estado | Como foi conferido |
|---|---|---|
| `.zip` com README (execução, declaração de IA, link do pitch), `backend/`, `mobile/`, `iot/`, `database/`, `docs/` | Pronto, com o link do vídeo | `sh docs/entrega/montar_zip.sh` gera o zip a partir do git; conteúdo listado e conferido |
| PDF com as 6 partes (DER, endpoints, plano de testes e evidências, telas, segurança, IoT) | Pronto | `docs/entrega/GateMinds-Cartao-SUS-Entrega.pdf` (20 páginas), gerado por `gerar_entrega.py` |
| `.txt` com nome do grupo, RM e nome de cada integrante e link público do vídeo | Pronto | `ENTREGA-TXT.txt` tem grupo, os cinco integrantes e o link do vídeo |
| `dotnet test` verde | Pronto | Clone limpo: 23 testes unitários e de contrato e 12 de integração, sem falhas (`docs/evidencia-testes/f10-clone-limpo.txt`) |
| App compilando e testes do Android | Pronto | Clone limpo: APK gerado e 50 testes, sem falhas (`f10-android-testes.txt`) |
| Scripts IoT executando | Pronto | Clone limpo: tensiômetro devolveu HTTP 201 com alerta crítico |
| Nenhum segredo versionado | Pronto | `.env`, chaves e hashes de dispositivo estão no `.gitignore`; varredura do zip contra os valores reais do `.env` e dos segredos TOTP demo não achou nada. O único achado são constantes falsas de teste em `IntegracaoInfra.cs`, que são públicas de propósito |
| Vídeo pitch de até 3 minutos, link público testado em aba anônima | Link inserido e testado em aba anônima | Roteiro em `docs/roteiro-pitch.md` |
| Declaração de uso de IA | Confirmada pelo grupo | Seção no `README.md` |

## Para fechar depois que o vídeo existir

1. Colocar o link no `ENTREGA-TXT.txt` e no `README.md` (seção Vídeo pitch) e em `ENTREGA-PDF` (rodar `gerar_entrega.py` de novo).
2. Fazer commit e rodar `sh docs/entrega/montar_zip.sh` para gerar o zip final em `gs-2026/`.
3. Enviar o `.zip` na plataforma até 19/11/2026 às 23h55.
