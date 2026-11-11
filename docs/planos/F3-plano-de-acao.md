# Plano de ação — Fase F3 (Domínio da API)

## Context

A F2 entregou a fundação: autenticação, erros padronizados e o seeder. A F3 implementa o domínio do MVP, ou seja, as regras que tornam o sistema útil: o acolhimento presencial com TOTP, a telemetria com alertas, o prontuário, os documentos, os sinais vitais, a auditoria e o proxy para as APIs do SUS.

O critério de aceite do SDD v3 (seção 10) é: todos os endpoints respondem conforme a seção 5, e os curls felizes e de erro funcionam. Esta fase é a maior do backend (estimativa do SDD: 14 horas). Por isso o plano é dividido em blocos que podem ser testados um a um.

## Decisões a confirmar

1. **Dispositivos IoT na F3 (recomendado).** E3 e E6 (via dispositivo) exigem chave de dispositivo. Proponho mover `iot/gerar_chaves.py` da F6 para a F3, porque sem ele não há como testar o terminal NFC nem o tensiômetro por `curl`. O script só gera as três chaves, grava o hash no banco e escreve as chaves no `.env` (ignorado pelo git).
2. **Acesso do profissional ao prontuário (recomendado: com consentimento).** O SDD diz que o `PROFISSIONAL_SAUDE` acessa o prontuário "temporariamente, mediante CNS + token ativo". Proponho que E4, E5 e E10 só liberem os dados do cidadão se existir uma sessão `AUTORIZADO` dos últimos 10 minutos para aquele cidadão. Sem isso, o profissional leria qualquer prontuário com o login dele. A alternativa é liberar sem restrição, mais simples, mas enfraquece a tese de consentimento do projeto.
3. **Anti-reuso do TOTP em memória (recomendado).** O SDD fala de cache em memória (`ConcurrentDictionary`) para impedir o reuso de um código. Com uma única instância da API, isso funciona. Se no futuro houver mais de uma instância, o controle precisa ir para o banco. Registro a limitação.

## Escopo (passo a passo)

### Bloco A — Base do domínio
- **Erros com código próprio.** Hoje o middleware mapeia o código pelo tipo da exceção. Vou fazer cada exceção carregar o próprio código, para acomodar `TOKEN_INVALIDO` e `BLOQUEADO` (ambos 403) e `FALHA_ORIGEM` (502).
- **Novas exceções:** `TokenInvalidoException` (403), `BloqueadoException` (403), `FalhaOrigemException` (502).
- **Paginação:** um tipo `PaginaResposta<T>` com `content`, `page`, `size` e `totalElements`, usado por E12, E13 e E14.
- **Repositórios** para as entidades que faltam (sinais, histórico, documentos, sessões, alertas, unidades, dispositivos), com consultas parametrizadas via LINQ.

### Bloco B — Autenticação de dispositivo (X-API-KEY)
- `ApiKeyAuthHandler` (esquema próprio): lê `X-API-KEY`, calcula o SHA-256 e procura em `TB_DISPOSITIVO_IOT.CHAVE_API_HASH` com `ATIVO = 1`.
- Chave ausente ou inválida → 401 `NAO_AUTORIZADO`. Dispositivo inativo → 401.
- O dispositivo autenticado fica disponível no contexto (tipo, unidade e cidadão vinculado).

### Bloco C — Acolhimento e TOTP (E3)
- `TotpService` (RFC 6238): gera e valida código de 6 dígitos com HMAC-SHA1 sobre o segredo Base32 decodificado. Janela de ±1 passo de 30 segundos. Comparação em tempo constante.
- Anti-reuso: `ConcurrentDictionary` com a chave `cidadao:contador`, descartando entradas antigas.
- `AcolhimentoService.ValidarNfcAsync`, na ordem do SDD 5.4:
  1. Busca o cidadão pelo CNS → 404 `NAO_ENCONTRADO` se não existir.
  2. Busca a unidade pelo CNES e confere se é a unidade do terminal → 404 se não existir, 403 se for outra unidade.
  3. Conta as sessões `REJEITADO` do cidadão nos últimos 10 minutos. Se forem 3 ou mais → `BLOQUEADO` e cria alerta `TENTATIVA_INDEVIDA` (uma vez por bloqueio).
  4. Valida o TOTP. Se falhar → grava sessão `REJEITADO` com o motivo e retorna `TOKEN_INVALIDO`.
  5. Sucesso → grava sessão `AUTORIZADO` e monta o resumo: documentos, alertas críticos (alergias graves) e últimas medições.
- Regra do bloqueio: 4ª tentativa dentro da janela recebe `BLOQUEADO` mesmo com o código certo.

### Bloco D — Telemetria e alertas (E6, E10)
- `TelemetriaService.IngerirAsync`: valida o tipo (`PRESSAO_SISTOLICA`, `PRESSAO_DIASTOLICA`, `GLICEMIA`, `PESO`, `FREQUENCIA_CARDIACA`) e o valor (maior que zero).
- Origem: `IOT` quando a chave é de dispositivo, `APP` quando é JWT.
- Dispositivo tensiômetro: o cidadão vem do vínculo. Se o `numeroCns` vier informado e não bater com o vínculo → 403 `SEM_PERMISSAO`.
- Limiares: sistólica acima de 180 ou diastólica acima de 110 → `PRESSAO_CRITICA`. Glicemia de 300 ou mais → `GLICEMIA_CRITICA`. Cada alerta vira linha em `TB_ALERTA`, e a resposta traz `alertaDisparado`.
- E10 lista os alertas do cidadão, com filtro por status e paginação simples.
- Endpoint extra `PUT /api/v1/cidadao/{id}/alertas/{idAlerta}` marca `ENTREGUE` (usado pelo app).

### Bloco E — Cidadão: prontuário, sinais e histórico (E4, E5, E7)
- E4 prontuário: histórico clínico, alertas pendentes e resumo. Regra de acesso (decisão 2 acima).
- E5 sinais vitais: filtro por tipo, `de` e `ate`, resumo com média, mínima, máxima e total.
- E7 atualização de histórico: só o próprio cidadão. Valida `grauSeveridade` contra o enum. 404 se o registro não pertencer ao cidadão.
- Endpoint extra `GET /api/v1/cidadao/{id}/acessos`: últimas sessões do cidadão (tela 4 do app).

### Bloco F — Documentos (E8, E9)
- E8: registra metadado de documento. O servidor só recebe o nome, o tipo, o hash e a validade. `DATA_VALIDADE` é obrigatória para `RECEITA_MEDICA`.
- E9: exclui o metadado. Só o dono pode excluir → 403 caso contrário. Retorna 204.

### Bloco G — Unidades e proxy do SUS (E11, E12, E13)
- E13: lista paginada das unidades ativas, com coordenadas.
- E11: proxy para `GET /assistencia-a-saude/cnes-equipamentos` com `co_cnes`. Converte os campos para o formato do SDD (`quantidadeExistente`, `quantidadeEmUso`). Timeout de 5 segundos e uma nova tentativa. Falha → 502 `FALHA_ORIGEM`. Sem equipamentos → 404.
- E12: proxy para `GET /daf/estoque-medicamentos-bnafar-horus` com `codigo_municipio` e `codigo_catmat`. Retorno normalizado e paginado. Usa o nome do catálogo local, não o da API, porque o campo `descricao_produto` vem vazio em parte dos registros.
- Catálogo: `Data/catalogo-medicamentos.json` com os 14 medicamentos já validados (SDD 5.3). Endpoint extra `GET /api/v1/sus/medicamentos/catalogo?nome=` faz a busca por substring, sem acento e sem diferença de maiúsculas.
- `HttpClient` tipado (`IHttpClientFactory`) com `BaseAddress` configurável por variável de ambiente, para os testes poderem apontar para um servidor falso.

### Bloco H — Auditoria (E14)
- Lista paginada das sessões com filtros `status`, `de` e `ate`, incluindo nome do cidadão, unidade e motivo.
- Restrito ao perfil `ADMIN` (403 para os demais).

### Bloco I — Autorização
- Políticas no `Program.cs`: `Admin`, `Profissional` (profissional ou admin), `CidadaoProprio` (dono do recurso ou profissional com consentimento, conforme a decisão 2), `Dispositivo` (esquema de chave).
- Verificação de propriedade no service, não só no controller, com `SemPermissaoException` (403).

### Bloco J — Documentação e evidências
- Swagger atualizado com todos os endpoints, com exemplos de request e response.
- `docs/postman-collection.json` exportado do Swagger (evidência do edital).
- Scripts de demonstração do IoT (`iot/terminal_nfc.py`, `iot/tensiometro.py`) **não** entram na F3. Ficam na F6.

## Ordem de execução

1. Bloco A (base) e Bloco B (dispositivo): sem eles nada do resto roda.
2. Blocos G e H (catálogo, proxy e auditoria): não dependem de TOTP e são fáceis de validar.
3. Bloco C (E3 e TOTP): núcleo do pilar 3.
4. Bloco D (telemetria e alertas): depende do Bloco B.
5. Blocos E e F (cidadão e documentos).
6. Bloco I (políticas) é aplicado junto com cada bloco, não no fim.
7. Bloco J (documentação e evidências) fecha a fase.

## Verificação

Com o banco, o seed e a API rodando:

1. `dotnet build` sem erros.
2. **E3 feliz:** `curl` com chave do terminal e TOTP atual do João ou da Maria (gerado com o segredo impresso pelo seeder) → 200 `AUTORIZADO`, e sessão gravada.
3. **E3 com código errado** → 403 `TOKEN_INVALIDO`, sessão `REJEITADO`.
4. **E3 bloqueio:** três códigos errados seguidos e um quarto, mesmo com o certo → 403 `BLOQUEADO`, e alerta `TENTATIVA_INDEVIDA` criado.
5. **E3 código reutilizado** dentro da mesma janela → 403.
6. **E3 unidade errada** (CNES de outra unidade) → 403.
7. **E6 IoT:** tensiômetro envia 185 → 201 com `alertaDisparado = true`. Enviar 120 → 201 sem alerta. Enviar com `numeroCns` de outra pessoa → 403.
8. **E6 JWT:** cidadão envia glicemia 310 → alerta `GLICEMIA_CRITICA`.
9. **E10:** lista os alertas, e o `PUT` marca como `ENTREGUE`.
10. **E4 e E5:** cidadão lê o próprio prontuário → 200. Lê o de outro → 403. Profissional sem sessão recente → 403. Com sessão recente → 200.
11. **E7:** atualiza histórico próprio → 200. Registro de outro cidadão → 404 ou 403.
12. **E8 e E9:** cria documento → 201. `RECEITA_MEDICA` sem validade → 400. Exclui → 204.
13. **E11:** CNES válido → 200 normalizado. Simular falha da origem (servidor falso ou URL inválida) → 502 `FALHA_ORIGEM`.
14. **E12:** busca por CATMAT de insulina asparte com município 355030 → 200 com estoque. Catálogo com `nome=insulina` → retorna asparte.
15. **E13:** lista paginada com 16 unidades.
16. **E14:** como ADMIN → 200 paginado. Como cidadão → 403.
17. **Segurança:** SQL com aspas (`'1 OR 1=1--'`) em qualquer parâmetro → 400 ou 404, nunca dados.
18. Salvar toda a saída em `docs/evidencia-testes/f3-*.txt` e exportar `docs/postman-collection.json`.

**Critério de pronto da F3:** os itens de 1 a 17 passam, o Swagger lista os 14 endpoints mais os extras, e as evidências estão em `docs/evidencia-testes/`.

## Riscos

| Risco | Mitigação |
|---|---|
| Dispositivo sem chave na hora do teste | Decisão 1 (gerar chaves na F3) |
| TOTP com relógio dessincronizado | Janela de ±1 passo; testes usam o relógio do próprio servidor |
| API do SUS fora do ar na demo | Timeout de 5 segundos, 502 tratado, e evidência com resposta salva |
| Código CATMAT sem estoque em um dia | Catálogo validado na véspera; teste de E12 faz parte do checklist |
| Escopo crescer | Bloco J e os extras ficam por último, se houver tempo |

## Arquivos a criar

- `cartao-sus/backend/src/CartaoSus.Api/Security/` (`ApiKeyAuthHandler`, `TotpService`, políticas de autorização)
- `cartao-sus/backend/src/CartaoSus.Api/Services/` (acolhimento, telemetria, cidadão, documentos, unidades, SUS e auditoria)
- `cartao-sus/backend/src/CartaoSus.Api/Repositories/` (novos repositórios)
- `cartao-sus/backend/src/CartaoSus.Api/Controllers/` (E3 a E14 e extras)
- `cartao-sus/backend/src/CartaoSus.Api/Data/catalogo-medicamentos.json`
- `cartao-sus/iot/gerar_chaves.py` (se a decisão 1 for aprovada)
- `cartao-sus/docs/postman-collection.json` e `docs/evidencia-testes/f3-*.txt`

Arquivos existentes que mudam: `Exceptions/DomainExceptions.cs` e `Middleware/ErrorHandlingMiddleware.cs` (código por exceção), `Program.cs` (novos serviços e políticas) e `backend/README.md` (endpoints da F3).

## Perguntas em aberto

1. **Gerar as chaves dos dispositivos já na F3?** Recomendo sim, porque sem isso não há como testar E3 e E6 por dispositivo.
2. **Profissional só lê prontuário com sessão autorizada recente (10 minutos)?** Recomendo sim, para respeitar o consentimento do SDD.
3. **Anti-reuso do TOTP em memória?** Recomendo sim, com a limitação de uma única instância registrada.
