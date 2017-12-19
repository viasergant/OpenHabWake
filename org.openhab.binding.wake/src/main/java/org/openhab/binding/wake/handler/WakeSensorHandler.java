/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wake.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Channel;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.wake.internal.ConnectionManager;
//import org.openhab.binding.wake.internal.WakeDeviceBindingProperties;
import org.openhab.binding.wake.internal.WakeDeviceBindingProperties;
import org.openhab.binding.wake.internal.WakeManager;
import org.openhab.binding.wake.internal.WakePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.wake.WakeBindingConstants.*;

/**
 * The {@link WakeSensorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Serhiy Piddubchak - Initial contribution
 */
public class WakeSensorHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(WakeSensorHandler.class);
    Configuration conf = this.getConfig();
    private Integer wakeAddress;
    private String dallasDeviceId;
    private ScheduledFuture<?> refreshJob;
    private Integer refreshDelay = 60;
//    private WakeDeviceBindingProperties properties;
    //private final Lock _mutex = new ReentrantLock(true);


    public WakeSensorHandler(Thing thing/*, WakeDeviceBindingProperties properties*/) {
        super(thing);
//        this.properties = properties;
    }

    protected void updateTemperature() {
        synchronized (ConnectionManager.getPinLockObj()) {
            WakeManager manager = new WakeManager("/dev/ttyS0", 9600, 5000);
            try {
                manager.open();
                WakePacket p = new WakePacket();
                p.setAddress(wakeAddress.byteValue());
                p.setCommand((byte) 5);
                logger.debug("prepare packet {}", p.toString());
                p.setData(DatatypeConverter.parseHexBinary(dallasDeviceId));
                logger.debug("prepare packet, set data {}", p.toString());
                logger.debug("send  command to {} : {}", wakeAddress, p.toString());

                WakePacket result = manager.askLine(p);
                if (result != null) {
                    logger.debug("got result {} : {}", result.getCodeErr(), result.getDataAsString());
                } else {
                    logger.error("No response received!");
                }

                if (result != null && result.getCodeErr().byteValue() == 0) {
                    if (result.getDataAsString() != null && !result.getDataAsString().equals("-127.00")) {
                        for (Channel channel : getThing().getChannels()) {
                            if (channel.getUID().getId().equals(SENSOR_TEMPERATURE)) {
                                updateState(channel.getUID(), new DecimalType(result.getDataAsString()));
                                updateStatus(ThingStatus.ONLINE);
                            }
                        }
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
                    }
                }
            } catch (Exception e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
                logger.error("Error: {}", e);
            } finally {
                try {
                    manager.close();
                } catch (Exception e) {
                    logger.error("Error closing manager {}", e);
                    // e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand of {}-{}",wakeAddress, dallasDeviceId);
        if (command instanceof RefreshType) {
            logger.debug("forced refreshDelay of {}-{}",wakeAddress, dallasDeviceId);
            if (channelUID.getId().equals(SENSOR_TEMPERATURE)) {
                updateTemperature();
            }

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    private void startAutomaticRefresh() {
        if (refreshJob == null || refreshJob.isCancelled()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        updateTemperature();
                    } catch (Exception e) {
                        logger.error("Exception occurred during execution: {}", e.getMessage(), e);
                    }
                }
            };

            refreshJob = scheduler.scheduleWithFixedDelay(runnable, 0, refreshDelay, TimeUnit.SECONDS);
        }
    }


    @Override
    public void initialize() {
        //logger.info("initialize");
        // Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);
        Configuration conf = this.getConfig();
        Object value;

        value = conf.get(PARAMETER_WAKE_ADDRESS);
        if (value != null) {
            wakeAddress = ((BigDecimal)value).intValue();
        }

        value = conf.get(PARAMETER_DEVICE_ID);
        if (value != null) {
            dallasDeviceId = value.toString();
        }

        value = conf.get(PARAMETER_REFRESH);
        if (value != null && (value instanceof Integer)) {
            refreshDelay = (Integer) value;
        }
        startAutomaticRefresh();


        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    @Override
    public void dispose() {
        logger.debug("Disposing the Air Quality handler.");

        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }
}
