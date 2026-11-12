# Revisão de UX do app (acessibilidade e navegação)

- Tema com fonte maior (corpo 18sp), alto contraste, botões de 56dp, rótulos em português simples.
- Barra inferior: Início, Atendimento, Consultar, Saúde. Cada aba tem ícone e texto.
- Correção: o botão "Início" agora volta à tela inicial de qualquer aba (NavHost interno com Início como destino inicial).
- Verificado no emulador: tocar em Início a partir de Saúde, Consultar e Atendimento mostra a tela inicial (capturas idênticas); o botão Voltar do sistema, estando em Saúde, também volta ao Início.
- Sair da conta fica no topo, com confirmação.
- 46 testes unitários Android passando.

Capturas: ux-login.png, ux-inicio.png, ux-atendimento.png, ux-consultar.png, ux-saude.png.

## Meus documentos
Atalho "Meus documentos" na tela inicial. O seletor do Android (sem permissão ampla de armazenamento) devolve o arquivo; o app guarda a permissão de leitura persistente e salva só o caminho, tipo e título no celular. Verificado no emulador: anexar, sobreviver ao reinício do app, abrir no visualizador e aviso quando o arquivo é apagado (ux-documentos*.png).
