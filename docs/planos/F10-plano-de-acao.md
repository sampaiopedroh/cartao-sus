# Plano de ação — Fase F10 (Entrega final)

## Context

As fases F0 a F9 entregaram o MVP, com código, testes e evidências. Falta montar o pacote que o professor recebe na plataforma, até 19/11/2026 às 23h55, e que o edital descreve (slide 14): um `.zip` do projeto com README de execução, um PDF com os entregáveis de cada parte, e um `.txt` com nome do grupo, RM de cada integrante e o link do vídeo pitch.

Além disso, o vídeo pitch (5 pontos) precisa ser gravado, enviado a um link público e testado em aba anônima. Hoje são 11/11, então há oito dias.

Estimativa do SDD: 4 horas de trabalho meu, mais a gravação do vídeo pelo grupo.

## Decisões e informações que preciso de você

1. **Nomes, RMs e e-mails de cada integrante do grupo.** São obrigatórios no `.txt` e não consigo obtê-los sozinho.
2. **Declaração de uso de IA (recomendado: texto baseado no que realmente foi usado).** A regra da disciplina ainda aparece como exemplo no slide 15. Proponho escrever a declaração com as ferramentas efetivamente usadas (Claude para o código e a documentação, Gemini para revisão do SDD) e o que cada uma fez, mais a revisão humana. Você confirma se o grupo concorda com esse texto.
3. **Vídeo pitch: quem grava e onde publica (recomendado: o grupo grava com o protótipo rodando; publicação no YouTube como não listado, ou Google Drive com acesso por link).** Eu preparo o roteiro final de 3 minutos com os tempos do SDD (seção 12) e a lista de cenas a gravar com o próprio app e a API.

## Escopo (passo a passo)

### Passo 1 — README raiz final
- Revisar o `cartao-sus/README.md` com o que o avaliador precisa: visão geral, arquitetura, estrutura, pré-requisitos, como executar (banco, API, app, sensores), como testar, declaração de IA e link do vídeo.
- Substituir os TODOs restantes (declaração de IA e link do pitch).
- Links relativos para as pastas de cada parte.

### Passo 2 — Teste de clone limpo do zero
- Clonar o repositório em uma pasta nova, sem `.env`, sem `bin/`, sem `build/` e sem `seed_dispositivos.sql`.
- Seguir só o README, na ordem: banco, schema, seed, chaves, API, `dotnet test`, sensores e aplicativo.
- Qualquer passo que falhar é corrigido no README antes de fechar a fase. Este é o teste que o avaliador vai fazer.

### Passo 3 — PDF com os entregáveis das seis partes
- Um documento único, gerado a partir de um Markdown consolidado (`ENTREGA-PDF.md`), com:
  - Capa com nome do grupo, integrantes e disciplina.
  - Parte 1 (banco): DER (`database/DER.md`), explicação da modelagem e as 7 consultas com resultado.
  - Parte 2 (API): lista dos endpoints, status HTTP, camadas e exemplos de curl.
  - Parte 3 (testes): plano de casos e resultado consolidado (`RELATORIO.md`).
  - Parte 4 (frontend mobile): telas com capturas (F7 e F8).
  - Parte 5 (segurança): modelo de ameaças, autenticação, RBAC, boas práticas e LGPD (SDD seção 8).
  - Parte 6 (IoT): explicação de sensor, medição e influência (`iot/README.md`).
  - Anexo: link do repositório, link do vídeo, declaração de IA e limitações.
- Ferramenta de geração: escolho na execução a melhor disponível (pandoc, se instalado, ou impressão a partir de HTML pelo navegador). Confirmo com você se o formato é PDF mesmo.

### Passo 4 — Arquivo `.txt` de entrega
- Nome do grupo, RM e nome completo de cada integrante, e-mail (opcional), e o link público do vídeo pitch.
- Conteúdo mínimo e formato simples, sem caracteres especiais.

### Passo 5 — Vídeo pitch (5 pontos)
- Roteiro final de 3 minutos, com os tempos do SDD seção 12 e as cenas que o grupo precisa gravar:
  1. Problema (0:00 a 0:30): cena da busca de remédio vazia ou do cidadão na fila.
  2. Solução (0:30 a 1:00): as três frentes do cartão.
  3. Demonstração (1:00 a 2:00): login, código TOTP no app, handshake no terminal, tensiômetro com banner de alerta.
  4. Segurança (2:00 a 2:30): documento no aparelho e cartão que não abre nada sem o código.
  5. Impacto (2:30 a 3:00): métricas (76 casos), ODS 3, 10 e 11, e próximos passos.
- Regras do edital: até 3 minutos, mostrar o protótipo (não slides), link público testado em aba anônima.
- Checklist de gravação com o ambiente pronto (API, banco e aplicativo no ar antes de gravar).

### Passo 6 — Montagem do `.zip`
- Incluir: `cartao-sus/` completo sem arquivos ignorados (`.env`, `bin/`, `obj/`, `build/`, `.gradle/`, `local.properties`, `seed_dispositivos.sql`, logs).
- Verificar com `git archive` ou com uma lista explícita de arquivos, e conferir se nenhum segredo está dentro (`git grep` e busca por padrões de chave).
- Incluir na raiz do `.zip`: README, PDF, `.txt` do grupo e pasta do projeto.

### Passo 7 — Checklist final do edital (SDD seção 14)
- Conferir item a item: `.zip`, README, PDF com as 6 partes, `.txt` com integrantes e link, `dotnet test` verde, app compilando, scripts IoT funcionando, nenhum segredo versionado e repositório com commits de todos.
- Os commits de todos os integrantes são um diferencial do edital; isso depende de cada um do grupo fazer commits com o próprio nome. Vou registrar no relatório que o histórico atual é de um único autor.

## Verificação

1. Clone limpo segue o README do zero e chega ao estado "API no ar, testes verdes e app compilando".
2. O `.zip` abre, e a lista de arquivos não contém nenhum item ignorado nem segredo.
3. O PDF abre, tem as seis partes, e os números batem com `RELATORIO.md`.
4. O `.txt` tem os nomes e RMs de todos os integrantes e um link que abre em aba anônima.
5. O vídeo tem até 3 minutos, mostra o protótipo funcionando e está publicado com acesso por link.
6. Checklist do edital completo, com cada item marcado.

**Critério de pronto da F10:** itens de 1 a 6 passam e o `.zip` está pronto para envio na plataforma antes de 19/11/2026 às 23h55.

## Riscos

| Risco | Mitigação |
|---|---|
| Prazo do vídeo apertado | Gravar até 16/11, deixando margem para regravar |
| Vídeo com link privado ou quebrado | Testar em aba anônima antes de enviar |
| Pacote com segredo ou arquivo grande demais | Lista explícita de arquivos e verificação com `git grep` |
| Declaração de IA fora da regra da disciplina | Confirmar a regra com o professor antes de enviar |
| Histórico do git com um único autor | Cada integrante faz commits com o próprio nome; registrar no relatório caso não seja possível |

## Arquivos a criar

- `cartao-sus/README.md` (revisão final)
- `ENTREGA-PDF.md` e o PDF gerado a partir dele
- `ENTREGA-TXT.txt` com integrantes e link do vídeo
- `docs/roteiro-pitch.md` (roteiro e checklist de gravação)
- O `.zip` final na pasta `gs-2026/`

Nenhuma mudança na API, no app ou nos testes é prevista.

## Perguntas em aberto

1. **Nome, RM e e-mail de cada integrante do grupo?**
2. **Aprova o texto de declaração de IA baseado no uso real (recomendado)?**
3. **Quem grava o vídeo e onde ele será publicado (YouTube não listado ou Google Drive com link)?**
