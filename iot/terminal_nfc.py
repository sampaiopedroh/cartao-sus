"""Sensor 1 — Terminal NFC da recepção (simulado). SDD 9.2.

Mede a aproximação do cartão (CNS) e recebe o código TOTP do cidadão.
Envia POST /api/v1/acolhimento/validar-nfc com a chave X-API-KEY do terminal.

Exemplos (na pasta cartao-sus/):
    python3 iot/terminal_nfc.py --cns 898000000000000 --cnes 9186492 --token 123456
    python3 iot/terminal_nfc.py --cns 898000000000000 --cnes 9186492 --app      # simula o app do cidadão
    python3 iot/terminal_nfc.py --cns 182345678901234 --cnes 8530378 --token 000000 --repeticoes 3
Códigos de saída: 0 = autorizado; 1 = recusado pela API; 2 = erro de conexão.
"""
import argparse
import sys
import time

from sensor_comum import carregar_env, enviar, exigir, imprimir

# Chave do terminal conforme a unidade (IDs do seed: 9186492 = UBS, 8530378 = UPA).
CHAVE_POR_CNES = {"9186492": "IOT_KEY_TERMINAL_UBS", "8530378": "IOT_KEY_TERMINAL_UPA"}


def main() -> int:
    carregar_env()
    ap = argparse.ArgumentParser(description="Terminal NFC simulado")
    ap.add_argument("--cns", required=True, help="CNS de 15 dígitos lido do cartão")
    ap.add_argument("--cnes", required=True, choices=sorted(CHAVE_POR_CNES), help="CNES da unidade do terminal")
    grupo = ap.add_mutually_exclusive_group(required=True)
    grupo.add_argument("--token", help="código de 6 dígitos digitado pelo atendente")
    grupo.add_argument("--app", action="store_true", help="gera o código do momento a partir do segredo TOTP do cidadão (env SEGREDO_TOTP)")
    ap.add_argument("--repeticoes", type=int, default=1, help="quantas vezes repetir a tentativa (força bruta: use 3)")
    args = ap.parse_args()

    chave = exigir(CHAVE_POR_CNES[args.cnes])
    cabecalhos = {"X-API-KEY": chave, "X-Device-Type": "TERMINAL_NFC", "Content-Type": "application/json"}

    resultado = 0
    for i in range(1, args.repeticoes + 1):
        token = args.token if args.token else gerar_codigo_app()
        status, corpo = enviar("POST", "/api/v1/acolhimento/validar-nfc", cabecalhos,
                               {"numeroCns": args.cns, "codigoCnesUnidade": args.cnes, "tokenRotativo": token})
        imprimir(f"tentativa {i}", status, corpo)
        if status != 200:
            resultado = 1
        if i < args.repeticoes:
            time.sleep(2)
    return resultado


def gerar_codigo_app() -> str:
    """Simula o celular do cidadão: gera o TOTP do momento (RFC 6238, 30 s)."""
    import base64, hashlib, hmac, struct
    segredo = exigir("SEGREDO_TOTP")
    chave = base64.b32decode(segredo.upper() + "=" * (-len(segredo) % 8))
    contador = int(time.time()) // 30
    digest = hmac.new(chave, struct.pack(">Q", contador), hashlib.sha1).digest()
    desloc = digest[-1] & 0x0F
    binario = struct.unpack(">I", digest[desloc:desloc + 4])[0] & 0x7FFFFFFF
    return str(binario % 1_000_000).zfill(6)


if __name__ == "__main__":
    sys.exit(main())
