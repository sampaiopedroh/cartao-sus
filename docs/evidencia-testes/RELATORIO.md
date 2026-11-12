# Relatório final de testes — Cartão SUS Inteligente

Execução consolidada da F9 (11/11/2026), atualizada na F10 com a revisão de UX, documentos e compartilhamento (casos MT-15 a MT-18), com banco limpo recriado a partir de `schema.sql`, `seed.sql` e `gerar_chaves.py`. API e aplicativo na versão final do repositório.

## Resumo

| Tipo | Quantidade | Resultado | Como executar | Evidência |
|---|---|---|---|---|
| Unitários (backend) | 12 (UT-01 a UT-12) | 12 PASS | `dotnet test --filter "Category!=Integracao"` | `f9-unitarios-contrato.txt` |
| Contrato (backend) | 8 (CT-01 a CT-08) | 8 PASS | mesmo comando acima | `f9-unitarios-contrato.txt` |
| Integração (MySQL) | 12 casos (IT-01 a IT-10, com IT-08 e IT-10 em dois casos) | 12 PASS | `backend/tests/run-integration.sh` | `f9-integracao.txt` |
| Manuais | 18 (MT-01 a MT-18) | 18 PASS | roteiro manual (abaixo) | `mt-XX-*`, `ux-*` |
| Unitários (aplicativo Android) | 50 | 50 PASS | `./gradlew testDebugUnitTest` | saída do Gradle (`mobile/`) |

**Resultado geral: 100 de 100 casos aprovados, sem falhas** (12 + 8 + 12 + 18 + 50).

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
| MT-15 | Botão "Início" volta à tela inicial a partir de Saúde, Consultar e Atendimento; Voltar do sistema volta ao Início | app (emulador) | PASS | `ux-resumo.md`, `ux-inicio.png` |
| MT-16 | Anexar documento pelo seletor do Android; o documento continua na lista depois de reiniciar o app e abre no visualizador | app (emulador) | PASS | `ux-documentos.png` |
| MT-17 | Documento cujo arquivo foi apagado: aviso na lista e sem queda do app ao compartilhar | app (emulador) | PASS | `ux-documentos-arquivo-ausente.png` |
| MT-18 | Compartilhamento só depois de o atendente validar o código (terminal responde `AUTORIZADO`); sem validação o botão não aparece | app e terminal | PASS | `ux-compartilhar-liberado.png`, `ux-compartilhar-android.png` |

## Problemas encontrados durante o projeto

| Fase | Problema | Correção |
|---|---|---|
| F10 | Avisos de vulnerabilidade em `MySqlConnector` 2.5.0 (moderada) e `System.Security.Cryptography.Pkcs` 10.0.9 (alta) | Atualizados para 2.6.2 e 10.0.15; `dotnet list package --vulnerable` sem alertas e testes verdes |
| F10 | A coleção do Postman era só a exportação do Swagger e não rodava de ponta a ponta (sem token, sem chave de dispositivo e sem variáveis de caminho) | Coleção reescrita em ordem, com 26 requisições e 39 testes; executada no Newman sem falhas (`f10-newman.txt`) |
| F10 | O app fechava ao compartilhar um documento cujo arquivo o Android não permitia mais ler (`SecurityException`) | O app confere cada arquivo antes, ignora os ilegíveis e avisa o usuário (MT-17) |
| F10 | A aba "Início" não voltava à tela inicial | Navegação interna com Início como destino inicial (MT-15) |
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

- Compartilhamento de documentos sem NFC real: a ideia é compartilhar as informações por NFC, mas, pelas limitações da simulação (sem leitor NFC físico nem HCE no emulador), o grupo optou por liberar o compartilhamento pelo seletor do Android depois que o atendente valida o código (ver `docs/adr/ADR-nfc-compartilhamento.md`).
- O segredo TOTP fica no SharedPreferences do aplicativo, sem criptografia (SDD 7.4).
- O anti-reuso do TOTP é em memória: vale para uma única instância da API.
- A consulta de remédio usa o estoque de São Paulo; a API do BNAFAR não informa total de registros, então o `totalElements` é aproximado.
- Cada pressão gera um alerta por leitura: sístole e diástole acima do limiar geram dois alertas.
- As consultas Q2 e Q7 usam `NOW()` do banco, que está em UTC; a janela pode deslocar 3 horas em relação à hora local.
- Testes manuais MT-05, MT-07, MT-08, MT-12 e MT-13 foram executados com o emulador Android; a versão física não foi testada.

## Segurança das evidências

Nenhum segredo, chave, senha ou token aparece nas evidências. As senhas de demonstração, as chaves de dispositivo e os segredos TOTP foram lidos do `.env` ou do console do seeder e omitidos das saídas.
