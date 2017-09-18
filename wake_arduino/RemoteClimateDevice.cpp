#include "RemoteClimateDevice.h"

const char RemoteClimateDevice::_deviceInfo[] =
		"WakeHub with DHT 22 and DS18B20 v0.3";
const char RemoteClimateDevice::_climateDevices[] = { ONE_WIRE_TEMPERATURE,
		BINARY_ACTUATOR, ANALOG_ACTUATOR };

boolean RemoteClimateDevice::_instanceFlag = false;
RemoteClimateDevice* RemoteClimateDevice::_rcd = NULL;

RemoteClimateDevice* RemoteClimateDevice::getInstance() {
	if (!_instanceFlag) {
		_rcd = new RemoteClimateDevice(DHTPIN, DS_PIN, DHTTYPE);
		_instanceFlag = true;
		return _rcd;
	} else {
		return _rcd;
	}
}

RemoteClimateDevice::RemoteClimateDevice(unsigned char dhtPin,
		unsigned char dsPin, unsigned char dhtType) :
	_dht(dhtPin, dhtType), _ds(dsPin), ds_sensors(&_ds) {
	_dht.begin();

	ds_sensors.begin();
	_time = millis();
}

void RemoteClimateDevice::enumOneWire() {
	//ds_sensors.begin();
	dsDevicesCount = ds_sensors.getDeviceCount();
	for (int i = 0; i < dsDevicesCount && i < MAX_DS_DEVICES; i++) {
		ds_sensors.getAddress(deviceAddress[i], i);
	}
}

void RemoteClimateDevice::updateData() {
	/*
	 if (millis() - _time > 2500) {
	 _time = millis();
	 // Reading temperature or humidity takes about 250 milliseconds!
	 // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
	 // Read temperature as Celsius
	 float t = _dht.readTemperature();
	 float h = _dht.readHumidity();
	 if ( !isnan(t) && !isnan(h)) {
	 _temperature = t;
	 _humidity = h;
	 }
	 }
	 */
}

void RemoteClimateDevice::updateDallasTemperature() {

}

void RemoteClimateDevice::readDallasTemperature(DeviceAddress address,
		String &temperature) {
	ds_sensors.requestTemperatures();
	float tempC = ds_sensors.getTempC(address);
	temperature = String(tempC, 2);
}

void RemoteClimateDevice::getValue(char *buffer, size_t maxsize,
		unsigned char cdNumber) const {
	float data = NAN;
	if (cdNumber < 2) {
		switch (_climateDevices[cdNumber]) {
		case DHT_TEMPERATURE:
			data = _temperature;
			break;
		case DHT_HUMIDITY:
			data = _humidity;
			break;
		}
	}
	String temp(data);
	strncpy(buffer, temp.c_str(), maxsize);
}

void RemoteClimateDevice::wakeUpRxHandler(const WakeUpSerial* wserial,
		const WakePacketRx* rxp) {
	char err = ERR_PA;
	unsigned char cdNumber;
	String temperature = "";
	char *value;

	switch (rxp->getCommand()) {
	case CMD_INFO:
		wserial->sendAnswer(rxp, _deviceInfo, strlen(_deviceInfo));
		break;
	case CMD_ENUM_ONE_WIRE:
		RemoteClimateDevice::getInstance()->enumOneWire();
		wserial->sendAnswer(
				rxp,
				&RemoteClimateDevice::getInstance()->deviceAddress[0][0],
				sizeof(DeviceAddress)
						* RemoteClimateDevice::getInstance()->dsDevicesCount);
		break;
	case CMD_READ_ONE_WIRE:
		RemoteClimateDevice::getInstance()->readDallasTemperature(
				(DeviceAddress) rxp->getData(), temperature);
		if (temperature.length() > 0) {
			wserial->sendAnswer(rxp, temperature.c_str(), temperature.length());
		} else {
			err = ERR_RE;
			wserial->sendErr(rxp->getAddress(), &err, 1);
		}
		break;
	case CMD_SET_PIN_MODE:
		pinMode(rxp->getData()[0], rxp->getData()[1]);
		err = ERR_NO;
		wserial->sendErr(rxp->getAddress(), &err, 1);
		break;
	case CMD_SET_PIN_BINARY:
        pinMode(rxp->getData()[0], OUTPUT);
		digitalWrite(rxp->getData()[0], rxp->getData()[1]?HIGH:LOW);
		err = ERR_NO;
		wserial->sendErr(rxp->getAddress(), &err, 1);
		break;
	case CMD_SET_PIN_ANALOG: {
        pinMode(rxp->getData()[0], OUTPUT);
		int value = ((int)rxp->getData()[1] & 0xFF) + (((int)rxp->getData()[2] & 0xFF) * 0xFF);
		analogWrite(rxp->getData()[0], value);
		err = ERR_NO;
		wserial->sendErr(rxp->getAddress(), &err, 1);
	} break;
	case CMD_READ_PIN_BINARY: {
        pinMode(rxp->getData()[0], INPUT);
        digitalWrite(rxp->getData()[0], /*rxp->getData()[1]==0?LOW:HIGH*/HIGH);
		char valueBinary = (digitalRead(rxp->getData()[0])==HIGH?1:0);
		wserial->sendAnswer(rxp, &valueBinary, 1);
	} break;
	case CMD_READ_PIN_ANALOG: {
        pinMode(rxp->getData()[0], INPUT);
		String value(analogRead(rxp->getData()[0]));
		wserial->sendAnswer(rxp, value.c_str(), value.length());
	} break;
	case CMD_GET_CDI:
		wserial->sendAnswer(rxp, _climateDevices, 2);
		break;
	case CMD_GET_CD_VAL:
		char tempbuf[20];
		cdNumber = *rxp->getData();
		if (cdNumber < 2) {
			RemoteClimateDevice::getInstance()->getValue(tempbuf, 20, cdNumber);
			wserial->sendAnswer(rxp, tempbuf, strlen(tempbuf));
		} else {
			wserial->sendErr(rxp->getAddress(), &err, 1);
		}
		break;
	case CMD_SET_CD_VAL:
		//setClimateDevice
		wserial->sendErr(rxp->getAddress(), &err, 1);
		break;
	default:
		wserial->sendErr(rxp->getAddress(), &err, 1);
		break;
	}

}

