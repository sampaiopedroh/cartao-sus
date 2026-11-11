# Evidência F8 — telas 3 a 6 e modo offline, no emulador contra a API local

Emulador: Pixel 7, Android 16 (Google APIs). App: `app-debug.apk`. API em `http://10.0.2.2:8080`.

| Verificação | Resultado observado |
|---|---|
| Dashboard (tela 3) | Lista os alertas da conta do João, com "Confirmar" nos pendentes. Confirmar um alerta chega à API (status ENTREGUE, verificado por GET /alertas) |
| Token TOTP (tela 4) | Sem segredo salvo, a tela pede "Informar segredo". Com o segredo colado, mostra o código de 6 dígitos com contagem regressiva de 30 s |
| Código do app aceito no handshake | O código exibido no celular foi enviado ao terminal NFC (`terminal_nfc.py --token`) e a API respondeu 200 AUTORIZADO para o João |
| Consulta SUS (tela 5) | Busca "insulina" encontra Insulina asparte; o estoque em São Paulo mostra quantidade 13 e posição 2026-11-08. Unidades próximas ordenadas pela distância (ex.: Hospital Ignácio Proença, 5,0 km) |
| Sinais (tela 6) | Registrar pressão sistólica de 185 mostra o banner "Alerta crítico — Procure a UPA mais próxima" (E6 com alertaDisparado=true) |
| Modo offline | Com a API parada, a medição de 130 foi guardada no aparelho ("Salvo no aparelho. Será enviado quando houver internet." e "Aguardando envio: 1") |
| Sincronização | Ao reabrir o app com a API de volta, a fila foi enviada: a medição apareceu no histórico (APP) e a fila zerou |

## Problemas encontrados e corrigidos durante a verificação

- **Consulta SUS cortada:** a tela não rolava, e os resultados abaixo do item escolhido ficavam fora da área visível. Corrigido com rolagem.
- **Crash ao abrir a Consulta:** a lista de unidades (LazyColumn) estava dentro de uma coluna com rolagem, o que o Compose não aceita. Substituída por coluna simples.
- O teclado cobria a barra de abas durante a automação de testes; não é defeito do app.

## Limitações conhecidas

- O segredo TOTP fica no SharedPreferences privado do app, sem criptografia (SDD 7.4).
- A distância usa um ponto de referência fixo (centro de São Paulo), sem permissão de localização.
- Cada pressão gera um alerta por leitura; sístole e diástole acima do limiar geram dois alertas (já registrado na F6).
