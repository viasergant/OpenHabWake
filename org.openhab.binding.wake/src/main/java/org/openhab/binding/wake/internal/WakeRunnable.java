package org.openhab.binding.wake.internal;

import jdk.dio.gpio.GPIOPin;
import org.openhab.binding.wake.handler.WakeHubHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WakeRunnable - inner thread for consecutive requests to line from
 * asynchronous clients
 *
 * @author Serhiy_Povoroznyuk
 */
public class WakeRunnable implements Runnable, WakeUpListener {
    private final Logger logger = LoggerFactory.getLogger(WakeHubHandler.class);
    private final BlockingQueue<WakeTask> requests;
    private final WakeUpSerial wakeUpSerial;
    private final long timeout; // TimeUnit.MILLISECONDS
    private SynchronousQueue<WakePacket> answerSyncQ = new SynchronousQueue<WakePacket>();
    private Integer threadId = ConnectionManager.incThreadCount();


    /**
     * @param wakeUpSerial - {@link WakeUpSerial} for interaction with remote devices
     * @param requests     - BlockingQueue for {@link WakeTask}
     * @param timeout      in TimeUnit.MILLISECONDS
     */
    public WakeRunnable(WakeUpSerial wakeUpSerial,
                        BlockingQueue<WakeTask> requests, long timeout) {
        this.wakeUpSerial = wakeUpSerial;
        this.requests = requests;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        do {
            if (!Thread.interrupted()) {
                WakeTask wakeTask = null;
                try {
                    wakeTask = requests.take();
                    WakePacket request = wakeTask.getRequest();
                        logger.debug("[{}] start transmit", threadId);
                        ConnectionManager.getInstance().usePin(new GPIOPinConsumer(request));
                        logger.debug("[{}]start read", threadId);
                        WakePacket answerWakePacket = wakeRead(); // read with
                        // timeout
                        if (answerWakePacket != null) {
                            wakeTask.setAnswer(answerWakePacket);
                        }
                        logger.debug("[{}]finish.", threadId);

                } catch (InterruptedException e) {
                    return;
                }
            } else {
                return;
            }
        } while (true);

    }

    @Override
    public void wakeUpReceived(WakePacket wp) {
        try {
            answerSyncQ.offer(wp, 5, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return;
        }

    }

    /**
     * @return received {@link WakePacket} or null if timeout
     * @throws InterruptedException
     */
    private WakePacket wakeRead() throws InterruptedException {
        return answerSyncQ.poll(timeout, TimeUnit.MILLISECONDS);
    }

    private class GPIOPinConsumer implements Consumer<GPIOPin> {
        private final WakePacket request;

        GPIOPinConsumer(WakePacket request) {
            this.request = request;
        }

        @Override
        public void accept(GPIOPin pin) {
            try {
                pin.setValue(true);
                wakeUpSerial.wakeTX(request); // send to serial
                pin.setValue(false);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }
}
