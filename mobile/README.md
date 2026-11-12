# App Android — Cartão SUS Inteligente

App do cidadão em Kotlin, Jetpack Compose (Material 3), MVVM, Retrofit com Gson e Navigation. Consome a API do backend (SDD 7).

## Telas

| Aba | O que faz |
|---|---|
| Início | Cartão digital, alertas com confirmação de entrega |
| Atendimento | Código TOTP de 6 dígitos com contagem regressiva. Se o segredo não estiver salvo, use "Informar código secreto" (o segredo impresso no console da API ao cadastrar) |
| Consultar | Busca de remédio com estoque em São Paulo, unidades por distância, equipamentos e "Ver no mapa" |
| Saúde | Registro de medição, gráfico, alerta crítico e fila offline |
| Meus documentos (atalho no Início) | Anexar PDF ou foto (carterinha, RG, CPF, laudo, cirurgia, receita, consulta) pelo seletor do Android, com permissão de leitura persistente. Só o caminho (URI), o tipo e o título ficam no celular (Room, tabela `documento_local`); nada vai para a API. Avisa se o arquivo foi apagado; permite abrir, editar e remover |

No cadastro, o botão "Salvar neste aparelho" guarda o segredo para o código TOTP funcionar sem digitação.

## Telas da F7

| Tela | O que faz |
|---|---|
| Login | Entra com e-mail e senha. Erro de credenciais, falta de conexão com botão "Tentar de novo" |
| Criar conta | Cadastro com validação por campo. Mostra o segredo TOTP uma única vez, com botão de copiar |
| Início (provisória) | Mostra o nome do cidadão. As telas 3 a 6 entram na F8 |

Ao abrir, se houver sessão salva, o app confirma o token com `GET /api/v1/auth/me` e pula o login.

## Como executar

Pré-requisitos: JDK 17 ou superior, Android SDK com a plataforma 36, e a API no ar (ver [README do backend](../backend/README.md)).

**1. Configure o SDK.** Crie `mobile/local.properties` (não versionado) com o caminho do seu SDK:

```properties
sdk.dir=/caminho/para/Android/sdk
```

**2. Rode a API** na pasta `cartao-sus/` (banco e API no ar, conforme o backend README).

**3. Gere e instale o app** na pasta `cartao-sus/mobile/`:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Ou abra a pasta `mobile/` no Android Studio e clique em Run.

**4. Rodar no emulador.** O emulador acessa o computador pelo endereço `10.0.2.2`. A URL da API está em `app/build.gradle.kts` (campo `API_URL`).

**5. Rodar em celular físico.** Troque `API_URL` pelo IP do computador na rede local (por exemplo, `http://192.168.0.10:8080/`) e inclua esse domínio em `res/xml/network_security_config.xml` se for usar HTTP. Celular e computador precisam estar na mesma rede.

## Testes

```bash
./gradlew testDebugUnitTest
```

Os testes são unitários (JUnit 4): validação de formulário, mapeamento de erros da API e leitura da resposta de login. Não precisam de emulador nem de API.

## Segurança

- A sessão (token e nome) fica em `SharedPreferences` privado ao app. **Não é criptografada** (limitação registrada no SDD 7.4).
- HTTP em texto claro só é permitido para `10.0.2.2` e `localhost` (`network_security_config.xml`).
- Nenhuma chave ou segredo fica no código.
