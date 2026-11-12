# Acessibilidade para pessoas com deficiência visual

Revisão feita em 12/11/2026 em todas as telas do aplicativo Android: login, cadastro, início, código de atendimento, consulta de remédios e unidades, sinais de saúde, meus documentos e compartilhamento.

O objetivo é que o app funcione para quem **não enxerga** (usa o leitor de tela **TalkBack**), para quem tem **baixa visão** (usa letra grande no celular) e para quem **não distingue cores** (daltonismo).

## O que foi feito

### Leitor de tela (TalkBack)

| Melhoria | Onde | Por que |
|---|---|---|
| **Nome de cada tela** anunciado ao trocar de aba ou de tela | Todas | A pessoa sabe onde está sem precisar explorar a tela |
| **Títulos navegáveis** (cabeçalhos) | Títulos das telas e das seções ("O que você quer fazer?", "Avisos", "Últimos atendimentos", "Seu histórico" etc.) | O TalkBack permite pular de título em título, como num índice |
| **Números lidos dígito por dígito** | Código de atendimento ("2 0 7 4 1 1"), número do cartão SUS (em grupos: "8 9 8, 0 0 0 0, ..."), código secreto do cadastro (em blocos de 4 letras) | Sem isso o leitor fala "duzentos e sete mil..." e a pessoa não consegue repetir o código no balcão |
| **Cartões lidos de uma vez** | Cartão digital, atalhos, avisos, histórico de atendimentos, medições, estoque, unidades, documentos | Uma frase completa, em vez de vários pedaços soltos |
| **Avisos falam o tipo** ("Importante:", "Atenção:", "Tudo certo:", "Informação:") | Todos os avisos | A cor vermelha ou verde não chega a quem não enxerga |
| **Anúncio automático** de mudanças importantes | Erros (login, rede), "O atendente validou o seu código", "Código copiado", carregamento, código que trocou | A pessoa é avisada sem ter que procurar a mudança na tela |
| **Botões com contexto** | "Como chegar a UBS X", "Ver equipamentos de UBS X", "Abrir RG", "Editar RG", "Remover RG" | Em listas, vários botões "Como chegar" iguais não dizem a qual unidade se referem |
| **Gráfico descrito em texto** | Sinais de saúde | Ex.: "Gráfico de pressão máxima com 5 medições. Mais recente 124 milímetros de mercúrio. Menor 118, maior 185. Tendência: descendo." |
| **Unidades por extenso** | Medições | "124 por 80 milímetros de mercúrio", e não "124 barra 80 m m H g" |
| **Barra de tempo escondida do leitor** | Código de atendimento | O tempo restante já é falado pelo texto "Muda em X segundos"; a barra só repetiria "60 por cento" |
| **Campos com erro anunciados** | Login, cadastro, sinais | O TalkBack fala o erro do campo junto com o nome dele |
| **Caixas de seleção acessíveis** | Escolha dos documentos para compartilhar | A linha inteira é uma caixa de seleção: o leitor fala "marcada" ou "não marcada" |
| **Atalhos dizem a ação** | Início | "Meu código de atendimento, botão, toque duas vezes para abrir" |

### Baixa visão (letra grande)

- Todo o texto usa a unidade que acompanha a configuração de **tamanho da fonte** do Android.
- Testado com a fonte do sistema em **200%** (o máximo do Android):
  - os atalhos do Início passam para **uma coluna**, sem quebrar palavras no meio;
  - os botões das unidades ficam **um embaixo do outro**;
  - os cartões crescem em altura (antes tinham altura fixa e cortavam o texto);
  - a barra inferior (4 abas) e o título da barra superior têm altura fixa: o rótulo das abas fica no tamanho padrão e o título cresce até 1,2x, terminando em "..." se não couber. O nome completo continua sendo falado pelo TalkBack.
- Botões com no mínimo **56 dp** de altura (o recomendado é 48 dp).

### Contraste e cores

- **Contraste de 4,5:1 ou mais** (WCAG 2.1, nível AA) em todos os pares de texto e fundo, nos temas **claro e escuro**. Isso é verificado por teste automático (`ContrasteTest`), então uma mudança de cor que piore o contraste quebra o build de testes.
- **Nada depende só da cor:** avisos têm ícone e texto; o gráfico de pressão usa linha **contínua** para a máxima e **tracejada** para a mínima, com a legenda dizendo qual é qual.
- O app segue o **modo escuro** do celular.

### Correção encontrada na revisão

- O botão "Como chegar" fechava o app em aparelhos sem aplicativo de mapas. Agora mostra "Nenhum aplicativo de mapas instalado."

## Como foi verificado

| Verificação | Resultado |
|---|---|
| Testes automáticos dos textos falados (`FalaTest`, 7 testes) | Passam |
| Testes automáticos de contraste nos temas claro e escuro (`ContrasteTest`, 2 testes) | Passam |
| Árvore de acessibilidade lida no emulador (`uiautomator`) | Descrições presentes (ex.: cartão "Cartão SUS de Maria Souza Lima. Número do cartão: 8 9 8, 0 0 0 0, ...") |
| Fonte do sistema em 200% no emulador | Atalhos, botões, barra inferior e título sem corte (capturas `a11y-fonte-200-*.png`) |
| TalkBack ligado no emulador | Liga e foca o título da tela ao abrir. **Limitação:** os gestos do TalkBack (deslizar para o próximo item) não podem ser simulados por linha de comando, então a navegação item a item não foi automatizada |

## Como testar com o TalkBack (recomendado antes da entrega)

1. No celular ou emulador: **Configurações > Acessibilidade > TalkBack > Ativar**.
2. Abra o app. Deslize **para a direita** para ir ao próximo item e **para a esquerda** para voltar. **Toque duas vezes** para ativar.
3. Roteiro sugerido:
   - Login: os campos falam o nome; com senha errada, o erro é anunciado.
   - Início: o cartão é lido com o número em grupos de dígitos; os atalhos dizem "toque duas vezes para abrir".
   - Atendimento: o código é lido dígito por dígito; ao trocar, é anunciado.
   - Saúde: registre uma medição e ouça o resultado; o gráfico é lido como resumo.
   - Documentos: os botões dizem o nome do documento.
4. Para desligar: aperte as **duas teclas de volume por 3 segundos** (se o atalho estiver ativo) ou volte em Configurações.

## Limites conhecidos

- A navegação completa com TalkBack foi revisada no código e na árvore de acessibilidade, mas não percorrida por uma pessoa que usa leitor de tela no dia a dia. O ideal é um teste com usuários reais.
- Os textos estão só em português. O TalkBack usa a voz do idioma configurado no celular.
- O compartilhamento abre a tela de compartilhamento do Android, que tem a acessibilidade do próprio sistema.
