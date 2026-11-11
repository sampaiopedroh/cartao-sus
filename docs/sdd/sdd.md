# PROJETO: CARTÃO SUS INTELIGENTE — SAÚDE CONECTADA
### Versão: 1.0
### Data: Novembro de 2026
### Domínio: Saúde Pública Digital / CitizenTech

## 1. VISÃO GERAL E ESCOPO DO SISTEMA
### 1.1 Objetivo do Sistema
O Cartão SUS Inteligente é uma solução de tecnologia cívica (CitizenTech) projetada para unificar o acolhimento presencial, o histórico clínico e a transparência de serviços no Sistema Único de Saúde (SUS). A solução elimina o deslocamento inútil do cidadão através de auditoria de disponibilidade e descentraliza a custódia de dados clínicos e documentos civis, garantindo privacidade estrita e agilidade de triagem via aproximação NFC associada a um token dinâmico (MFA).

### 1.2 Os Três Pilares de Serviço
- Serviço 1 — Consulta de Disponibilidade Pública: Consulta em tempo real aos dados abertos do DATASUS para equipamentos (API CNES) e posição consolidada de remédios (API BNAFAR), cruzados com telemetria local.
- Serviço 2 — Carteira Cidadã e Gestão Clínica (Local-First): Autoalimentação de documentos civis (RG, CPF, CNS, Certidões), receitas médicas, histórico de alergias, cirurgias e telemetria de sinais vitais (pressão, glicemia, peso).
- Serviço 3 — Acolhimento Presencial Seguro (NFC + TOTP): Handshake presencial no balcão da UBS/UPA utilizando identificador físico por aproximação e autorização em dois fatores via Token Rotativo de 6 dígitos.

## 2. ARQUITETURA GERAL DO SISTEMA
O sistema adota uma arquitetura distribuída baseada no padrão Mobile-First Client-Server, combinada com princípios de Zero-Knowledge Encryption para a custódia de arquivos sensíveis e Edge Computing para a simulação dos dispositivos IoT.

### 2.1 Diagrama de Contexto e Containers (C4 Model - Nível 2)
```Code
                               ┌─────────────────────────────────────────────────┐
                               │           APIs PÚBLICAS DO GOVERNO              │
                               │  - API CNES (Equipamentos em Unidades)          │
                               │  - API BNAFAR (Estoque Consolidado Remédios)    │
                               └───────────────────────▲─────────────────────────┘
                                                       │ HTTPS / REST
                                                       │
┌───────────────────────┐            ┌─────────────────┴─────────────────────────┐
│     DISPOSITIVOS      │            │             BACKEND API                   │
│      IOT / EDGE       │            │        (ASP.NET Core / Java Spring)       │
│  - Leitor NFC UBS     │ HTTP/REST  │  ┌──────────────┐      ┌───────────────┐  │
│  - Telemetria Vital   ├───────────►│  │  Controller  │ ───► │    Service    │  │
│    (Glicemia / PA)    │            │  └──────────────┘      └───────┬───────┘  │
└───────────────────────┘            │                                │          │
                                     │                        ┌───────▼───────┐  │
┌───────────────────────┐            │                        │  Repository   │  │
│    APLICATIVO MÓVEL   │            │                        └───────┬───────┘  │
│   (Cidadão / Flutter) │ HTTP/REST  └────────────────────────────────┼──────────┘
│  - Secure Storage     ├───────────►                                 │
│    (Docs Pessoais)    │                                             │ ADO.NET / JPA
│  - Gerador TOTP       │                                     ┌───────▼──────────┐
└───────────────────────┘                                     │ BANCO RELACIONAL │
                                                              │  (Oracle/MySQL)  │
                                                              └──────────────────┘
```

## 3. DESIGN DE COMPONENTES E CAMADAS (BACKEND)
Conforme a exigência da Parte 2 do edital, a API é estritamente desacoplada em três camadas:
1. Camada de Apresentação (Controllers):
Responsável pela exposição dos endpoints RESTful, validação de payloads (Data Annotations / Bean Validation), roteamento e despacho dos códigos de status HTTP adequados (200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 404 Not Found).
2. Camada de Negócio (Services):
Implementa as regras de negócio: validação criptográfica do Token TOTP rotativo, verificação de expiração, cálculo de médias de pressão/glicemia e regras de auditoria de acessos.
3. Camada de Acesso a Dados (Repositories):
Encapsula as operações de CRUD e consultas analíticas no banco relacional, utilizando transações ACID e consultas parametrizadas contra injeção de SQL.

## 4. DESIGN DE DADOS E MODELO RELACIONAL
### 4.1 Estratégia de Armazenamento Híbrido (Privacidade & LGPD)
No Smartphone (Armazenamento Seguro do SO): Arquivos brutos (PDFs e imagens de RG, CPF, Certidão e Laudos) residem no Sandbox Seguro do dispositivo do cidadão (EncryptedSharedPreferences / Keychain).

No Banco de Dados Central: São armazenados apenas os metadados cadastrais, os hashes de validação de integridade, a trilha de auditoria e os registros estruturados de sinais vitais.
### 4.2 Diagrama Entidade-Relacionamento Lógico (DER)
```Code
┌────────────────────────┐         1:N         ┌────────────────────────┐
│       TB_CIDADAO       ├────────────────────►│    TB_SINAL_VITAL      │
├────────────────────────┤                     ├────────────────────────┤
│ PK  ID_CIDADAO         │                     │ PK  ID_SINAL           │
│     CPF_HASH           │                     │ FK  ID_CIDADAO         │
│     NUMERO_CNS         │                     │     TIPO_MEDICAO       │
│     NOME_COMPLETO      │                     │     VALOR_MEDIDO       │
│     SENHA_HASH         │                     │     DATA_HORA          │
│     CRIADO_EM          │                     └────────────────────────┘
└───────────┬────────────┘
            │ 1:N
            ├─────────────────────────────────┐
            │ 1:N                             │ 1:N
            ▼                                 ▼
┌────────────────────────┐         ┌────────────────────────┐
│  TB_CARTEIRA_METADADOS │         │  TB_HISTORICO_CLINICO  │
├────────────────────────┤         ├────────────────────────┤
│ PK  ID_DOCUMENTO       │         │ PK  ID_HISTORICO       │
│ FK  ID_CIDADAO         │         │ FK  ID_CIDADAO         │
│     TIPO_DOCUMENTO     │         │     TIPO_REGISTRO      │
│     HASH_INTEGRIDADE   │         │     DESCRICAO_DETALHE  │
│     DATA_CADASTRO      │         │     GRAU_SEVERIDADE    │
└────────────────────────┘         └────────────────────────┘
            ▲
            │
┌───────────┴────────────┐         1:N         ┌────────────────────────┐
│   TB_UNIDADE_SAUDE     ├────────────────────►│   TB_SESSAO_ACESSO     │
├────────────────────────┤                     ├────────────────────────┤
│ PK  ID_UNIDADE         │                     │ PK  ID_SESSAO          │
│     CODIGO_CNES        │                     │ FK  ID_CIDADAO         │
│     NOME_ESTABELECIM.  │                     │ FK  ID_UNIDADE         │
│     BAIRRO             │                     │     TOKEN_UTILIZADO    │
│     LOGRADOURO         │                     │     STATUS_AUTENTICACAO│
└────────────────────────┘                     │     DATA_HORA_ACESSO   │
                                               └────────────────────────┘
````

### 4.3 DDL das Tabelas (Padrão SQL ANSI / Oracle / PostgreSQL)
``` SQL
CREATE TABLE TB_CIDADAO (
    ID_CIDADAO NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    CPF_HASH VARCHAR2(64) NOT NULL UNIQUE,
    NUMERO_CNS VARCHAR2(15) NOT NULL UNIQUE,
    NOME_COMPLETO VARCHAR2(150) NOT NULL,
    SENHA_HASH VARCHAR2(255) NOT NULL,
    CRIADO_EM TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE TB_UNIDADE_SAUDE (
    ID_UNIDADE NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    CODIGO_CNES VARCHAR2(10) NOT NULL UNIQUE,
    NOME_ESTABELECIMENTO VARCHAR2(150) NOT NULL,
    BAIRRO VARCHAR2(80) NOT NULL,
    LOGRADOURO VARCHAR2(200) NOT NULL
);

CREATE TABLE TB_CARTEIRA_METADADOS (
    ID_DOCUMENTO NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ID_CIDADAO NUMBER NOT NULL,
    TIPO_DOCUMENTO VARCHAR2(30) NOT NULL, -- 'RG', 'CPF', 'CERTIDAO', 'RECEITA'
    HASH_INTEGRIDADE VARCHAR2(64) NOT NULL,
    DATA_CADASTRO TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_DOC_CIDADAO FOREIGN KEY (ID_CIDADAO) REFERENCES TB_CIDADAO(ID_CIDADAO)
);

CREATE TABLE TB_HISTORICO_CLINICO (
    ID_HISTORICO NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ID_CIDADAO NUMBER NOT NULL,
    TIPO_REGISTRO VARCHAR2(30) NOT NULL, -- 'ALERGIA', 'DOENCA_CRONICA', 'CIRURGIA'
    DESCRICAO_DETALHE VARCHAR2(255) NOT NULL,
    GRAU_SEVERIDADE VARCHAR2(20) DEFAULT 'MODERADO', -- 'LEVE', 'ALTO', 'CRITICO'
    CONSTRAINT FK_HIST_CIDADAO FOREIGN KEY (ID_CIDADAO) REFERENCES TB_CIDADAO(ID_CIDADAO)
);

CREATE TABLE TB_SINAL_VITAL (
    ID_SINAL NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ID_CIDADAO NUMBER NOT NULL,
    TIPO_MEDICAO VARCHAR2(30) NOT NULL, -- 'GLICEMIA', 'PRESSAO_SISTOLICA', 'PRESSAO_DIASTOLICA', 'PESO'
    VALOR_MEDIDO NUMBER(6,2) NOT NULL,
    DATA_HORA TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SINAL_CIDADAO FOREIGN KEY (ID_CIDADAO) REFERENCES TB_CIDADAO(ID_CIDADAO)
);

CREATE TABLE TB_SESSAO_ACESSO (
    ID_SESSAO NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ID_CIDADAO NUMBER NOT NULL,
    ID_UNIDADE NUMBER NOT NULL,
    TOKEN_UTILIZADO VARCHAR2(6) NOT NULL,
    STATUS_AUTENTICACAO VARCHAR2(20) NOT NULL, -- 'AUTORIZADO', 'EXPIRADO', 'REJEITADO'
    DATA_HORA_ACESSO TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SESSAO_CIDADAO FOREIGN KEY (ID_CIDADAO) REFERENCES TB_CIDADAO(ID_CIDADAO),
    CONSTRAINT FK_SESSAO_UNIDADE FOREIGN KEY (ID_UNIDADE) REFERENCES TB_UNIDADE_SAUDE(ID_UNIDADE)
);
````

## 5. DESIGN DE INTERFACES E CONTRATOS DE API (RESTful)
A API expõe serviços documentados via Swagger (OpenAPI 3.0):
- 5.1 Matriz de Endpoints Obrigatórios

| Método | Rota | Descrição | Status de Retorno |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/auth/login` | Autentica usuário e emite JWT. | 200, 401 |
| **POST** | `/api/v1/acolhimento/validar-nfc` | Recebe UID do cartão + Token TOTP e libera a triagem. | 200, 400, 403 |
| **POST** | `/api/v1/telemetria/sinais-vitais` | Ingestão de medições (feitas pelo usuário ou IoT). | 201, 400 |
| **GET** | `/api/v1/cidadao/{id}/prontuario` | Retorna histórico clínico autorizado. | 200, 404 |
| **GET** | `/api/v1/sus/equipamentos/{cnes}` | Proxy integrado à API oficial de equipamentos do CNES. | 200, 502 |
| **GET** | `/api/v1/sus/medicamentos/busca` | Consulta de estoque consolidado (API BNAFAR). | 200, 404 |
| **PUT** | `/api/v1/cidadao/{id}/alergias` | Atualização do cadastro de restrições medicamentosas. | 200, 400 |
| **DELETE** | `/api/v1/documentos/{idDoc}` | Exclui metadados de um documento da carteira. | 204, 404 |

### 5.2 Exemplo de Contrato (Handshake de Acolhimento na Recepção)
- Request (POST /api/v1/acolhimento/validar-nfc):
``` JSON
{
  "numeroCns": "898000000000000",
  "codigoCnesUnidade": "2078015",
  "tokenRotativo": "482913"
}
````

- Response Success (200 OK):
``` JSON
{
  "status": "AUTORIZADO",
  "mensagem": "Identificação e autorização validadas com sucesso.",
  "paciente": {
    "nomeCompleto": "Maria Souza Lima",
    "cns": "898000000000000",
    "documentosDisponiveis": ["RG", "CPF", "CARTAO_VACINA"],
    "alertasCriticos": [
      {
        "tipo": "ALERGIA",
        "substancia": "Penicilina",
        "severidade": "CRITICO"
      }
    ],
    "ultimasMedicoes": {
      "glicemia": 112.0,
      "pressaoArterial": "124/80",
      "dataRegistro": "2026-11-09T07:15:00"
    }
  }
}
```

## 6. DESIGN DO MÓDULO IOT (PARTE 6 DO EDITAL)
Para atender à exigência de 2+ sensores simulados que disparam regras de negócio:

### Dispositivo IoT 1: Terminal Leitor NFC da Recepção (Edge Device)
- **Função:** Captura a aproximação física do Cartão SUS do paciente no balcão e envia os dados para validação.
- **Comportamento Simulado (Script Python):**
    - O script gera requisições sequenciais enviando o código CNS lido via RFID e o token digitado no terminal.
    - Regra de Alerta do Sistema: Caso o token seja enviado com valor expirado por mais de 3 vezes consecutivas, a API rejeita a requisição, grava o incidente em TB_SESSAO_ACESSO com status REJEITADO e notifica o app do cidadão sobre tentativa indevida de leitura.

### Dispositivo IoT 2: Medidor Biométrico / Tensiômetro Conectado
- **Função:** Simula um monitor residencial de pressão arterial conectado que transmite a sístole e diástole via telemetria para o app e a API.
- **Comportamento Simulado (Script Python):**
    - O script injeta valores de pressão arterial (120/80, 130/85, 185/115).
    - Regra de Alerta do Sistema: Se o valor registrado ultrapassar o limiar de crise hipertensiva (Sistólica > 180 mmHg ou Diastólica > 110 mmHg), o backend dispara um evento imediato que projeta no app: "Alerta de Pressão Crítica: Procure a UPA mais próxima", mapeando automaticamente a rota no módulo de unidades.

## 7. ESPECIFICAÇÃO DE SEGURANÇA E PRIVACIDADE
1. **Autenticação e Perfis (RBAC):**
    - Perfil `CIDADAO`: Acesso exclusivo aos próprios dados, geração de TOTP e visualização de estoque.
    - Perfil `PROFISSIONAL_SAUDE`: Acesso temporário aos dados do paciente somente mediante validação simultânea de CNS + Token Rotativo ativo.
2. **Criptografia e Armazenamento:**
    - Senhas armazenadas com salt dinâmico via algoritmo BCrypt (custo 12).
    - Comunicação estrita sobre HTTPS / TLS 1.3.
3. **Mecanismo de Consentimento Dinâmico (MFA / TOTP):**
    - Implementação do algoritmo RFC 6238 (TOTP) com chave secreta compartilhada, gerando códigos de 6 dígitos que expiram em uma janela de tolerância máxima de 60 segundos. O cartão por si só não libera nenhum dado se o cidadão não autorizar pelo app.

## 8. FLUXO DE NAVEGAÇÃO MOBILE (ESTRUTURA DAS TELAS)
- **Tela 1 — Autenticação e Boas-Vindas:** Login biométrico/senha com perfil do cidadão.
- **Tela 2 — Painel Central / Cartão Digital:** Exibição do número CNS, atalhos rápidos e alertas preventivos de receitas a vencer.
- **Tela 3 — Módulo NFC & Token de Atendimento:** Exibição do código rotativo de 6 dígitos para fornecimento ao atendente e histórico dos últimos acessos médicos concedidos.
- **Tela 4 — Consulta de Serviços e Remédios (APIs SUS):** Interface de busca integrada às bases do CNES e BNAFAR, exibindo unidades no raio com o medicamento e os equipamentos em operação.
- **Tela 5 — Carteira de Sinais Vitais e Histórico:** Visualização de gráficos temporais de pressão e glicemia com registro manual e telemetria recebida do IoT.