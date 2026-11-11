-- Banco separado para os testes de integração (SDD seção 6.4).
-- Executado automaticamente pelo MySQL na primeira criação do volume.
CREATE DATABASE IF NOT EXISTS cartao_sus_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
