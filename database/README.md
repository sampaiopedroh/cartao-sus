# Banco de dados — Cartão SUS Inteligente

Scripts do MySQL 8. O modelo lógico está em [`DER.md`](DER.md).

| Arquivo | Conteúdo |
|---|---|
| `schema.sql` | 9 tabelas, com chaves, `ENUM`, `CHECK` e índices |
| `seed.sql` | Carga inicial de demonstração (sem segredos) |
| `consultas.sql` | 7 consultas de uso real, com JOIN e agregação |

## Pré-requisito

O MySQL da fase F0 precisa estar rodando e `healthy`. Veja o [README da raiz](../README.md#como-executar-f0--banco-de-dados).

## Como executar

Rode dentro da pasta `cartao-sus/`, nesta ordem:

```bash
docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/schema.sql
```

```bash
docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/seed.sql
```

```bash
docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/consultas.sql
```

O `schema.sql` pode ser executado de novo: ele apaga e recria as tabelas. Para voltar a demo ao estado inicial, rode `schema.sql` e depois `seed.sql`.

Para ver o resultado das consultas no terminal, use o mesmo comando do `consultas.sql` acima.

## Dados de demonstração

- **Cidadãos:** Maria Souza Lima (CNS 898000000000000) e João Pereira Santos (CNS 182345678901234).
- **CPFs:** os hashes de `CPF_HASH` foram gerados a partir de CPFs **fictícios** de demonstração. Não são CPFs reais.
- **Unidades:** 16 unidades reais da cidade de São Paulo, com códigos CNES oficiais (SDD 4.4.1).
- **Senhas e segredos TOTP:** o `seed.sql` deixa `__SERA_GERADO__`. Eles são preenchidos pelo backend na primeira execução (fase F2).
- **Dispositivos IoT:** não estão no seed. São criados pelo `iot/gerar_chaves.py` (fase F6).

## Atenção à data

As consultas **Q2** (glicemia dos últimos 7 dias) e **Q7** (sessões rejeitadas dos últimos 7 dias) usam `NOW() - INTERVAL 7 DAY`. Os dados de demonstração são de 08 a 09/11/2026.

- Até 16/11/2026, as duas consultas retornam linhas.
- Depois disso, elas voltam vazias. Isso é esperado, não é erro de SQL.

As evidências em `docs/evidencia-testes/` foram geradas em 11/11/2026.

## Evidências

- `docs/evidencia-testes/f1-schema.txt`: execução do `schema.sql` duas vezes.
- `docs/evidencia-testes/f1-seed.txt`: contagem por tabela, comparada com o esperado.
- `docs/evidencia-testes/f1-consultas.txt`: saída das 7 consultas.
- `docs/evidencia-testes/f1-integridade.txt`: quatro violações que o banco deve recusar.
