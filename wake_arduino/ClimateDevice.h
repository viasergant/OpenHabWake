#ifndef CLIMATE_DEVICE_H
#define CLIMATE_DEVICE_H


//ClimateDeviceType
#define DHT_TEMPERATURE 			1
#define DHT_HUMIDITY    			2
#define LUMINOSITY    				3
#define CO2           				4
#define BINARY_ACTUATOR   			5
#define ANALOG_ACTUATOR   			6
#define ONE_WIRE_TEMPERATURE      	7

template<class T>
class ClimateDevice {
 private:
  T _value;
  char _type;
public:
  char getType() const {return _type;}
  T getValue() const {return _value;}
  void setValue(T val) { _value = val;}
  void getValue(char *buffer, size_t maxsize){}
  void setValue(const char *buffer){}
};

template<>
class ClimateDevice<float> {
  private:
  float _value;
  char _type;
  public:
  char getType() const {return _type;}
  float getValue() const {return _value;}
  void setValue(float val) { _value = val;}
  void getValue(char *buffer, size_t maxsize){
    String temp(_value);
    strncpy(buffer, temp.c_str(), maxsize);
  }
  void setValue(const char *buffer){
    String temp(buffer);
    _value = temp.toFloat();
  }
};




#endif
