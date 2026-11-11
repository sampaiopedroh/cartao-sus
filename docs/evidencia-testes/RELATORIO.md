# Relatório final de testes — Cartão SUS Inteligente

Execução consolidada da F9 (11/11/2026), com banco limpo recriado a partir de `schema.sql`, `seed.sql` e `gerar_chaves.py`. API e aplicativo na versão final do repositório.

## Resumo

| Tipo | Quantidade | Resultado | Como executar | Evidência |
|---|---|---|---|---|
| Unitários (backend) | 12 (UT-01 a UT-12) | 12 PASS | `dotnet test --filter "Category!=Integracao"` | `f9-unitarios-contrato.txt` |
| Contrato (backend) | 8 (CT-01 a CT-08) | 8 PASS | mesmo comando acima | `f9-unitarios-contrato.txt` |
| Integração (MySQL) | 12 casos (IT-01 a IT-10, com IT-08 e IT-10 em dois casos) | 12 PASS | `backend/tests/run-integration.sh` | `f9-integracao.txt` |
| Manuais | 14 (MT-01 a MT-14) | 14 PASS | roteiro manual (abaixo) | `mt-XX-*` |
| Unitários (aplicativo Android) | 30 | 30 PASS | `./gradlew testDebugUnitTest` | saída do Gradle (`mobile/`) |

**Resultado geral: 76 de 76 casos aprovados, sem falhas.**

## Testes manuais (MT)

| ID | Caso | Onde foi executado | Resultado | Evidência |
|---|---|---|---|---|
| MT-01 | Login válido | API e app | PASS | `mt-01-login-valido.txt`, `mt-01-app-login.png` |
| MT-02 | Login com senha errada | API e app | PASS | `mt-02-senha-errada.txt`, `mt-02-app-senha-errada.png` |
| MT-03 | Payload malformado | API | PASS | `mt-03-payload-malformado.txt` |
| MT-04 | Cadastro duplicado (409) | API | PASS | `mt-04-cadastro-duplicado.txt` |
| MT-05 | Handshake com TOTP válido | API e app (código da tela Token enviado ao terminal) | PASS | `mt-05-handshake-valido-api.txt`, `mt-05-handshake-valido-app.txt`, `mt-05-app-token.png` |
| MT-06 | Handshake com código de 5 minutos atrás | API | PASS | `mt-06-codigo-antigo.txt` |
| MT-07 | Três falhas, bloqueio e alerta | API e app (alerta visível no Dashboard) | PASS | `mt-07-bloqueio-alerta-api.txt`, `mt-07-08-app-alertas.png` |
| MT-08 | Pressão crítica pelo tensiômetro | API e app (alerta visível no Dashboard) | PASS | `mt-08-pressao-critica-tensiometro.txt`, `mt-07-08-app-alertas.png` |
| MT-09 | Tentativas de SQL injection | API | PASS | `mt-09-sql-injection.txt` |
| MT-10 | Prontuário de outro cidadão (403) | API | PASS | `mt-10-prontuario-de-outro.txt` |
| MT-11 | CRUD de documento, verificado pelo handshake | API | PASS | `mt-11-crud-documento.txt` |
| MT-12 | Medicamento no BNAFAR | API e app | PASS | `mt-12-medicamento-bnafar.txt`, `mt-12-app-medicamento.png` |
| MT-13 | App sem conexão e sincronização da fila | app (API parada e depois restaurada) | PASS | `mt-13-app-offline.png`, `mt-13-app-sincronizado.png` |
| MT-14 | Auditoria por perfil | API | PASS | `mt-14-auditoria-perfil.txt` |

## Problemas encontrados durante o projeto

| Fase | Problema | Correção |
|---|---|---|
| F3 | O prontuário (E4) não mostrava documentos, diferente do SDD | Teste de integração IT-07 verificou pelo handshake (E3) |
| F4 | `AuthService` dependia do banco e impedia testes unitários | Refatorado; cadastro com um único `SaveChanges` |
| F5 | Teste rodou sobre build antigo e falhou por engano | Recompilação; resultado revalidado |
| F6 | Fuso horário: container MySQL em UTC e API em UTC-3, afetando `NOW()` nas consultas Q2 e Q7 | Registrado como limitação |
| F7 | Pressionar o botão de login com campo vazio retornava 400 sem mensagem clara ao usuário | Validação de formulário no app antes do envio |
| F8 | A tela Consulta SUS não rolava; os resultados ficavam fora da tela | Adicionada rolagem |
| F8 | Crash ao abrir a Consulta SUS: lista rolável aninhada em coluna rolável | Substituída por coluna simples |
| F9 | Dashboard sem botão "Sair", exigido pelo plano da F8 | Botão adicionado e verificado |
| F9 | Script de teste do MT-07 comparava contagem absoluta de alertas, ignorando o alerta de demonstração já existente | Comparação antes e depois do caso |
| F9 | Teste MT-11 reutilizava janela de código já aceita | Espera da janela seguinte antes da segunda leitura |

## Limitações conhecidas

- O segredo TOTP fica no SharedPreferences do aplicativo, sem criptografia (SDD 7.4).
- O anti-reuso do TOTP é em memória: vale para uma única instância da API.
- A consulta de remédio usa o estoque de São Paulo; a API do BNAFAR não informa total de registros, então o `totalElements` é aproximado.
- Cada pressão gera um alerta por leitura: sístole e diástole acima do limiar geram dois alertas.
- As consultas Q2 e Q7 usam `NOW()` do banco, que está em UTC; a janela pode deslocar 3 horas em relação à hora local.
- Avisos de vulnerabilidade em `MySqlConnector` e `System.Security.Cryptography.Pkcs` (sem correção publicada nas versões usadas).
- Testes manuais MT-05, MT-07, MT-08, MT-12 e MT-13 foram executados com o emulador Android; a versão física não foi testada.

## Segurança das evidências

Nenhum segredo, chave, senha ou token aparece nas evidências. As senhas de demonstração, as chaves de dispositivo e os segredos TOTP foram lidos do `.env` ou do console do seeder e omitidos das saídas.
