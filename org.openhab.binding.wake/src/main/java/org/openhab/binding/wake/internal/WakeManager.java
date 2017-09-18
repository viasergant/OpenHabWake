package org.openhab.binding.wake.internal;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WakeManager synchronizes threads for synchronous interaction request-answer
 * by Wake protocol
 *
 * @author Serhiy_Povoroznyuk
 */
public class WakeManager implements AutoCloseable {

    private final BlockingQueue<WakeTask> requests = new LinkedBlockingQueue<WakeTask>();

    private final String port;
    private final int baudrate;
    private final long timeout;
    //
    private final Serial serial = SerialFactory.createInstance();
    private final WakeUpSerial wakeUpSerial = new WakeUpSerial();
    private final WakeRunnable wakeRunnable;
    private Thread walk;

    /**
     * @param port     : String like Serial.DEFAULT_COM_PORT
     * @param baudrate - 9600, 19200, .. , 115200
     * @param timeout  in TimeUnit.MILLISECONDS
     */
    public WakeManager(String port, int baudrate, long timeout) {
        this.port = port;
        this.baudrate = baudrate;
        this.timeout = timeout;
        wakeRunnable = new WakeRunnable(wakeUpSerial, requests, timeout);
        walk = new Thread(wakeRunnable);
        wakeUpSerial.addWakeUpListener(wakeRunnable);
        wakeUpSerial.setSerial(serial);
    }

    /**
     * @param request - {@link WakePacket}
     * @param answer  - {@link WakePacket}
     * @return true if answer correspond of request otherwise false
     */
    public static boolean checkAnswer(WakePacket request, WakePacket answer) {
        boolean result = true;

        if (request == null || answer == null) {
            result = false;
        } else if ((int) request.getAddress() != (int) answer.getAddress()) {
            result = false;
        } else if (((int) request.getCommand() | 0x40) != (int) answer
                .getCommand()) {
            result = false;
        } else if (answer.getCodeErr() != Constants.ERR_NO) {
            result = false;
        }
        return result;
    }

    /**
     * opens {@link Serial} connection and starts Thread for consecutive
     * requests to line from asynchronous clients
     *
     * @throws Exception
     */
    public void open() throws Exception {
        serial.open(port, baudrate);
        walk.start();
    }

    /**
     * close {@link Serial} , interrupt inner thread, remove
     * {@link WakeUpListener}
     */
    public void close() throws Exception {
        wakeUpSerial.removeWakeUpListener(wakeRunnable);
        wakeUpSerial.unsetSerial();
        walk.interrupt();
        serial.close();
    }

    /**
     * synchronous requests by Wake protocol
     *
     * @param wakePacket - {@link WakePacket} with request
     * @return {@link WakePacket} with answer or null after timeout
     * @throws InterruptedException
     */
    public WakePacket askLine(WakePacket wakePacket)
            throws InterruptedException {
        Thread.sleep(500);
        WakeTask wakeTask = new WakeTask(wakePacket, timeout);
        requests.add(wakeTask);
        return wakeTask.getAnswer();
    }

}


