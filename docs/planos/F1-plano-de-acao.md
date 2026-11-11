# Plano de ação — Fase F1 (Banco de dados)

## Context

A F1 transforma o modelo do SDD v3 (seção 4) em scripts SQL que rodam no MySQL 8 da F0. O critério de aceite do SDD (seção 10) é: `schema.sql` e `seed.sql` rodam sem erro, e os 7 SELECTs de `consultas.sql` retornam dados. A F1 também entrega o DER, que o edital exige (Parte 1, item 01).

Ao revisar o SDD v3 para este plano, encontrei um problema que precisa de decisão antes de codar: o seed atual tem placeholders (`__SERA_GERADO__`) em colunas `UNIQUE`. Por exemplo, `CPF_HASH` é único, e os dois cidadãos recebem o mesmo texto, então o `INSERT` falha com erro de chave duplicada. O mesmo acontece com `CHAVE_API_HASH` nos três dispositivos. O critério da F1 não passa como está escrito.

## Decisões propostas (a confirmar)

1. **Dispositivos IoT fora do seed (recomendado).** As linhas de `TB_DISPOSITIVO_IOT` saem do `seed.sql` e passam a ser criadas pelo `iot/gerar_chaves.py` na F6, porque o hash só existe quando a chave é gerada. Isso remove a duplicidade e mantém o seed sem segredos.
2. **`CPF_HASH` com valor real (recomendado).** Em vez de placeholder, o seed grava o SHA-256 de dois CPFs fictícios (gerados e documentados em `database/README.md` como "CPF fictício de demonstração"). Não é segredo, porque o CPF é inventado, e o valor é único.
3. **Schema re-executável (recomendado).** No topo do `schema.sql`, `DROP TABLE IF EXISTS` na ordem inversa das dependências, para que rodar de novo reconstrua o banco sem `docker compose down -v`. Não usamos `DROP DATABASE`, para não apagar outros bancos por acidente.

## Escopo da F1 (passo a passo)

### Passo 1 — `database/schema.sql`
- Copiar o DDL da seção 4.3 do SDD v3 (9 tabelas).
- Adicionar o bloco de `DROP TABLE IF EXISTS` do item 3.
- Usar `USE cartao_sus;` e não criar o banco (o compose já cria).
- Verificar que todos os `ENUM`, `CHECK` e `FOREIGN KEY` estão presentes, incluindo `CK_IOT_VINCULO` e `CK_SINAL_VALOR_POSITIVO`.

### Passo 2 — `database/seed.sql`
- Usuários: 4 linhas. `SENHA_HASH` recebe placeholder fixo (`'__SERA_GERADO__'`), porque o hash real é gerado pelo backend na F2 (SDD 8.5). Não há `UNIQUE` nessa coluna, então o placeholder não quebra o script.
- Cidadãos: 2 linhas com `CPF_HASH` real (decisão 2) e `SEGREDO_TOTP` placeholder. `SEGREDO_TOTP` não é `UNIQUE`.
- Unidades: as 16 unidades reais já levantadas, com IDs fixos 1 a 16 (o SDD v3 §4.4 e §4.4.1 já trazem os valores).
- Carteira, histórico, sinais vitais, sessões de acesso e alertas: copiar do SDD v3, sem alteração de valores.
- `TB_DISPOSITIVO_IOT`: removido (decisão 1).
- Salvar em UTF-8 sem BOM, para os acentos entrarem certo.

### Passo 3 — `database/consultas.sql`
- As 7 consultas Q1 a Q7 do SDD v3 §4.5, cada uma com comentário explicando o que faz e qual requisito do edital atende (JOIN, agregação, HAVING etc.).
- Ajustar apenas o que for necessário para rodar. Q3 usa subconsulta correlacionada dentro do `ON`, então precisa ser testada.

### Passo 4 — `database/DER.md`
- Diagrama Mermaid `erDiagram` com as 9 entidades, copiado da seção 4.1 do SDD v3.
- Tabela de cardinalidades e as decisões de modelagem (3FN, `CPF_HASH`, trilha de auditoria).
- Renderiza direto no GitHub. O PDF da F10 usa a mesma fonte.

### Passo 5 — `database/README.md`
- Como rodar, na ordem: `schema.sql`, `seed.sql`, `consultas.sql`.
- Comando exato usando o container da F0, com `--default-character-set=utf8mb4`.
- Nota sobre a data: Q2 e Q7 usam `NOW() - INTERVAL 7 DAY`, então o resultado depende do relógio da máquina. Os dados de demo são de 08 a 09/11/2026.

### Passo 6 — Atualizar o README raiz
- Trocar o `TODO (Fase F1)` da tabela de estrutura por "Pronto".
- Adicionar a seção "Banco de dados (F1)" com link para `database/README.md`.

### Passo 7 — Evidências
- Rodar os três scripts e salvar a saída em `docs/evidencia-testes/f1-schema.txt`, `f1-seed.txt` e `f1-consultas.txt`. Essas evidências entram no PDF da F10 (edital: "evidências").

## Verificação

Com o MySQL da F0 `healthy`:

1. `schema.sql` roda sem erro, e `SHOW TABLES;` lista 9 tabelas.
2. Rodar `schema.sql` **uma segunda vez** também roda sem erro (prova da decisão 3).
3. `seed.sql` roda sem erro, e `SELECT COUNT(*)` bate com o esperado em cada tabela (4 usuários, 2 cidadãos, 16 unidades, 5 documentos, 5 históricos, 9 sinais, 5 sessões, 3 alertas).
4. Os 7 SELECTs de `consultas.sql` rodam e retornam linhas. Q2 e Q7 dependem da data: se rodar depois de 16/11/2026, eles voltam vazios, e isso precisa estar documentado.
5. Teste de integridade: um `INSERT` com `CK_IOT_VINCULO` violado (dispositivo sem vínculo) deve falhar. Um `INSERT` de sinal com `VALOR_MEDIDO = 0` deve falhar.
6. `git grep` no `database/` não mostra nenhum segredo (chave, senha ou `SEGREDO_TOTP` real).

**Critério de pronto da F1:** os passos 1 a 6 passam, os scripts estão no `database/`, o DER está documentado e as evidências estão em `docs/evidencia-testes/`.

## Riscos

| Risco | Mitigação |
|---|---|
| Acento quebrado no seed | Arquivo UTF-8 sem BOM e cliente com `utf8mb4` |
| Q2 e Q7 vazias com o tempo | Nota no `database/README.md` e data de referência nas evidências |
| Q3 com subconsulta lenta ou errada | Testar com o seed; se falhar, reescrever com `ROW_NUMBER()` (MySQL 8) |
| Seed divergir do SDD | Passo 2 copia valores do SDD; a verificação 3 confere contagens |

## Arquivos a criar

- `cartao-sus/database/schema.sql`
- `cartao-sus/database/seed.sql`
- `cartao-sus/database/consultas.sql`
- `cartao-sus/database/DER.md`
- `cartao-sus/database/README.md`
- `cartao-sus/docs/evidencia-testes/f1-schema.txt`, `f1-seed.txt`, `f1-consultas.txt`

Arquivo existente que muda: `cartao-sus/README.md` (passo 6). O `database/.gitkeep` é removido.
