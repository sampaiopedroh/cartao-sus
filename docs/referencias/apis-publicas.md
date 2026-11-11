
1. Equipamentos por Unidade de Saúde (CNES)

A API pública e gratuita que retorna a listagem de equipamentos é fornecida pelo portal oficial da API de Dados Abertos do SUS.
• URL Base: https://apidadosabertos.saude.gov.br
• Endpoint (GET): /assistencia-a-saude/cnes-equipamentos
• Como usar: Essa API é REST e aceita parâmetros de consulta na URL (como o código coCnes do estabelecimento ou filtros por estado/município). Ela retorna um JSON contendo a descrição do equipamento, a quantidade existente e a quantidade em uso.
(Nota: Para integrações corporativas ou de sistemas legados, o DATASUS também mantém uma versão em WebService SOAP antiga chamada EquipamentoService, mas a versão REST acima é a recomendada para novos projetos).

2. Medicamentos nos Postos de Saúde (BNAFAR)

Como o estoque em tempo real por posto de saúde não fica exposto, os dados disponíveis são os de posição de estoque consolidada, enviados pelos municípios ao sistema nacional. Você pode acessar essa base de duas formas:
• Através do Endpoints da API REST (Dados Abertos):
	• URL Base: https://apidadosabertos.saude.gov.br
	• Endpoint (GET): /bnafar/estoque-medicamentos (ou endpoints equivalentes mapeados no catálogo sob a tag BNAFAR).
• Através do Portal de Downloads (Arquivos CSV/Grandes Volumes):
	• Caso precise cruzar o estoque do município inteiro, o Ministério da Saúde disponibiliza os pacotes brutos quizenais na página do Portal de Dados Abertos do SUS - BNAFAR. A própria plataforma do portal de dados abertos possui uma API interna (CKAN) que permite consultar esses arquivos programaticamente através do endpoint /api/3/action/package_show?id=bnafar-posicao-de-estoque.
