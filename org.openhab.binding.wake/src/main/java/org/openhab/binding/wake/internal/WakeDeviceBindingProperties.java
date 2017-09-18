package org.openhab.binding.wake.internal;

import java.util.Dictionary;

/**
 * Created by Serhiy_Piddubchak on 7/20/2017.
 */
public class WakeDeviceBindingProperties {
    private static final String MAX_SEARCH_DEVICE_ID = "maxSearchDeviceId";
    private static final String SERIAL_PORT = "serialPort";
    private final Integer maxSearchDeviceId;
    private final String serialPort;

    public WakeDeviceBindingProperties(Dictionary<String, Object> properties) {
        maxSearchDeviceId = (Integer) properties.get(MAX_SEARCH_DEVICE_ID);
        serialPort = (String) properties.get(SERIAL_PORT);
    }

    public String getSerialPort() {
        return serialPort;
    }

    public Integer getMaxSearchDeviceId() {
        return maxSearchDeviceId;
    }

}
