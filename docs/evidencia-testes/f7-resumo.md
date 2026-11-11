# Evidência F7 — app Android no emulador contra a API local

Emulador: Pixel 7, Android 16 (Google APIs, arm64), 4 GB de partição de dados.
App: `app-debug.apk` (build do Gradle 8.11.1 e AGP 8.9.1). API em `http://10.0.2.2:8080` (computador de desenvolvimento).

| Fluxo | Resultado observado | Captura |
|---|---|---|
| Abrir o app sem sessão | Tela de login | `f7-01` |
| Senha errada | Mensagem "Credenciais inválidas." (API 401) | `f7-02` |
| Login correto (Maria) | Tela inicial "Olá, Maria Souza Lima" (API 200) | `f7-03`, `f7-06` |
| Cadastro novo | Conta criada (API 201); segredo TOTP exibido uma vez, com botão copiar | `f7-04` |
| API desligada | Mensagem "Sem conexão. Verifique sua internet." com botão "Tentar de novo" | `f7-05` |
| Reabrir o app com sessão salva | Entra direto na tela inicial, depois de confirmar o token com `GET /auth/me` (200) | `f7-07` |
| Cadastro com CPF já usado | API 409; tela permanece com os dados digitados | `f7-08` |

Logs do cliente (logcat) confirmam as chamadas: login 200, `me` 200, cadastro 201 e 409, e a falha de conexão quando a API estava parada.

## Observações

- O cadastro com CPF repetido mostra o formulário preenchido; a mensagem da API aparece como aviso, mas a captura foi feita depois que o aviso já tinha sumido. A mensagem está coberta pelo teste unitário `conflitoUsaAMensagemDaApi`.
- Durante os testes, houve um login com 400 porque o campo de e-mail estava vazio após reiniciar o app. A validação do app e da API recusou o envio, como esperado.
