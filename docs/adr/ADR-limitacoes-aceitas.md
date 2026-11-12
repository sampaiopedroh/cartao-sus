# ADR: limitações aceitas no MVP

**Status:** adotada em 12/11/2026 (F10)

Este registro explica dois comportamentos que o avaliador pode notar ao testar o projeto: **por que acontecem**, **por que não vale a pena mudar agora** e **por que não diminuem a qualidade do MVP**.

---

## 1. O estoque de remédios do BNAFAR muda com o tempo

### Por que acontece

A consulta de remédios (aba **Consultar** do app e `GET /api/v1/sus/medicamentos`) não usa dados guardados no nosso banco. A API repassa a pergunta, em tempo real, para a **API pública do Ministério da Saúde** (BNAFAR, `apidadosabertos.saude.gov.br`), filtrando pelo município de São Paulo (IBGE 355030).

O BNAFAR é atualizado pelas próprias farmácias e unidades do SUS. Por isso:

- um remédio que tinha estoque no dia do teste pode aparecer sem estoque em outro dia;
- a quantidade, a data da posição e o estabelecimento mudam;
- se a API do governo estiver fora do ar, a nossa API responde `502 FALHA_ORIGEM` e o app mostra "Serviço do SUS indisponível no momento", com o botão "Tentar de novo".

### Por que não é um defeito e não há o que corrigir no código

- **É o comportamento correto.** O objetivo do produto é mostrar o estoque **real**. Uma lista fixa daria a mesma resposta todo dia, mas estaria errada para o cidadão.
- **O código trata todos os casos.** Há tela de lista vazia ("Nenhum estoque encontrado nesta cidade"), tela de erro com "Tentar de novo" e tempo limite de 5 segundos para não travar o app.
- **O que os testes verificam é o formato, não a quantidade.** O caso MT-12 e a coleção do Postman conferem que a resposta chega no formato combinado (lista paginada e normalizada). A data de cada execução fica registrada na evidência.

### Por que não vale a pena mudar agora

A alternativa seria copiar o estoque para o nosso banco (um "cache") e atualizar de tempos em tempos. Isso traria:

- dados **desatualizados** entre uma cópia e outra, o oposto do que o cidadão precisa;
- um serviço agendado e mais tabelas, fora do escopo e do conteúdo da disciplina;
- mais risco perto do prazo de entrega, sem ganho para a avaliação.

### Por que não tira a qualidade do MVP

O MVP prova o que precisa provar: a integração com uma fonte oficial do SUS funciona de ponta a ponta, com dados reais, erros tratados e testes. A variação do estoque é **evidência de que os dados são reais**, não falha.

### Se um dia for preciso

Para uma demonstração gravada, escolher na hora um remédio que tenha estoque (a aba Consultar lista o catálogo). Em produção, um cache curto (por exemplo, 15 minutos) reduziria chamadas ao governo sem perder atualidade.

---

## 2. O botão "Compartilhar documentos" também aparece se outro aparelho da mesma conta for atendido

### Por que acontece

Depois que o atendente valida o código, o app libera o botão "Compartilhar documentos". Para saber que o código foi validado, o app consulta, a cada 5 segundos, o **histórico de atendimentos da conta** (`GET /api/v1/cidadao/{id}/acessos`). Quando aparece um atendimento `AUTORIZADO` novo, o botão aparece.

O histórico é **da conta**, não do aparelho. A API registra quem foi atendido (o cidadão) e onde (a unidade), mas não registra **qual celular** gerou o código. Então, se a mesma pessoa estiver logada em dois celulares e for atendida usando um deles, o outro também vê o atendimento novo e mostra o botão.

### Por que não é um risco de segurança

- **Os dois aparelhos são do mesmo dono.** Só abre o app quem sabe o e-mail e a senha da conta. O "outro aparelho" é da própria pessoa.
- **Nada sai do celular sozinho.** O botão só abre a lista. A pessoa ainda precisa escolher os documentos e confirmar o envio no próprio aparelho.
- **Os arquivos ficam em cada celular.** Um aparelho não tem acesso aos arquivos do outro.
- **A liberação vale para um único envio.** Depois de compartilhar, um novo envio exige outro código validado.

### Por que não vale a pena mudar agora

Ligar a autorização a um aparelho específico exigiria mudar várias partes de uma vez:

- **API:** criar um identificador do aparelho, guardar esse identificador junto com o atendimento (nova coluna em `TB_SESSAO_ACESSO`) e um endpoint para o app perguntar "este aparelho foi autorizado?".
- **Terminal:** enviar o identificador do aparelho junto com o código, o que no mundo real viria do toque por NFC, que neste protótipo é simulado (ver `ADR-nfc-compartilhamento.md`).
- **App:** gerar e guardar o identificador e trocar a consulta ao histórico.
- **Testes:** refazer os testes de contrato, de integração e os casos manuais do acolhimento.

É uma mudança no contrato da API, no banco e no terminal para um caso raro (a mesma pessoa logada em dois celulares) que **não expõe dados a terceiros**. A poucos dias da entrega, o risco de quebrar uma parte que já está testada é maior que o benefício.

### Por que não tira a qualidade do MVP

O fluxo principal funciona e foi testado: sem código validado, o botão não aparece; com código validado, aparece; depois de um envio, some. A regra de segurança que importa, **nenhum documento é compartilhado sem a ação do próprio cidadão**, continua valendo em todos os casos.

### Caminho futuro

Com NFC real, o celular passa a ser o próprio cartão (HCE) e o terminal identifica o aparelho no toque. A autorização fica ligada ao celular que encostou no terminal, e esta limitação desaparece junto com a simulação.
