#include <WiFi.h>
#include <HTTPClient.h>
#include <ESPmDNS.h>

// ---------- USER CONFIG ----------
const char* WIFI_SSID     = "name";
const char* WIFI_PASSWORD = "password";
const char* hostName = "raspberrypi"; // To dynamically find local IP
const char* ESP32_ID = "ESP32_002";
IPAddress serverIP;

// ---------- PINS ----------
#define FLAME_SENSOR 4    // Flame IR digital output
#define MQ2_SENSOR   34    // MQ-2 digital output (can also use analog if needed)
#define PUMP_PIN    23    // Relay for water pump
#define BUZZER_PIN  18    // Buzzer

// ---------- TIMING ----------
unsigned long lastSafeTime = 0;

void setup() {
  Serial.begin(9600);
  pinMode(FLAME_SENSOR, INPUT);
  pinMode(MQ2_SENSOR, INPUT);
  pinMode(PUMP_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  digitalWrite(PUMP_PIN, LOW);
  noTone(BUZZER_PIN);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi...");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nConnected to WiFi!");
  MDNS.begin("esp32");
  serverIP = MDNS.queryHost(hostName); // returns IP
  Serial.println(serverIP);
  delay(10000);
  
}

void loop() {
  int flame = digitalRead(FLAME_SENSOR);
  int sensorValue = analogRead(MQ2_SENSOR); // Read analog value (0-1023)
  float voltage = sensorValue * (3.3 / 4095.0);

  // If flame or gas detected
  // 70-170/ below 650 is normal value above 170/810 flame
  if ((voltage >= 1.4) || (flame == LOW) ||( (voltage>=1.05) && flame == LOW)) {
    Serial.println(voltage);
    Serial.println("FIRE detected!");
    sendData("fire");
    digitalWrite(PUMP_PIN, HIGH);
    tone(BUZZER_PIN, 1000);
    delay(10000);
    noTone(BUZZER_PIN);
    lastSafeTime = millis(); // reset safe timer
  } 
  else {
    digitalWrite(PUMP_PIN, LOW);
    noTone(BUZZER_PIN);
    // 10 Minute = 600000 Milli-Second
    if (millis() - lastSafeTime >= 600000) {
      Serial.println("Safe signal sent.");
      sendData("safe");
      lastSafeTime = millis();
    }
  }
  // Hence millis() gives millisecond passed since board is started
  // Hence lastsafetime is updated every 10 minutes to amount of time since board was active this reflects time
  // Hence subtraction reflects time new value - old value  
  // Sensor does not have built in clock hence this method milliseconds are used.
}

void sendData(String status) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    String serverURL = "http://" + serverIP.toString() + ":5000/flame";
    http.begin(serverURL);
    http.addHeader("Content-Type", "application/json");

    String payload = "{\"esp32_id\":\"" + String(ESP32_ID) + "\",\"status\":\"" + status + "\"}";
    int code = http.POST(payload);

    if (code > 0) {
      Serial.print("HTTP code: ");
      Serial.println(code);
    } else {
      Serial.print("POST failed: ");
      Serial.println(code);
      Serial.println(serverIP);
    }

    http.end();
  } else {
    Serial.println("WiFi not connected.");
  }
}