# Evidência F4 — testes unitários e de contrato

Execução: `dotnet test backend/CartaoSus.slnx` **sem MySQL em execução** (container parado).
Resultado: **23 de 23 aprovados**, 0 falhas. Saída completa em `f4-dotnet-test.txt`.

| ID | Teste | Resultado |
|---|---|---|
| UT-01 | Código válido no passo atual é aceito (vetor RFC 6238) | Aprovado |
| UT-02 | Código de 5 minutos atrás é recusado | Aprovado |
| UT-03 | Código reutilizado é recusado na segunda vez | Aprovado |
| UT-04 | Sistólica de 185 gera PRESSAO_CRITICA | Aprovado |
| UT-05 | Pressão de 120 não gera alerta | Aprovado |
| UT-06 | Glicemia de 310 gera GLICEMIA_CRITICA | Aprovado |
| UT-07 | Três falhas geram TENTATIVA_INDEVIDA e a quarta é bloqueada | Aprovado |
| UT-08 | Duas falhas e um código válido autorizam | Aprovado |
| UT-09 | Senha errada lança CredenciaisInvalidasException | Aprovado |
| UT-10 | Senha gravada como hash, nunca em texto puro | Aprovado |
| UT-11 | Excluir documento de outro cidadão é recusado | Aprovado |
| UT-12 | API do SUS fora do ar (503) vira FalhaOrigemException | Aprovado |
| CT-01 | Login 200: formato do token e do perfil | Aprovado |
| CT-02 | Login 401: envelope com NAO_AUTORIZADO | Aprovado |
| CT-03 | Prontuário 200: listas de histórico e alertas | Aprovado |
| CT-04 | Prontuário 403: envelope com SEM_PERMISSAO | Aprovado |
| CT-05 | Validar NFC 200: paciente com alertas e medições | Aprovado |
| CT-06 | Validar NFC 403: envelope com TOKEN_INVALIDO | Aprovado |
| CT-07 | Telemetria 201: alertaDisparado e alerta | Aprovado |
| CT-08 | Excluir documento 204: sem corpo | Aprovado |

Testes extras: sete casos de TOTP e telemetria adicionais (código de tamanho errado, segredo de outro cidadão, tensiômetro com CNS de outra pessoa, terminal de outra unidade e cadastro com hash).

## Verificação adicional

- **Quebra de contrato proposital:** renomeei `expiraEmMinutos` para `expiraEm` no snapshot de login. O CT-01 falhou, com a mensagem apontando o campo. Snapshot restaurado; os 23 voltaram a passar.
- **Regressão da F3:** após a refatoração do `AuthService`, a bateria de 48 casos de ponta a ponta passou inteira (`f4-regressao.txt`). O cadastro novo retorna 201 e o duplicado retorna 409.
