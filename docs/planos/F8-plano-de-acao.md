# Plano de ação — Fase F8 (App Android: telas 3 a 6 e modo offline)

## Context

A F7 entregou a base do app e as telas de acesso. Falta o conteúdo que justifica o app: o cartão digital, o código TOTP que o cidadão mostra no balcão, a consulta de remédios e unidades, e a carteira de sinais vitais.

O edital (Parte 4) pede 4 ou mais telas funcionais, integração com a API tratando erros de conexão, e feedback nas ações. O SDD (seção 7.2) pede que cada tela tenha os três estados: carregando, sucesso (ou vazio) e erro com ação de tentar de novo. A F8 também entrega o gerador de TOTP no aparelho e a fila offline de medições.

Critério do SDD v3 (seção 10, F8): telas completas com os três estados; o código TOTP gerado no app é aceito pelo handshake.

Estimativa do SDD: 10 horas.

## Decisões a confirmar

1. **Onde fica o segredo TOTP no aparelho (recomendado: salvar após o cadastro, com opção de importar).** O segredo aparece uma única vez, na tela de cadastro. Para o código TOTP funcionar, o app precisa guardá-lo. Proponho: na tela de cadastro, um botão "Salvar neste aparelho" grava o segredo no `SharedPreferences`. Para contas criadas antes ou em outro aparelho (como as de demonstração do seed), a tela do código TOTP oferece "Informar segredo" com um campo para colar o segredo impresso no console da API. Sem isso, o código não pode ser gerado.
2. **Fila offline com Room (recomendado).** O SDD pede Room para guardar medições quando não há internet e enviá-las ao reconectar. Room está no material do curso (fase 04-07). Proponho uma tabela `medicao_pendente` e envio ao abrir a tela de sinais e ao voltar a conexão, sem WorkManager.

Demais escolhas, já adotadas no plano: gráfico de linha desenhado com Canvas do Compose (sem biblioteca de gráficos, que não é do curso); "Ver no mapa" abre o aplicativo de mapas por intent `geo:`, sem chave de API; distância até as unidades calculada pela fórmula de Haversine, a partir de um ponto de referência fixo (centro de São Paulo), sem pedir permissão de localização.

## Escopo (passo a passo)

### Passo 1 — Camada de dados
- Retrofit (`SusApi`) com E4, E5, E6, E10, PUT de alerta entregue, `GET /cidadao/{id}/acessos`, E11, E12, `GET /sus/medicamentos/catalogo`, E13.
- DTOs espelhando os contratos da API (SDD 5.3). Campos de paginação em `PaginaResposta`.
- `SessionStore` ganha o `idCidadao` e o segredo TOTP (decisão 1).
- Mapeamento de erros da F7 reaproveitado. 502 vira "Serviço do SUS indisponível no momento."

### Passo 2 — Gerador de TOTP (`TotpGenerator`)
- HMAC-SHA1, 6 dígitos, passo de 30 s (RFC 6238), a partir do segredo Base32.
- Mesmo algoritmo do servidor, validado com o vetor oficial da RFC (`287082` no instante 59 s).
- Função para o tempo restante do passo, usada na contagem regressiva.

### Passo 3 — Tela 3: Dashboard (cartão digital)
- Cartão com nome, CNS e um ícone de cartão.
- Atalhos para Token, Consulta SUS e Sinais.
- Cards de alertas recentes (E10 com status `PENDENTE`). Alerta crítico em destaque vermelho e botão "Confirmar" (PUT, marca `ENTREGUE`).
- Aviso de receita a vencer: alerta `RECEITA_VENCENDO`.
- Estados: carregando (esqueleto), vazio ("Nenhum alerta por enquanto") e erro com "Tentar de novo".

### Passo 4 — Tela 4: Token TOTP (atendimento)
- Código de 6 dígitos gerado no aparelho, com barra de progresso de 30 s e contagem regressiva.
- Ao chegar a zero, o código é recalculado sem recarregar a tela.
- Botão "Copiar" (com aviso de que o código vale por 30 s).
- Histórico dos últimos acessos (E cidadão `/acessos`): data, unidade e status.
- Sem segredo salvo: a tela mostra "Informar segredo" (decisão 1), e nunca gera código sem ele.
- Erro de rede não afeta o código: ele é calculado localmente.

### Passo 5 — Tela 5: Consulta SUS
- Campo de busca de medicamento por nome. Lista do catálogo (E12 do catálogo local), com seleção que consulta o estoque em São Paulo (E12 com `municipio=355030`).
- Resultados em cards: nome, quantidade em estoque, data da posição e estabelecimento.
- Aba ou seção de unidades: lista de E13 ordenada pela distância até o ponto de referência. Botão "Ver no mapa" abre `geo:` com as coordenadas da unidade.
- Equipamentos por unidade (E11) ao tocar em uma UBS ou UPA.
- Estados: carregando, vazio ("Nenhum estoque encontrado nesta cidade") e erro 502 com "Tentar de novo".

### Passo 6 — Tela 6: Sinais vitais
- Registro manual: tipo (pressão sistólica, diastólica, glicemia, peso), valor e data.
- Gráfico de linha desenhado com Canvas, para pressão ou glicemia, com os dados de E5 e eixo de datas.
- Alerta crítico (E6 devolve `alertaDisparado`): banner vermelho com "Ver UPA mais próxima".
- Modo offline (decisão 2): se o envio falhar por falta de conexão, a medição vai para a tabela `medicao_pendente`, com aviso "Salvo no aparelho. Será enviado quando houver internet." A tela lista as pendentes e tenta enviá-las ao abrir e ao voltar a conexão.
- Estados: carregando, vazio ("Nenhuma medição registrada") e erro.

### Passo 7 — Persistência offline (Room)
- Entidade `MedicaoPendente` (tipo, valor, data, tentativas).
- DAO com inserção, listagem e remoção.
- Envio em lote: cada item enviado é removido só depois do 201. Falha mantém o item.

### Passo 8 — Navegação e rodapé
- Barra inferior com Início, Token, Consulta e Sinais. Início vira o Dashboard (substitui o placeholder da F7).
- Sair limpa a sessão, o segredo e as pendentes do usuário anterior (pergunta antes se houver pendentes).

### Passo 9 — Testes
- JUnit: `TotpGenerator` (vetor RFC e tempo restante), distância de Haversine, escala do gráfico e ordenação das unidades.
- Mapeamento de respostas de E5, E10 e E12 com Gson.
- Sem emulador nos testes unitários. O Room é testado no emulador (fluxo offline).

### Passo 10 — Documentação
- `mobile/README.md`: as telas da F8, o passo de informar o segredo TOTP, e a limitação de uma única instância do TOTP no servidor.
- README da raiz: status da F8.

## Verificação

1. `./gradlew assembleDebug` e `./gradlew testDebugUnitTest` passam.
2. Dashboard: alertas da Joao aparecem; confirmar um alerta crítico e ver o status mudar na API (E10).
3. Token TOTP: o código na tela bate com o gerado pelo `iot/terminal_nfc.py --app` (mesmo segredo); a contagem regressiva recalcula ao zerar.
4. Handshake real: digitar no terminal o código mostrado no app e receber `AUTORIZADO`.
5. Consulta SUS: buscar "insulina" retorna asparte com estoque; tocar em uma UBS mostra equipamentos; "Ver no mapa" abre o aplicativo de mapas.
6. Sinais: registrar pressão 185/115 mostra o banner; o gráfico exibe os pontos de E5.
7. Offline: desligar a rede, registrar uma medição (fica pendente, com aviso); religar a rede e confirmar que a medição chegou à API (E5) e saiu da fila.
8. Os três estados em cada tela (carregando, vazio, erro), com capturas.
9. Capturas e logs salvos em `docs/evidencia-testes/f8-*`.

**Critério de pronto da F8:** itens de 1 a 9 passam, todas as telas têm os três estados, e o código TOTP do app foi aceito pelo handshake real.

## Riscos

| Risco | Mitigação |
|---|---|
| Segredo TOTP perdido ou não salvo no aparelho | Decisão 1 e aviso na tela de cadastro; "Informar segredo" como saída |
| Código TOTP fora de sincronia com o relógio do aparelho | Usar o relógio em UTC e mostrar o tempo restante; o servidor aceita ±1 passo |
| Estoque do SUS vazio ou API lenta | Estados vazio e erro; timeout de 5 s do servidor já existe |
| Fila offline enviando duplicado | Item só sai da fila após o 201; um identificador local impede envio repetido |
| Gráfico com poucos pontos | Mensagem de "poucos registros" abaixo de 2 pontos |
| Escopo crescer | Equipamentos do CNES e busca por nome são as partes que podem ser cortadas primeiro |

## Arquivos a criar

- `mobile/app/src/main/java/br/edu/cartaosus/data/` (novos DTOs, `SusApi` ampliado, `MedicaoDao`, banco Room)
- `mobile/app/src/main/java/br/edu/cartaosus/security/TotpGenerator.kt`
- `mobile/app/src/main/java/br/edu/cartaosus/domain/` (`Distancia.kt`, `Grafico.kt`)
- `mobile/app/src/main/java/br/edu/cartaosus/ui/dashboard/`, `ui/totp/`, `ui/consulta/`, `ui/sinais/`
- `mobile/app/src/test/java/br/edu/cartaosus/` (TOTP, distância, gráfico, DTOs)
- `docs/evidencia-testes/f8-*.png` e `f8-resumo.md`

Arquivos existentes que mudam: `navegacao/NavegacaoApp.kt` (rotas e barra inferior), `ui/cadastro/CadastroScreen.kt` (botão "Salvar neste aparelho"), `data/SessionStore.kt` (segredo e id do cidadão), `app/build.gradle.kts` (Room com KSP) e `gradle/libs.versions.toml`.

Nenhuma mudança na API é prevista.

## Perguntas em aberto

1. **Salvar o segredo TOTP no aparelho após o cadastro, com opção de informar manualmente? (recomendado)**
2. **Fila offline com Room, como o SDD pede? (recomendado)**
