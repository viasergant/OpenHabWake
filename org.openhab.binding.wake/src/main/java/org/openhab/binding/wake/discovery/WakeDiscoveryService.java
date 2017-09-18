/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wake.discovery;

import static org.openhab.binding.wake.WakeBindingConstants.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

import static org.openhab.binding.wake.WakeBindingConstants.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.wake.service.DiscoveryCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link org.openhab.binding.wake.discovery.WakeDiscoveryService} is responsible for discovering devices on
 * the current Network. It uses every Network Interface which is connected to a network.
 *
 * @author Marc Mettke - Initial contribution
 */
public class WakeDiscoveryService extends AbstractDiscoveryService implements DiscoveryCallback {
    static final int PING_TIMEOUT_IN_MS = 500;
    private final Logger logger = LoggerFactory.getLogger(org.openhab.binding.wake.discovery.WakeDiscoveryService.class);
    private ExecutorService executorService = null;
    private Integer maxSeaarchDeviceId = 127;

    public WakeDiscoveryService() {
        super(SUPPORTED_DEVICE_THING_TYPES_UIDS, 900, false);
    }

    @Override
    protected void	activate(Map<String,Object> configProperties) {
        super.activate(configProperties);
        logger.debug("configProperties {}", configProperties.keySet().toString());
        maxSeaarchDeviceId = (Integer) configProperties.get("maxSearchDeviceId");
        if (maxSeaarchDeviceId==null) {
            maxSeaarchDeviceId = 10;
        }
    }

    /**
     * Starts the DiscoveryThread for each IP on each interface on the network
     *
     */
    @Override
    protected void startScan() {
        if (executorService != null) {
            stopScan();
        }
        logger.debug("Starting Discovery");

        executorService = Executors.newSingleThreadExecutor();

        for (int i = 1; i <= maxSeaarchDeviceId; i++) {
            executorService.execute(new PingRunnable(i, this));
        }
        stopScan();
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        if (executorService == null) {
            return;
        }

        try {
            executorService.awaitTermination(PING_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        executorService.shutdown();
        executorService = null;

    }

    /**
     * Submit newly discovered devices. This method is called by the spawned threads in {@link startScan}.
     *
     * @param id The device IP, received by the
     */
    @Override
    public void newDevice(String wakeDevice) {
        logger.info("Found device {}", wakeDevice);
        String params[] = wakeDevice.split("_");
        if (wakeDevice.indexOf("wakeHub") >= 0) {
            if (params.length == 2) {
                Map<String, Object> properties = new HashMap<>();
                ThingUID uid = new ThingUID(THING_TYPE_HUB, params[1]);
                properties.put(PARAMETER_WAKE_ADDRESS, Integer.valueOf(params[1]));
                logger.info("Adding {}", "Wake Device (" + wakeDevice + ")");
                thingDiscovered(DiscoveryResultBuilder.create(uid).withProperties(properties)
                        .withLabel("Wake Device (" + wakeDevice + ")").build());
            }
        } else if (wakeDevice.indexOf("wakeSensor") >= 0) {
            if (params.length == 3) {
                Map<String, Object> properties = new HashMap<>();
                ThingUID uid = new ThingUID(THING_TYPE_SENSOR, String.format("%s-%s", params[1], params[2]));
                properties.put(PARAMETER_WAKE_ADDRESS, Integer.valueOf(params[1]));
                properties.put(PARAMETER_DEVICE_ID, params[2]);
                properties.put(PARAMETER_REFRESH, 60);

                logger.info("Adding {}", "Wake Sensor (" + wakeDevice + ")");
                thingDiscovered(DiscoveryResultBuilder.create(uid).withProperties(properties)
                        .withLabel("Wake Sensor (" + wakeDevice + ")").build());
            }
        }
        // uid must not contains dots
    }
}

