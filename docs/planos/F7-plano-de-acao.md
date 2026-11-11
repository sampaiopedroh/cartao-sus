# Plano de ação — Fase F7 (App Android: base e telas de acesso)

## Context

A API (F2 a F5) e os sensores (F6) estão prontos. Falta o aplicativo do cidadão, que é o último pilar do MVP e o que o avaliador vai ver primeiro. O edital (Parte 4, 3 pontos) exige 4 ou mais telas funcionais, integração com a API tratando erros de conexão, e usabilidade com feedback nas ações.

A F7 entrega a base do app e as duas primeiras telas do SDD: Login e Cadastro. As telas 3 a 6 ficam para a F8. Critério do SDD v3 (seção 10, F7): o app loga e cadastra contra a API local, pelo emulador em `10.0.2.2:8080`.

Estimativa do SDD: 8 horas.

Ambiente verificado: JDK 21, Android SDK com plataforma 36 e imagem de sistema `google_apis` 36, e Android Studio instalado. Não há Gradle no PATH.

## Decisões a confirmar

1. **Gson em vez de kotlinx.serialization (recomendado).** O SDD v3 cita kotlinx.serialization, mas o material do curso usa Gson para consumir APIs (fase 05-05). Como o edital proíbe tecnologia fora do conteúdo, troco para Gson. É uma mudança no SDD, que registro na seção 0.1.
2. **Gradle via Homebrew (recomendado).** Para gerar o wrapper do projeto é preciso o Gradle instalado. Proponho `brew install gradle` uma vez, e depois o projeto usa o `gradlew` versionado, sem depender da instalação local. A alternativa é usar o Gradle que vem com o Android Studio.

Não há decisão nova sobre o armazenamento de sessão: segue a seção 7.4 do SDD v3, com `SharedPreferences` privado ao app e a limitação registrada.

## Escopo (passo a passo)

### Passo 1 — Projeto Android
- Pasta `cartao-sus/mobile/`, com o pacote `br.edu.cartaosus` (o SDD v3 usa esse nome).
- Kotlin 2.0.x, Android Gradle Plugin 8.x compatível com a versão do Gradle escolhida, Jetpack Compose com Material 3, `minSdk 26`, `compileSdk 36`.
- Dependências do curso: Compose, ViewModel e Navigation, Retrofit com Gson, OkHttp (logging só em debug), JUnit 4 nos testes.
- `gradle.properties` e `local.properties` (este último no `.gitignore`).

### Passo 2 — Configuração de rede
- Base URL em `BuildConfig`: `http://10.0.2.2:8080/` (emulador) e a variável configurável para dispositivo físico.
- `network_security_config.xml`: tráfego em texto claro permitido **somente** para `10.0.2.2` e `localhost`, porque o Android 9+ bloqueia HTTP por padrão. O restante continua exigindo HTTPS.
- Permissão `INTERNET`.

### Passo 3 — Camada de dados
- `SusApi` (interface Retrofit) com `POST /api/v1/auth/login`, `POST /api/v1/auth/cadastro` e `GET /api/v1/auth/me`.
- DTOs espelhando os contratos da API (mesmos nomes em camelCase, como no SDD 5.3).
- `SessionStore`: guarda o token e o `idCidadao` em `SharedPreferences` privado, com limpeza no logout.
- Interceptor do OkHttp que envia `Authorization: Bearer` quando há token.
- Mapeamento de erros: `IOException` vira "Sem conexão. Verifique sua internet."; o envelope `ErrorResponse` vira a mensagem do campo `mensagem`; 401 limpa a sessão.

### Passo 4 — Estado e lógica (MVVM)
- `LoginViewModel` e `CadastroViewModel` com `StateFlow` e um `sealed class UiState` (carregando, sucesso, erro, pronto).
- Validação de formulário no ViewModel: e-mail válido, senha com 8 caracteres ou mais, CPF com 11 dígitos, CNS com 15 dígitos, nome com 3 caracteres ou mais, data no passado. Os erros aparecem por campo, como no SDD 7.2.
- Regras de validação ficam em funções puras, testáveis por JUnit (sem emulador).

### Passo 5 — Telas (Compose, Material 3)
- **Tela 1, Login:** campos e-mail e senha (oculta), botão "Entrar" com indicador de carregamento, link "Criar conta". Erro 401 vira snackbar "Credenciais inválidas". Sem conexão vira snackbar com botão "Tentar de novo".
- **Tela 2, Cadastro:** login, senha, CPF, CNS, nome e data de nascimento. Erros por campo. Em caso de sucesso, mostra o segredo TOTP **uma única vez** em uma caixa com opção de copiar, com a instrução de guardar no app. Conflito 409 vira "Já existe conta com este CPF ou CNS".
- **Tela de entrada (placeholder):** após o login, uma tela inicial simples com o nome do cidadão. Ela será substituída na F8 pelo Dashboard.
- Navegação com `NavHost`: Login → Cadastro → Login, e Login → Início.

### Passo 6 — Feedback e estados
- Todo botão que chama a rede desabilita enquanto carrega.
- Todas as telas têm os três estados: carregando, sucesso e erro com ação de tentar de novo.
- Ao iniciar, se houver token salvo, o app pula o Login e chama `GET /auth/me`. Se a API recusar, volta ao Login.

### Passo 7 — Testes
- JUnit 4 nos validadores de formulário e no mapeamento de erros.
- Teste de DTO: a resposta do login é lida corretamente pelo Gson.
- Nada de emulador nos testes unitários.

### Passo 8 — Documentação
- `mobile/README.md`: como abrir no Android Studio, como rodar no emulador e como configurar a URL para dispositivo físico.
- README da raiz: status da F7.

## Verificação

1. `./gradlew assembleDebug` gera o APK sem erros.
2. `./gradlew testDebugUnitTest` passa.
3. Emulador (imagem `google_apis` 36) com a API no host: o app abre na tela de login.
4. Login com a Maria (senha de demo): entra na tela inicial com o nome dela. Confirmar com `GET /auth/me`.
5. Login com senha errada: snackbar "Credenciais inválidas".
6. Cadastro novo: mostra o segredo TOTP uma vez. Cadastro repetido: mensagem de conflito.
7. Com a API desligada: snackbar de sem conexão, sem travar o app.
8. Girar a tela no meio do cadastro não perde os dados digitados.
9. Capturas de tela das telas 1 e 2 e dos erros, salvas em `docs/evidencia-testes/f7-*`.

**Critério de pronto da F7:** itens de 1 a 9 passam, `mobile/README.md` permite rodar o app do zero, e as capturas estão na evidência.

## Riscos

| Risco | Mitigação |
|---|---|
| Versões incompatíveis de AGP, Gradle e Kotlin | Versões fixadas no `libs.versions.toml`; teste de build logo no início |
| Emulador lento ou sem a imagem certa | Imagem `google_apis` 36 já instalada; se o Mac for Apple Silicon, usar a variante `arm64` |
| HTTP bloqueado no Android 9+ | `network_security_config` restrito a `10.0.2.2` e `localhost` |
| Segredo TOTP exibido e perdido | Caixa com botão de copiar e aviso de que aparece uma vez |
| Escopo crescer para as telas 3 a 6 | Ficam fora da F7; a tela inicial é só um placeholder |

## Arquivos a criar

- `cartao-sus/mobile/` (projeto Gradle: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradlew`, `gradle/wrapper/`)
- `mobile/app/src/main/java/br/edu/cartaosus/` (`data/`, `ui/login/`, `ui/cadastro/`, `ui/inicio/`, `navegacao/`)
- `mobile/app/src/main/res/xml/network_security_config.xml`
- `mobile/app/src/test/java/br/edu/cartaosus/` (validadores e mapeamento de erros)
- `mobile/README.md` e `docs/evidencia-testes/f7-*.png` com `f7-resumo.md`

Arquivos existentes que mudam: `cartao-sus/README.md` (status da F7), `.gitignore` (confirmar `local.properties`, `.gradle/` e `build/` do módulo `mobile/`) e `sdd-v3.md` (troca de Gson na seção 7.1; registro em nova seção de ajustes).

Nenhuma mudança na API é prevista.

## Perguntas em aberto

1. **Gson no lugar de kotlinx.serialization?** Recomendo sim, pelo conteúdo do curso.
2. **Instalar o Gradle com Homebrew para gerar o wrapper?** Recomendo sim.
