package org.openhab.binding.wake.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;

/**
 * class WakeUpSerial implements asynchronous interaction with remote devices using Wake protocol
 *
 * @author Serhiy_Povoroznyuk
 */
public class WakeUpSerial {
    private static final Logger s_logger = LoggerFactory
            .getLogger(WakeUpSerial.class);

    private Serial serial = null;

    private Queue<Byte> rxbuffer = new LinkedList<Byte>();
    private WakePacket rxWakePacket = new WakePacket();
    private List<WakeUpListener> listeners = new CopyOnWriteArrayList<WakeUpListener>();
    private SerialDataListener serialListener = new SerialDataListener() {
        @Override
        public void dataReceived(SerialDataEvent event) {
            for (char ch : event.getData().toCharArray()) {
                rxbuffer.add((byte) ch);
            }
            boolean isReady = false;
            while (!rxbuffer.isEmpty()) {
                byte bt = rxbuffer.remove();
                //s_logger.debug(String.format("receive from serial: 0x%02X", bt));
                isReady = rxWakePacket.setRXbyte(bt);
                if (isReady) {
                    s_logger.debug(rxWakePacket.toString());
                    if (rxWakePacket.getCodeErr() != Constants.ERR_TX) {
                        notifyListeners(rxWakePacket);
                    }
                    rxWakePacket = new WakePacket();
                    isReady = false;
                }
            }
        }
    };

    public void setSerial(Serial serial) {
        this.serial = serial;
        // create and register the serial data listener
        this.serial.addListener(serialListener);

    }

    public void unsetSerial() {
        this.serial.removeListener(serialListener);
        this.serial = null;
    }

    public void addWakeUpListener(WakeUpListener listener) {
        listeners.add(listener);
    }

    public void removeWakeUpListener(WakeUpListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(WakePacket wp) {
        for (WakeUpListener listener : listeners) {
            listener.wakeUpReceived(wp);
        }
    }

    /**
     * Transferring {@link WakePacket} to {@link Serial}
     *
     * @param wp - {@link WakePacket} for transferring
     */
    public void wakeTX(WakePacket wp) {
        if (serial != null && wp != null) {
            //s_logger.debug("\n-- tx -->> [{}] : {}", wp.getPacketAsHexString(), wp.toString());
                for (byte bt : wp.getTXbuf()) {
                    serial.write(bt);
                }
            try {
                Thread.sleep(wp.getTXbuf().size()*2); // 9600 bit/s - 1 byte = 1ms
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
