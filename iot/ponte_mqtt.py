"""Ponte MQTT -> API (Wokwi + HiveMQ). SDD 9.1.

O tensiômetro simulado no Wokwi (ESP32) publica a leitura em um tópico MQTT do HiveMQ.
Esta ponte assina o tópico e repete cada leitura na API, com a X-API-KEY do tensiômetro:
    Wokwi -> HiveMQ (MQTT) -> ponte_mqtt.py -> POST /api/v1/telemetria/sinais-vitais -> app

Só repassa dados. A regra de alerta (180/110) continua na API.
Payload esperado: {"dispositivo":"tensiometro","sistolica":120,"diastolica":80}

Uso (na pasta cartao-sus/):
    iot/.venv/bin/python iot/ponte_mqtt.py
Variáveis (opcionais): MQTT_HOST, MQTT_PORTA, MQTT_TOPICO, API_URL, IOT_KEY_TENSIOMETRO.
"""
import json
import os
import sys

import paho.mqtt.client as mqtt

from sensor_comum import carregar_env, enviar, exigir, horario, imprimir

TOPICO_PADRAO = "gateminds-cartaosus-7f3k/tensiometro/leitura"
FAIXAS = {"sistolica": (50, 300), "diastolica": (30, 200)}  # só rejeita valor absurdo; a regra clínica é da API


def validar(payload: dict) -> tuple[float, float]:
    valores = []
    for campo, (minimo, maximo) in FAIXAS.items():
        v = payload.get(campo)
        if isinstance(v, bool) or not isinstance(v, (int, float)) or not (minimo <= v <= maximo):
            raise ValueError(f"campo '{campo}' ausente ou fora de {minimo}-{maximo}")
        valores.append(float(v))
    return valores[0], valores[1]


def repassar(cabecalhos: dict, sistolica: float, diastolica: float) -> None:
    print(f"[{horario()}] leitura recebida: {sistolica:.0f}/{diastolica:.0f} mmHg")
    for tipo, valor in (("PRESSAO_SISTOLICA", sistolica), ("PRESSAO_DIASTOLICA", diastolica)):
        status, corpo = enviar("POST", "/api/v1/telemetria/sinais-vitais", cabecalhos,
                               {"tipoMedicao": tipo, "valor": valor})
        imprimir(tipo, status, corpo)
        if status == 201 and isinstance(corpo, dict) and corpo.get("alertaDisparado"):
            print(f"    ALERTA: {corpo['alerta']['mensagem']}")


def main() -> int:
    carregar_env()
    host = os.environ.get("MQTT_HOST", "broker.hivemq.com")
    porta = int(os.environ.get("MQTT_PORTA", "1883"))
    topico = os.environ.get("MQTT_TOPICO", TOPICO_PADRAO)
    chave = exigir("IOT_KEY_TENSIOMETRO")
    cabecalhos = {"X-API-KEY": chave, "X-Device-Type": "TENSIOMETRO", "Content-Type": "application/json"}

    def ao_conectar(cliente, _dados, _flags, codigo, _props):
        if codigo.is_failure:
            sys.exit(f"Falha ao conectar no broker: {codigo}")
        cliente.subscribe(topico)
        print(f"[{horario()}] ponte ligada: {host}:{porta} | tópico {topico}\nAguardando leituras do Wokwi (Ctrl+C para sair)...")

    def ao_receber(_cliente, _dados, msg):
        try:
            sistolica, diastolica = validar(json.loads(msg.payload.decode("utf-8")))
        except (ValueError, UnicodeDecodeError) as erro:
            print(f"[{horario()}] mensagem ignorada: {erro}")
            return
        repassar(cabecalhos, sistolica, diastolica)

    cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    cliente.on_connect = ao_conectar
    cliente.on_message = ao_receber
    try:
        cliente.connect(host, porta, keepalive=30)
        cliente.loop_forever()
    except KeyboardInterrupt:
        print("\nPonte encerrada.")
    except OSError as erro:
        print(f"ERRO de conexão com o broker {host}:{porta}: {erro.__class__.__name__}")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
