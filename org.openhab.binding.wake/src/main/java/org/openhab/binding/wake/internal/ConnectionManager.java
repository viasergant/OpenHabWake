package org.openhab.binding.wake.internal;

import jdk.dio.*;
import jdk.dio.gpio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Created by Volodymyr_Kychak on 6/13/2017.
 */
public final class ConnectionManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);
    private final static int PIN_NUMBER_INT = 4;
    //private static Lock lock = new ReentrantLock();
    private static ConnectionManager instance = null;
    private static Lock pinLock = new ReentrantLock();

    public static Object getPinLockObj() {
        return pinLockObj;
    }

    private static Object pinLockObj = new Object();

    private static Integer threadCount = 0;
    private static GPIOPin pin = null;

    private ConnectionManager() {
        try {
            LOGGER.debug("try to config pin {} to control RS-485 transmitter", PIN_NUMBER_INT);
            GPIOPinConfig pinConfig = new GPIOPinConfig(DeviceConfig.DEFAULT,
                    PIN_NUMBER_INT,
                    GPIOPinConfig.DIR_OUTPUT_ONLY,
                    GPIOPinConfig.MODE_OUTPUT_PUSH_PULL,
                    GPIOPinConfig.TRIGGER_NONE,
                    false);
            LOGGER.debug("try to open pin {}", PIN_NUMBER_INT);
            pin = (GPIOPin) DeviceManager.open(GPIOPin.class, pinConfig);

            LOGGER.debug("pin {} opened", PIN_NUMBER_INT);
        } catch (Exception ex) {
            LOGGER.error("Can't open pin {}: {}", PIN_NUMBER_INT, ex.getMessage());
        }
    }

    public static Lock getLock() {return pinLock;}
    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public void setPinValue(boolean value) {
        try {
            pin.setValue(value);
        } catch (Exception e) {
            LOGGER.error("Can't set value to pin", e);
        }
    }

    public void usePin(Consumer<GPIOPin> consumer) {
        try {
            consumer.accept(pin);
        } catch (Exception e) {
            LOGGER.error("Can't set value to pin", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        pin.close();
    }
    public static Integer incThreadCount() {
        return threadCount++;
    }
}
