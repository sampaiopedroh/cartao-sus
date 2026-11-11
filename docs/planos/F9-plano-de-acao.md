# Plano de ação — Fase F9 (Testes manuais, evidências e relatório final)

## Context

As fases anteriores já geraram evidências por partes: a bateria de 48 casos da F3, os testes automatizados (23 unitários e de contrato na F4, 12 de integração na F5), os cenários dos sensores na F6 e as telas do app na F7 e F8. Falta a visão consolidada que o SDD exige (seção 6.5 e 6.6): os 14 casos manuais MT-01 a MT-14, com resultado e evidência, e um relatório único cruzando UT, CT, IT e MT.

O critério do SDD v3 (seção 10, F9): a tabela MT preenchida, com evidências em `docs/evidencia-testes/`. O DoD da seção 6.6 pede pelo menos 14 casos manuais documentados e o relatório `RELATORIO.md`.

Estimativa do SDD: 4 horas.

## Decisões a confirmar

1. **Executar tudo de novo, em banco limpo (recomendado).** As evidências anteriores foram geradas em estados diferentes do banco e com versões intermediárias do app. Para o relatório final, proponho uma execução única e reproduzível: banco recriado pelo `schema.sql`, `seed.sql` e `gerar_chaves.py`, API e app na versão final, e um roteiro com os 14 casos. Cada caso registra data, hora, entrada e saída. Leva cerca de duas horas, mas o relatório fica coerente.
2. **Coleção do Postman com exemplos e testes (recomendado).** A coleção atual foi gerada do Swagger, sem corpos de exemplo nem asserções. Proponho enriquecê-la com um exemplo de corpo por requisição e testes simples (status esperado e campo `erro`), para que o avaliador rode a coleção inteira com um clique. Alternativa: manter só a exportação do Swagger.

## Escopo (passo a passo)

### Passo 1 — Preparação
- Banco limpo: `docker compose down -v`, `up -d`, `schema.sql`, `seed.sql`, `gerar_chaves.py`, `seed_dispositivos.sql`.
- API com o `.env` da execução, e o segredo TOTP de Maria e João anotado do console (sem gravar em arquivo versionado).
- Emulador com o app instalado a partir do commit final.

### Passo 2 — Roteiro dos casos manuais

| ID | Caso | Como executar | Esperado |
|---|---|---|---|
| MT-01 | Login válido | Login da Maria (API e app) | Token e perfil CIDADAO; 200 |
| MT-02 | Login com senha errada | Senha errada no app e na API | `NAO_AUTORIZADO` com mensagem genérica; 401 |
| MT-03 | Payload malformado | Login sem o campo `senha` | `VALIDACAO` com `campos.senha`; 400 |
| MT-04 | Cadastro duplicado | Cadastro com CNS já existente | `CONFLITO`; 409 |
| MT-05 | Handshake com TOTP válido | Código do app na tela Token enviado ao terminal | `AUTORIZADO` e resumo do paciente; 200 |
| MT-06 | Handshake com TOTP antigo | Código de 5 minutos atrás | `TOKEN_INVALIDO`; sessão gravada; 403 |
| MT-07 | Três falhas e alerta no app | Três códigos errados, depois abrir o Dashboard | `BLOQUEADO`; alerta `TENTATIVA_INDEVIDA` visível no app |
| MT-08 | Pressão crítica pelo tensiômetro | `tensiometro.py --cenario demo` com o app aberto | 201 com alerta; banner visível no app (Dashboard ou Sinais) |
| MT-09 | Tentativa de SQL injection | `GET /api/v1/cidadao/1 OR 1=1--/prontuario` e variantes com aspas | Sem dados; 400 ou 404 |
| MT-10 | Acesso ao prontuário de outro | Maria lendo `/cidadao/2/prontuario` | `SEM_PERMISSAO`; 403 |
| MT-11 | CRUD de documento | Criar, listar pelo handshake e excluir | 201, presença confirmada, 204 |
| MT-12 | Medicamento no BNAFAR | Busca "insulina" e estoque em São Paulo, no app e na API | Lista normalizada; 200 |
| MT-13 | App sem conexão | API parada, abrir Sinais e registrar medição | Mensagem de sem conexão e medição na fila local |
| MT-14 | Auditoria por perfil ADMIN | Admin lista rejeições; cidadão tenta o mesmo | 200 para admin; 403 para cidadão |

Para cada caso: data e hora, entrada exata, status ou tela observada, resultado (PASS ou FAIL) e nome do arquivo de evidência.

### Passo 3 — Evidências
- Saída de terminal em `docs/evidencia-testes/mt-XX-*.txt` (curl e scripts).
- Capturas de tela do app em `docs/evidencia-testes/mt-XX-*.png` quando o caso envolve a interface (MT-05, MT-07, MT-08, MT-12, MT-13).
- Logs de execução da API sem segredos (filtrados antes de salvar).

### Passo 4 — Coleção do Postman (decisão 2)
- Partir do `docs/postman-collection.json` atual.
- Adicionar um corpo de exemplo para cada requisição e um teste por requisição: status esperado e, quando aplicável, o campo `erro`.
- Variáveis `baseUrl`, `token` e `chaveDispositivo`, sem valores reais.
- Validar importando no Postman ou rodando pelo Newman, se disponível.

### Passo 5 — Relatório consolidado (`docs/evidencia-testes/RELATORIO.md`)
- Tabela única com UT-01 a UT-12, CT-01 a CT-08, IT-01 a IT-10 (com IT-08 e IT-10 em dois casos), MT-01 a MT-14, com resultado PASS ou FAIL, tipo de teste e arquivo de evidência.
- Resumo de contagem por tipo, com total e resultado geral.
- Seção de problemas encontrados ao longo do projeto, com a correção aplicada (os bugs de F5 e F8, o `AuthService` da F4 e o fuso horário da F6).
- Lista de limitações conhecidas, para o avaliador ter a visão completa.

### Passo 6 — Swagger
- Exportar `swagger.json` da API final e salvar em `docs/swagger.json`.
- Conferir que a lista de endpoints bate com a seção 5.2 do SDD mais os extras.

### Passo 7 — Atualização de documentação
- `docs/evidencia-testes/README.md`: índice de todas as evidências, por fase e tipo.
- README da raiz: status da F9.

## Verificação

1. Todos os 14 casos MT têm resultado registrado. Qualquer FAIL é corrigido ou documentado como limitação antes de fechar a fase.
2. Os casos de interface (MT-05, MT-07, MT-08, MT-12, MT-13) têm captura de tela do app.
3. Os casos de API (MT-01 a MT-04, MT-06, MT-09 a MT-11, MT-14) têm saída de terminal.
4. A coleção do Postman roda do começo ao fim sem falha de asserção, com o banco limpo.
5. `RELATORIO.md` bate com as saídas: os números de UT, CT, IT e MT conferem com as contagens das execuções.
6. Nenhum segredo nas evidências: `git grep` por senhas, chaves e segredos TOTP demo.

**Critério de pronto da F9:** itens de 1 a 6 passam, a tabela MT está preenchida e o `RELATORIO.md` existe.

## Riscos

| Risco | Mitigação |
|---|---|
| Código TOTP expirando durante o roteiro manual | Usar o modo de geração do terminal (`--app`) ou gerar o código no momento de cada envio |
| Estado do banco mudando entre os casos | Banco limpo no início e ordem fixa dos casos; MT-07 e MT-13 ao final, quando o estado já é conhecido |
| Estoque do BNAFAR variando com o tempo | Registrar a data da consulta; se o estoque mudar, o critério é o formato da resposta |
| Caso de app sem captura clara | Repetir o caso com a tela específica aberta e salvar a captura no momento do resultado |
| Relatório com números desatualizados | Gerar a tabela a partir das saídas, não à mão |

## Arquivos a criar

- `cartao-sus/docs/evidencia-testes/mt-01` a `mt-14` (`.txt` e `.png` conforme o caso)
- `cartao-sus/docs/evidencia-testes/RELATORIO.md`
- `cartao-sus/docs/evidencia-testes/README.md`
- `cartao-sus/docs/swagger.json`

Arquivos existentes que mudam: `docs/postman-collection.json` (exemplos e testes, se a decisão 2 for aprovada) e `README.md` (status da F9).

Nenhuma mudança na API ou no app é prevista. Se um caso falhar por defeito, a correção entra como commit próprio.

## Perguntas em aberto

1. **Executar os 14 casos de novo, em banco limpo? (recomendado)**
2. **Enriquecer a coleção do Postman com exemplos e testes? (recomendado)**
