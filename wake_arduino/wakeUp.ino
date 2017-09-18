#include "RemoteClimateDevice.h"
#include <DHT.h>

#define DHTPIN 		2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
//#define DS_PIN  	10
#define RS_485_PIN  3


//int deviceAddress = 3;
WakeUpSerial* wakeUpSerial;

void alarm() {
  digitalWrite(13, HIGH);
}

void alarm_off() {
  digitalWrite(13, LOW);
}


void setup() {
  pinMode(13, OUTPUT);
  pinMode(RS_485_PIN, OUTPUT);
  wakeUpSerial = new WakeUpSerial(Serial, 9600);
  wakeUpSerial->setRs485Pin(RS_485_PIN);  
  wakeUpSerial->addRxPacketListener(RemoteClimateDevice::wakeUpRxHandler);
 }

void loop() {
  wakeUpSerial->processing();
  RemoteClimateDevice::getInstance()->updateData();  
}

/*
  SerialEvent occurs whenever a new data comes in the
 hardware serial RX.  This routine is run between each
 time loop() runs, so using delay inside loop can delay
 response.  Multiple bytes of data may be available.
 */
void serialEvent() {
  alarm();
  wakeUpSerial->keepRxOn();
  alarm_off();
}


