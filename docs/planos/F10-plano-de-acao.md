# Plano de ação — Fase F10 (Entrega final), versão de 11/11/2026

## Context

As fases F0 a F9 estão prontas e no GitHub (`sampaiopedroh/cartao-sus`, branch `main`): banco, API, testes, app Android, sensores, evidências e relatório. Depois da F9 entraram duas melhorias no app, que mudam o que vai no PDF e no vídeo:

- **Redesenho de UX** (letras maiores, abas Início, Atendimento, Consultar e Saúde, botão Início que volta à tela inicial).
- **Meus documentos** e **compartilhamento depois da validação do código**, com a decisão registrada em `docs/adr/ADR-nfc-compartilhamento.md` (a ideia é NFC; por limitação da simulação, usamos o seletor do Android).

Falta montar o pacote que o professor recebe na plataforma, até **19/11/2026 às 23h55**: um `.zip` do projeto com README de execução, um PDF com os entregáveis das seis partes e um `.txt` com nome do grupo, RM de cada integrante e o link do vídeo pitch. Hoje é 11/11, então há oito dias. Meta interna: tudo pronto até **17/11**, com a margem de dois dias para imprevistos.

## O que já está pronto

| Item | Estado |
|---|---|
| Nome do grupo e integrantes com RM | Preenchidos em `ENTREGA-TXT.txt` |
| Código, testes e evidências (UT, CT, IT, MT) | Prontos, em `docs/evidencia-testes/` |
| Capturas do app novo | `ux-*.png` e `ux-resumo.md` |
| Decisões e limitações | `docs/adr/`, `docs/PENDENCIAS.md`, seção de limitações do `RELATORIO.md` |
| Link do vídeo, declaração de IA, PDF, `.zip` | **Faltam** |

## Decisões e informações que preciso de você

1. **Declaração de uso de IA (recomendado: texto baseado no uso real).** Escrevo com as ferramentas efetivamente usadas (Claude para código, testes e documentação; Gemini para revisão do SDD) e com a revisão humana do grupo. Você confirma se o grupo concorda com o texto. Se a disciplina tiver regra própria, confirmo com o professor antes de enviar.
2. **Vídeo pitch: quem grava e onde publica (recomendado: o grupo grava com o protótipo rodando; YouTube como "não listado", ou Google Drive com acesso por link).** Eu preparo o roteiro e a lista de cenas; a gravação e o link são do grupo.
3. **Formato do PDF (recomendado: um único PDF).** Gerado a partir de um Markdown consolidado, com as capturas.
4. **Commits dos outros integrantes.** O histórico tem um único autor (22 commits). O edital valoriza commits de todos. Cada integrante pode fazer um commit pequeno e real (por exemplo, conferir o README no próprio computador e corrigir um detalhe). Se não for possível, registro no relatório.

## Escopo (passo a passo)

### Passo 1 — Atualizar documentação com o que mudou desde a F9
- `README.md` da raiz: status das fases (F8 com UX, documentos e compartilhamento), contagem de testes e telas do app.
- `RELATORIO.md`: recontar os testes (o Android agora tem 50 testes unitários) e incluir os casos de UX: voltar ao Início, documentos, compartilhamento e arquivo ausente.
- Plano de cada fase copiado para `docs/planos/` (inclui este plano atualizado).

### Passo 2 — README raiz final
- Visão geral, arquitetura, estrutura, pré-requisitos, como executar (banco, API, app, sensores), como testar.
- Substituir os dois itens pendentes: declaração de IA e link do pitch.
- Links relativos para cada parte.

### Passo 3 — Teste de clone limpo, do zero
- Clonar o repositório do GitHub em uma pasta nova, sem `.env`, `bin/`, `build/` nem `seed_dispositivos.sql`.
- Seguir só o README, na ordem: banco, schema, seed, chaves, API, `dotnet test`, sensores e app.
- Qualquer passo que falhar vira correção do README antes de fechar a fase. É o teste que o avaliador vai fazer.
- Incluir o teste do app: `./gradlew assembleDebug testDebugUnitTest` e o fluxo de código mais compartilhamento descrito em `iot/README.md`.

### Passo 4 — PDF com as seis partes
Um Markdown consolidado (`ENTREGA-PDF.md`) gerando um único PDF, com:
- Capa: grupo GateMinds, integrantes, disciplina.
- **Parte 1 (banco):** DER, explicação da modelagem e as 7 consultas com resultado.
- **Parte 2 (API):** endpoints, status HTTP, camadas e exemplos de curl.
- **Parte 3 (testes):** plano de casos e `RELATORIO.md` (contagens atualizadas).
- **Parte 4 (frontend mobile):** telas com as capturas novas (login, início, atendimento, consultar, saúde, documentos e compartilhamento).
- **Parte 5 (segurança):** modelo de ameaças, autenticação, RBAC, boas práticas e LGPD (SDD seção 8).
- **Parte 6 (IoT):** sensor, medição e influência (`iot/README.md`).
- **Anexo:** link do repositório, link do vídeo, declaração de IA, limitações e o ADR do NFC.
- Ferramenta: não há pandoc nem wkhtmltopdf instalados. Uso HTML gerado do Markdown e impresso em PDF pelo Chrome sem interface (ou pelo navegador do app). Instalo uma dependência só se for necessário e peço antes.

### Passo 5 — Roteiro e checklist do vídeo pitch (`docs/roteiro-pitch.md`)
Até 3 minutos, mostrando o protótipo (não slides), com os tempos do SDD seção 12:
1. **Problema (0:00 a 0:30):** busca de remédio vazia, ou cidadão na fila.
2. **Solução (0:30 a 1:00):** as frentes do cartão (cartão digital, código de atendimento, documentos e sinais vitais).
3. **Demonstração (1:00 a 2:00):** login, código no app, terminal validando, botão de compartilhar documentos, e tensiômetro gerando alerta crítico.
4. **Segurança (2:00 a 2:30):** documentos só no aparelho, sem código nada é liberado, bloqueio após três falhas. Uma frase honesta: o toque por NFC é simulado.
5. **Impacto (2:30 a 3:00):** número de testes, ODS 3, 10 e 11, e próximos passos (NFC real com HCE).
- Checklist de gravação: banco, API, emulador e terminal no ar antes de gravar; arquivos de exemplo no celular; segredo da Maria salvo no app.
- Link testado em **aba anônima** antes de ir para o `.txt`.

### Passo 6 — Arquivo `.txt` de entrega
- Já tem nome do grupo e RMs. Falta só o link do vídeo.
- Texto simples, sem caracteres especiais, como está.

### Passo 7 — Montagem do `.zip`
- Gerado a partir do **git** (`git archive`), para não levar arquivos ignorados: `.env`, `bin/`, `obj/`, `build/`, `.gradle/`, `local.properties`, `seed_dispositivos.sql`, logs.
- Raiz do `.zip`: pasta do projeto, `ENTREGA-PDF` (PDF), `ENTREGA-TXT.txt`.
- Verificação: listar o conteúdo, procurar padrões de segredo (`git grep` e busca por chaves e senhas), conferir tamanho e abrir o `.zip` em pasta vazia.

### Passo 8 — Checklist final do edital (SDD seção 14)
Conferir item a item: `.zip`, README, PDF com as 6 partes, `.txt` com integrantes e link, `dotnet test` verde, app compilando e testes do Android, scripts IoT funcionando, nenhum segredo versionado, e histórico de commits.

## Verificação

1. Clone limpo segue o README do zero e chega a "API no ar, testes verdes e app compilando".
2. O `.zip` abre, e a lista não contém nenhum arquivo ignorado nem segredo.
3. O PDF abre, tem as seis partes, e os números batem com o `RELATORIO.md`.
4. O `.txt` tem os nomes e RMs de todos e um link que abre em aba anônima.
5. O vídeo tem até 3 minutos, mostra o protótipo e está público.
6. Checklist do edital completo, item por item.

**Critério de pronto da F10:** itens de 1 a 6 passam e o `.zip` está pronto para envio na plataforma antes de 19/11/2026 às 23h55.

## Cronograma sugerido

| Data | O que |
|---|---|
| 11/11 a 12/11 | Passos 1 e 2 (documentação e README), declaração de IA aprovada |
| 13/11 | Passo 3 (clone limpo) e correções |
| 14/11 | Passo 4 (PDF) e Passo 5 (roteiro) |
| 15/11 a 16/11 | Grupo grava o vídeo e envia o link; commits dos integrantes |
| 17/11 | Passos 6 a 8 (`.txt`, `.zip`, checklist). Meta de entrega |
| 18/11 e 19/11 | Margem e envio na plataforma |

## Riscos

| Risco | Mitigação |
|---|---|
| Vídeo com link privado ou quebrado | Testar em aba anônima antes de enviar |
| Gravar tarde demais | Gravar até 16/11, deixando espaço para regravar |
| Pacote com segredo ou grande demais | `git archive` e varredura de padrões de segredo |
| README falha no clone limpo | Passo 3 corrige antes do PDF e do `.zip` |
| Declaração de IA fora da regra da disciplina | Confirmar a regra com o professor antes de enviar |
| Histórico com um único autor | Commits dos demais integrantes; se não der, registrar no relatório |
| Demo ao vivo falhar na gravação | Checklist de ambiente e arquivos de exemplo prontos; gravar em partes |

## Arquivos a criar ou mudar

- Criar: `ENTREGA-PDF.md` e o PDF, `docs/roteiro-pitch.md`, o `.zip` final em `gs-2026/`.
- Mudar: `README.md` (status, IA, link), `RELATORIO.md` (contagens e casos de UX), `ENTREGA-TXT.txt` (link do vídeo).

Nenhuma mudança na API nem no app é prevista; se o clone limpo achar defeito, a correção entra como commit próprio.

## Perguntas em aberto

1. **Aprova o texto de declaração de IA baseado no uso real?** (recomendado)
2. **Quem grava o vídeo e onde ele será publicado?** (YouTube não listado ou Google Drive com link)
3. **Aprova o PDF único gerado de HTML pelo Chrome?** (recomendado)
