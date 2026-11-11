-- ========================================================
-- Cartão SUS Inteligente — Carga inicial (MySQL 8)
-- Executar após schema.sql. Não contém segredos (SDD 8.5).
--   - SENHA_HASH e SEGREDO_TOTP: preenchidos pelo backend (F2, SeedDemoService).
--   - Dispositivos IoT: criados por iot/gerar_chaves.py (F6).
--   - CPF_HASH: SHA-256 de CPFs FICTÍCIOS de demonstração.
-- ========================================================
USE cartao_sus;

-- Usuários (4). O hash de senha é substituído pelo backend na primeira execução.
INSERT INTO TB_USUARIO (ID_USUARIO, LOGIN, SENHA_HASH, PERFIL, ATIVO) VALUES
(1, 'admin@cartaosus.demo',        '__SERA_GERADO__', 'ADMIN', 1),
(2, 'maria.souza@cartaosus.demo',  '__SERA_GERADO__', 'CIDADAO', 1),
(3, 'joao.pereira@cartaosus.demo', '__SERA_GERADO__', 'CIDADAO', 1),
(4, 'carla.lima@cartaosus.demo',   '__SERA_GERADO__', 'PROFISSIONAL_SAUDE', 1);

-- Cidadãos (2). CPF_HASH = SHA-256 de CPFs fictícios (12345678909 e 98765432100).
INSERT INTO TB_CIDADAO (ID_CIDADAO, ID_USUARIO, CPF_HASH, NUMERO_CNS, NOME_COMPLETO, DATA_NASCIMENTO, SEGREDO_TOTP) VALUES
(1, 2, '7ec94663084bd506d4f0c3e21042df233681fd7426e93f397c921b1d3e397bba', '898000000000000', 'Maria Souza Lima',    '1985-03-12', '__SERA_GERADO__'),
(2, 3, 'fe63757c49af2f9466851ad0b5cd915a5097ab13354c15e42114958b7173521d', '182345678901234', 'João Pereira Santos', '1978-11-25', '__SERA_GERADO__');

-- Unidades (16) — CNES oficial, cidade de São Paulo (SDD 4.4.1).
-- IDs 1, 2 e 3 são usados pela demo (UBS Jardim Colombo, UPA Vila Prudente, Hospital Carmino Caricchio).
INSERT INTO TB_UNIDADE_SAUDE (ID_UNIDADE, CODIGO_CNES, NOME_ESTABELECIMENTO, TIPO_UNIDADE, BAIRRO, LOGRADOURO, LATITUDE, LONGITUDE) VALUES
(1,  '9186492', 'UBS JARDIM COLOMBO',                     'UBS',      'JARDIM COLOMBO',        'RUA FREI BONIFACIO DUX 40',            -23.6010000, -46.7270000),
(2,  '8530378', 'UPA VILA PRUDENTE',                      'UPA',      'JARDIM INDEPENDENCIA',  'AVENIDA ALBERTO RAMOS 103',            -23.5831231, -46.5627113),
(3,  '2080346', 'HOSP MUN DR CARMINO CARICCHIO',          'HOSPITAL', 'TATUAPE',               'AV CELSO GARCIA 4815',                 -23.5328766, -46.5663774),
(4,  '8047790', 'UBS VILA RUBI',                          'UBS',      'PARQUE DAS ARVORES',    'RUA ARCHOTE DO PERU 604',              -23.7275750, -46.6920470),
(5,  '8033420', 'UBS JARDIM KIOTO IGNES FONTANA TATTO',   'UBS',      'JARDIM KIOTO',          'RUA DUNKERQUE 36',                     -23.7340770, -46.7171930),
(6,  '2026961', 'UBS VILA HELOISA',                       'UBS',      'VILA HELOISA',          'RUA AIRES QUARESMA 11',                -23.5897011, -46.5348190),
(7,  '2786737', 'UBS ALTO DA RIVIERA',                    'UBS',      'ALTO DA RIVIERA',       'AVENIDA PROFESSOR MARIO MAZAGAO 194',  -23.7000400, -46.7643479),
(8,  '2788306', 'UBS PARQUE EDU CHAVES',                  'UBS',      'PARQUE EDU CHAVES',     'AV EDU CHAVES 1197',                   -23.4775689, -46.5686595),
(9,  '9971513', 'UPA ERMELINO MATARAZZO',                 'UPA',      'VILA PARANAGUA',        'RUA MIGUEL NOVAIS 113',                -23.4991211, -46.4736664),
(10, '4050169', 'UPA PERUS DR LUIZ ANTONIO DE ABREU SAMPAIO DORIA', 'UPA', 'PERUS',            'RUA ESTEVAN RIBEIRO RESENDE S/N',      -23.4007267, -46.7471480),
(11, '6133622', 'UPA SACOMA',                             'UPA',      'SACOMA',                'ESTRADA DAS LAGRIMAS 1403',            -23.6134979, -46.5938383),
(12, '2752107', 'UPA SANTO AMARO DR JOSE SYLVIO DE CAMARGO', 'UPA',   'SANTO AMARO',           'AVENIDA ADOLFO PINHEIRO 122',          -23.6512613, -46.7048800),
(13, '2081970', 'HOSP MUN JABAQUARA ARTUR RIBEIRO DE SABOYA', 'HOSPITAL', 'JABAQUARA',         'AVENIDA FRANCISCO DE PAULA QUINTANILHA RIBEIRO 860', -23.6536386, -46.6450463),
(14, '2077450', 'HOSP MUN PIRITUBA JOSE SOARES HUNGRIA', 'HOSPITAL', 'PIRITUBA',              'AVENIDA MENOTTI LAUDISIO 100',         -23.4855600, -46.7087300),
(15, '2084473', 'HOSP MUN IGNACIO PROENCA DE GOUVEA',     'HOSPITAL', 'PARQUE DA MOOCA',       'JUVENTUS 562',                         -23.5716969, -46.5900993),
(16, '2079240', 'HOSPITAL GERAL JESUS TEIXEIRA DA COSTA GUAIANASES SAO PAULO', 'HOSPITAL', 'JARDIM SAO PAULO', 'AV MIGUEL ACHIOLE DA FONSECA 135', -23.5566508, -46.4002537);

-- Carteira de documentos (5) — somente metadados; arquivos ficam no device.
INSERT INTO TB_CARTEIRA_METADADOS (ID_CIDADAO, TIPO_DOCUMENTO, NOME_ARQUIVO, HASH_INTEGRIDADE, DATA_VALIDADE) VALUES
(1, 'RG',             'rg_maria.pdf',     '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08', NULL),
(1, 'CPF',            'cpf_maria.jpg',    '60303ae22b998861bce3b28f33eec1be758a213c86c93c076dbe9f558c11c752', NULL),
(1, 'CARTAO_VACINA',  'vacina_maria.pdf', 'fd61a03af4f77d870fc21e05e7e80678095c92d808cfb3b5c279ee04c74aca13', NULL),
(2, 'RG',             'rg_joao.pdf',      'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', NULL),
(2, 'RECEITA_MEDICA', 'receita_joao.pdf', 'ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb', '2026-11-14');

-- Histórico clínico (5)
INSERT INTO TB_HISTORICO_CLINICO (ID_CIDADAO, TIPO_REGISTRO, DESCRICAO, GRAU_SEVERIDADE, DATA_REGISTRO) VALUES
(1, 'ALERGIA', 'Penicilina', 'CRITICO', '2024-05-10'),
(1, 'DOENCA_CRONICA', 'Diabetes tipo 2', 'ALTO', '2021-02-01'),
(1, 'CIRURGIA', 'Colecistectomia (vesícula)', 'MODERADO', '2019-08-15'),
(2, 'DOENCA_CRONICA', 'Hipertensão arterial', 'ALTO', '2020-07-01'),
(2, 'ALERGIA', 'Dipirona', 'ALTO', '2023-01-20');

-- Sinais vitais (9)
INSERT INTO TB_SINAL_VITAL (ID_CIDADAO, TIPO_MEDICAO, VALOR_MEDIDO, ORIGEM, DATA_HORA) VALUES
(1, 'GLICEMIA', 112.00, 'APP', '2026-11-09 07:15:00'),
(1, 'GLICEMIA', 138.00, 'APP', '2026-11-08 19:40:00'),
(1, 'PRESSAO_SISTOLICA', 124.00, 'APP', '2026-11-09 07:15:00'),
(1, 'PRESSAO_DIASTOLICA', 80.00, 'APP', '2026-11-09 07:15:00'),
(2, 'PRESSAO_SISTOLICA', 185.00, 'IOT', '2026-11-09 21:30:00'),
(2, 'PRESSAO_DIASTOLICA', 115.00, 'IOT', '2026-11-09 21:30:00'),
(2, 'PRESSAO_SISTOLICA', 130.00, 'IOT', '2026-11-09 08:00:00'),
(2, 'PRESSAO_DIASTOLICA', 85.00, 'IOT', '2026-11-09 08:00:00'),
(2, 'PESO', 92.50, 'APP', '2026-11-01 06:00:00');

-- Trilha de auditoria (5 sessões)
INSERT INTO TB_SESSAO_ACESSO (ID_CIDADAO, ID_UNIDADE, ID_USUARIO_ATENDENTE, TOKEN_UTILIZADO, STATUS_AUTENTICACAO, MOTIVO, DATA_HORA_ACESSO) VALUES
(1, 1, 4, '482913', 'AUTORIZADO', NULL, '2026-11-08 09:12:00'),
(1, 1, 4, '771204', 'AUTORIZADO', NULL, '2026-11-08 09:30:00'),
(2, 2, NULL, '111222', 'REJEITADO', 'Token inválido', '2026-11-09 21:35:00'),
(2, 2, NULL, '333444', 'REJEITADO', 'Token inválido', '2026-11-09 21:36:00'),
(2, 2, NULL, '555666', 'REJEITADO', 'Token inválido', '2026-11-09 21:37:00');

-- Alertas (3)
INSERT INTO TB_ALERTA (ID_CIDADAO, TIPO_ALERTA, SEVERIDADE, MENSAGEM, STATUS_ENVIO) VALUES
(2, 'PRESSAO_CRITICA', 'CRITICA', 'Alerta de Pressão Crítica: Procure a UPA mais próxima', 'PENDENTE'),
(2, 'TENTATIVA_INDEVIDA', 'ALTA', '3 tentativas inválidas de leitura detectadas no terminal da UPA', 'PENDENTE'),
(2, 'RECEITA_VENCENDO', 'ALTA', 'Receita médica vence em 14/11/2026', 'PENDENTE');
