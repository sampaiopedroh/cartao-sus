#!/bin/sh
# Roda os testes de integração (IT-01 a IT-10) contra o MySQL do docker compose.
# Pré-requisitos: .env preenchido na pasta cartao-sus/ e MySQL em execução (docker compose up -d).
set -e
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$RAIZ"
if [ ! -f .env ]; then
  echo "Crie o .env antes: cp .env.example .env" >&2
  exit 1
fi
STATUS=$(docker inspect -f '{{.State.Health.Status}}' cartao-sus-mysql 2>/dev/null || echo ausente)
if [ "$STATUS" != "healthy" ]; then
  echo "MySQL não está healthy (status: $STATUS). Rode: docker compose up -d" >&2
  exit 1
fi
set -a
. ./.env
set +a
dotnet test "$RAIZ/backend/CartaoSus.slnx" --filter "Category=Integracao" "$@"
