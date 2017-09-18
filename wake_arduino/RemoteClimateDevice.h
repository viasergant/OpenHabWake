#ifndef REMOTE_CLIMATE_DEVICE_H
#define REMOTE_CLIMATE_DEVICE_H

#include "WakeUpSerial.h"
#include "ClimateDevice.h"
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// PINs defination
#define DHTPIN       2     // what pin we're connected to
#define DS_PIN     (10)

#define DHTTYPE DHT22   // DHT 22  (AM2302)
#define MAX_DS_DEVICES (20)


//command code:

#define CMD_GET_CDI     50    //request ClimateDeviceInfo info
//  0   1   2   3 .. N  - climate devices
//  2   2   1   2 .. 5  -  type of climate devices

#define CMD_GET_CD_VAL 51    //getClimateDevice value
#define CMD_SET_CD_VAL 52    //set ClimateDevice value


class RemoteClimateDevice {
private:
  static boolean _instanceFlag;
  static RemoteClimateDevice* _rcd;
  DHT _dht;
  OneWire  _ds;
  DallasTemperature ds_sensors;
  unsigned long _time;
  float _temperature;
  float _humidity;
  static const char _deviceInfo[];
  static const char _climateDevices[];
  DeviceAddress deviceAddress[MAX_DS_DEVICES];
  unsigned int dsDevicesCount = 0;
  void getValue(char *buffer, size_t maxsize, unsigned char cdNumber) const;
  RemoteClimateDevice(unsigned char dhtPin, unsigned char dsPin, unsigned char dhtType);  
public:
  ~RemoteClimateDevice() {_instanceFlag = false;}
  static RemoteClimateDevice* getInstance();
  static void wakeUpRxHandler(const WakeUpSerial* wserial, const WakePacketRx* rxp);
  void enumOneWire();
  void updateData();
  void updateDallasTemperature();
  void readDallasTemperature(DeviceAddress address, String &temperature);
};


#endif
