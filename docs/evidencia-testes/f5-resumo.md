# Evidência F5 — testes de integração com MySQL

Execução: `backend/tests/run-integration.sh` com o MySQL do `docker compose` em estado `healthy`.
Banco: `cartao_sus_test`, recriado a partir de `schema.sql` e `seed.sql` antes de cada teste.
Resultado: **12 testes de integração aprovados** (IT-08 tem dois casos, IT-10 também). Saída em `f5-integracao.txt`.

| ID | Teste | Verificação no banco | Resultado |
|---|---|---|---|
| IT-01 | Cadastro → login → prontuário | Cidadão e usuário gravados; CPF só como hash | Aprovado |
| IT-02 | Usuário com `ATIVO = 0` não loga | Login recusado com 401 | Aprovado |
| IT-03 | Handshake com TOTP válido | Sessão `AUTORIZADO` nova em `TB_SESSAO_ACESSO` | Aprovado |
| IT-04 | Três falhas e uma quarta bloqueada | Alerta `TENTATIVA_INDEVIDA` criado | Aprovado |
| IT-05 | Tensiômetro acima do limiar | Alerta `PRESSAO_CRITICA`; `ORIGEM = IOT` | Aprovado |
| IT-06 | Medição pelo app com JWT | `ORIGEM = APP` | Aprovado |
| IT-07 | Documento criado, listado no handshake e excluído | Linha inserida e removida de `TB_CARTEIRA_METADADOS` | Aprovado |
| IT-08a | Proxy do CNES normaliza a resposta | Resposta do SUS simulada | Aprovado |
| IT-08b | SUS fora do ar vira 502 | Resposta 503 simulada | Aprovado |
| IT-09 | Auditoria paginada | `totalElements` igual ao `COUNT` no banco | Aprovado |
| IT-10a | Sem token: 401 | Envelope `NAO_AUTORIZADO` | Aprovado |
| IT-10b | Cidadão em rota de admin: 403 | Envelope `SEM_PERMISSAO` | Aprovado |

## Verificação adicional

- **Quebra proposital:** removi a gravação da sessão `AUTORIZADA` no acolhimento. O IT-03 falhou (esperado 3, obtido 2), e a alteração foi desfeita em seguida.
- **Testes unitários sem banco:** com o container parado, `dotnet test --filter "Category!=Integracao"` continua verde (`f5-unitarios-sem-banco.txt`).

## Ajuste em relação ao SDD

O IT-07 verifica a inclusão e a exclusão pelo handshake (que lista `documentosDisponiveis`), e não pelo prontuário. O prontuário (E4) não expõe documentos.
