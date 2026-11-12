#!/bin/sh
# Monta o .zip da entrega a partir do que está no git (sem .env, builds, chaves ou logs).
# Uso (na pasta cartao-sus/, com tudo commitado):  sh docs/entrega/montar_zip.sh
set -e
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$RAIZ"
if [ -n "$(git status --porcelain)" ]; then echo "Há alterações não commitadas; faça commit antes." >&2; exit 1; fi
TMP="$(mktemp -d)"
PASTA="$TMP/GateMinds-GS2026"
mkdir -p "$PASTA"
git archive --prefix=cartao-sus/ HEAD | tar -x -C "$PASTA"
cp README.md "$PASTA/README.md"
cp ENTREGA-TXT.txt "$PASTA/ENTREGA-TXT.txt"
cp docs/entrega/GateMinds-Cartao-SUS-Entrega.pdf "$PASTA/GateMinds-Cartao-SUS-Entrega.pdf"
DESTINO="$RAIZ/../GateMinds-Cartao-SUS-GS2026.zip"
rm -f "$DESTINO"
(cd "$TMP" && zip -qr "$DESTINO" GateMinds-GS2026)
rm -rf "$TMP"
echo "Gerado: $DESTINO"
