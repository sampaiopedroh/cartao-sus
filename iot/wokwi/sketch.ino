#include <WiFi.h>
#include <PubSubClient.h>

// Wi-Fi virtual do Wokwi (sem senha)
const char* WIFI_SSID = "Wokwi-GUEST";

// Broker público do HiveMQ (sem conta). O tópico precisa ser igual ao da ponte (iot/ponte_mqtt.py).
const char* MQTT_HOST  = "broker.hivemq.com";
const int   MQTT_PORTA = 1883;
const char* TOPICO     = "gateminds-cartaosus-7f3k/tensiometro/leitura";

const int PINO_SISTOLE  = 34;  // ADC1 (ADC2 não funciona com Wi-Fi)
const int PINO_DIASTOLE = 35;
const int PINO_BOTAO    = 4;
const int PINO_LED      = 2;

WiFiClient rede;
PubSubClient mqtt(rede);

int lerMmHg(int pino, int minimo, int maximo) {
  long soma = 0;
  for (int i = 0; i < 8; i++) soma += analogRead(pino);
  return map(soma / 8, 0, 4095, minimo, maximo);
}

void conectarWifi() {
  WiFi.begin(WIFI_SSID, "", 6);
  Serial.print("Conectando ao Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) { delay(300); Serial.print("."); }
  Serial.println(" ok");
}

void conectarMqtt() {
  while (!mqtt.connected()) {
    String id = "tensiometro-" + String(random(0xffff), HEX);
    Serial.print("Conectando ao HiveMQ...");
    if (mqtt.connect(id.c_str())) Serial.println(" ok");
    else { Serial.printf(" falhou (%d), tentando de novo\n", mqtt.state()); delay(1500); }
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PINO_BOTAO, INPUT_PULLUP);
  pinMode(PINO_LED, OUTPUT);
  conectarWifi();
  mqtt.setServer(MQTT_HOST, MQTT_PORTA);
  conectarMqtt();
  Serial.println("Gire os potenciometros e aperte o botao para medir.");
}

void loop() {
  if (!mqtt.connected()) conectarMqtt();
  mqtt.loop();

  int sistolica  = lerMmHg(PINO_SISTOLE, 90, 220);
  int diastolica = lerMmHg(PINO_DIASTOLE, 50, 140);
  digitalWrite(PINO_LED, (sistolica > 180 || diastolica > 110) ? HIGH : LOW);

  static unsigned long ultimoBotao = 0;
  if (digitalRead(PINO_BOTAO) == LOW && millis() - ultimoBotao > 600) {
    ultimoBotao = millis();
    char json[96];
    snprintf(json, sizeof(json),
             "{\"dispositivo\":\"tensiometro\",\"sistolica\":%d,\"diastolica\":%d}",
             sistolica, diastolica);
    bool ok = mqtt.publish(TOPICO, json);
    Serial.printf("Medicao %d/%d mmHg -> %s\n", sistolica, diastolica, ok ? "enviada" : "FALHOU");
  }
  delay(50);
}
