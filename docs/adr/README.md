# Registro de decisões de arquitetura (ADR)

Este projeto registra as decisões de arquitetura e de implementação em três níveis:

| Pasta | Conteúdo | Quando consultar |
|---|---|---|
| `docs/sdd/` | Documento de design: `sdd.md` (v1, superado), `sdd-v2.md` e `sdd-v3.md` (vigente) | Visão geral do sistema, stack e decisões (ADR-1 a ADR-10 na seção 2 do v3) |
| `docs/planos/` | Plano de ação de cada fase (F0 a F10), aprovado antes da execução | Por que cada fase foi feita do jeito que foi |
| `docs/referencias/` | Diretrizes do edital e a lista de APIs públicas do SUS | Requisitos da disciplina e fontes de dados |

## Decisões que mudaram ao longo do projeto

| Decisão | Origem | Motivo |
|---|---|---|
| Backend em ASP.NET Core (.NET 10), e não Spring | v2 → v3 | Stack do curso; Spring não é coberto |
| Schema por scripts SQL, sem Flyway | v2 → v3 | Edital pede script `.sql` que roda sem erro |
| Gson, e não kotlinx.serialization | Plano F7 | Biblioteca coberta pelo curso |
| Segredos fora do repositório (hash no banco, `.env`) | v2 → v3 (seção 8.5) | Contradição de segredos corrigida |
| Usuário `cartao_app` com permissão mínima | Plano F2 | Principio do menor privilégio |
| TOTP com anti-reuso em memória | Plano F3 | Suficiente para uma instância; limitação registrada |
| Testes de contrato por snapshot, sem Pact | Plano F4 | Pact não é coberto pelo curso |
| Fila offline com Room | Plano F8 | SDD pede; Room é coberto pelo curso |
| Estoque do BNAFAR em tempo real, sem cópia local; liberação do compartilhamento pela conta, não pelo aparelho | F10 | Limitações aceitas, explicadas em `ADR-limitacoes-aceitas.md` |
| Compartilhar documentos pelo seletor do Android, e não por NFC | Sessão de UX (F10) | A ideia é NFC, mas a simulação não tem leitor nem HCE; ver `ADR-nfc-compartilhamento.md` |

Dados de APIs do SUS conferidos no Swagger oficial: `apis-publicas.md` estava com caminhos errados (ver v3, seção 0.1).
