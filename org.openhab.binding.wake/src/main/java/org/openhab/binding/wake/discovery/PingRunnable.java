/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wake.discovery;

import org.openhab.binding.wake.WakeBindingConstants;
import org.openhab.binding.wake.internal.WakeManager;
import org.openhab.binding.wake.internal.WakePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This runnable pings the given IP address and is used by the {@see WakeDiscoveryService}.
 * If the java ping does not work, a native ping will be tried. This procedure is necessary,
 * because in some OS versions (e.g. Windows 7) the java ping does not work reliably.
 *
 * @author David Graeff <david.graeff@web.de>
 */
class PingRunnable implements Runnable {
    final Integer id;
    final WakeDiscoveryService service;
    private final Logger logger = LoggerFactory.getLogger(PingRunnable.class);

    public PingRunnable(Integer id, WakeDiscoveryService service) {
        this.id = id;
        this.service = service;
        if (id == null || id.equals(0)) {
            throw new RuntimeException("id may not be zero or null!");
        }
    }

    @Override
    public void run() {
        WakeManager manager = new WakeManager("/dev/ttyS0",9600,2000);
        try {
            logger.info("Probing Wake device with address {}", id);
            manager.open();
            WakePacket p = new WakePacket();
            p.setAddress(id.byteValue());
            p.setCommand((byte) 3);
            WakePacket result = manager.askLine(p);
            if (result != null) {
                logger.info("got result {} : {}", result.getCodeErr(), result.getDataAsString());
            } else {
                logger.error("Wake packet was not received!" );
            }

            if (result != null && result.getCodeErr().byteValue()==0) {
                service.newDevice(String.format("%s_%d", "wakeHub", id));
                logger.info("enumeraing Dallas Sensors...");
                WakePacket enumDSPacket = new WakePacket();
                enumDSPacket.setAddress(id.byteValue());
                enumDSPacket.setCommand((byte) 4);
                WakePacket enumAnswer = manager.askLine(enumDSPacket);
                logger.info("enum result: {}, len {}",enumAnswer.getCodeErr(),enumAnswer.getDataCount());
                if (enumAnswer.getCodeErr().byteValue()==0) {
                    int devices = enumAnswer.getDataCount()/8;
                    logger.info("found {} Dallas Temperature sensors",devices);
                    String addrArray = enumAnswer.getDataAsHexString();
                    logger.info("data: {}",addrArray);
                    for (int i=0;i<devices;i++) {
                        String address = addrArray.substring(i*16,i*16+16);
                        logger.info("found Dallas Temperature sensor: {}",address);
                        service.newDevice(String.format("%s_%d_%s", "wakeSensor", id, address));
                    }
                } else {
                    logger.error("enum result: {}",enumAnswer.getCodeErr());
                }
            }
        } catch (Exception e) {
            logger.error("Error: {}", e);
        } finally {
            try {
                manager.close();
            } catch (Exception e) {
               // e.printStackTrace();
            }
        }
    }
}
