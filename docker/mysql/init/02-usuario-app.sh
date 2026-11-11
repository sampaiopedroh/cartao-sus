#!/bin/sh
# Cria o usuario de aplicacao com permissao minima (SDD 8.5 / F2).
# Executado pelo MySQL na primeira criacao do volume. Senha vem do .env.
set -e
if [ -z "$MYSQL_APP_PASSWORD" ]; then
  echo "MYSQL_APP_PASSWORD nao definida" >&2
  exit 1
fi
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER IF NOT EXISTS 'cartao_app'@'%' IDENTIFIED BY '$MYSQL_APP_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON cartao_sus.* TO 'cartao_app'@'%';
GRANT ALL PRIVILEGES ON cartao_sus_test.* TO 'cartao_app'@'%';
FLUSH PRIVILEGES;
SQL
