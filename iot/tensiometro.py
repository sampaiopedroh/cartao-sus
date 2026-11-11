"""Sensor 2 — Tensiômetro conectado (simulado). SDD 9.3.

Mede sístole e diástole. Envia POST /api/v1/telemetria/sinais-vitais com a chave
X-API-KEY do tensiômetro. O cidadão é o vinculado ao dispositivo, por isso não há CNS.

Exemplos (na pasta cartao-sus/):
    python3 iot/tensiometro.py --cenario demo             # 120/80, 130/85 e 185/115
    python3 iot/tensiometro.py --sistolica 185 --diastolica 115
Códigos de saída: 0 = todas as leituras aceitas; 1 = alguma recusada; 2 = erro de conexão.
"""
import argparse
import sys
import time

from sensor_comum import carregar_env, enviar, exigir, imprimir

CENARIO_DEMO = [("normal", 120, 80), ("elevada", 130, 85), ("crise hipertensiva", 185, 115)]
LIMIAR_SISTOLICA, LIMIAR_DIASTOLICA = 180, 110


def main() -> int:
    carregar_env()
    ap = argparse.ArgumentParser(description="Tensiômetro simulado")
    grupo = ap.add_mutually_exclusive_group(required=True)
    grupo.add_argument("--cenario", choices=["demo"], help="roteiro pronto de leituras")
    grupo.add_argument("--sistolica", type=float, help="valor da sístole em mmHg")
    ap.add_argument("--diastolica", type=float, help="valor da diástole em mmHg (com --sistolica)")
    args = ap.parse_args()
    if args.sistolica is not None and args.diastolica is None:
        ap.error("--diastolica é obrigatória com --sistolica")

    chave = exigir("IOT_KEY_TENSIOMETRO")
    cabecalhos = {"X-API-KEY": chave, "X-Device-Type": "TENSIOMETRO", "Content-Type": "application/json"}

    if args.cenario == "demo":
        leituras = [(rotulo, s, d) for rotulo, s, d in CENARIO_DEMO]
    else:
        leituras = [("manual", args.sistolica, args.diastolica)]

    resultado = 0
    for rotulo, sistolica, diastolica in leituras:
        print(f"-- leitura {rotulo}: {sistolica:.0f}/{diastolica:.0f} mmHg (limiar de crise: {LIMIAR_SISTOLICA}/{LIMIAR_DIASTOLICA})")
        for tipo, valor in (("PRESSAO_SISTOLICA", sistolica), ("PRESSAO_DIASTOLICA", diastolica)):
            status, corpo = enviar("POST", "/api/v1/telemetria/sinais-vitais", cabecalhos,
                                   {"tipoMedicao": tipo, "valor": valor})
            imprimir(tipo, status, corpo)
            if status != 201:
                resultado = 1
            elif isinstance(corpo, dict) and corpo.get("alertaDisparado"):
                print(f"    ALERTA: {corpo['alerta']['mensagem']}")
        time.sleep(1)
    return resultado


if __name__ == "__main__":
    sys.exit(main())
