/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wake.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.wake.internal.ConnectionManager;
import org.openhab.binding.wake.internal.WakeDeviceBindingProperties;
import org.openhab.binding.wake.internal.WakeManager;
import org.openhab.binding.wake.internal.WakePacket;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.config.core.Configuration;

import static org.openhab.binding.wake.WakeBindingConstants.*;

/**
 * The {@link WakeHubHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Serhiy Piddubchak - Initial contribution
 */
public class WakeHubHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(WakeHubHandler.class);
    private Integer wakeAddress;
    private Integer refreshDelay = 60;
    private ScheduledFuture<?> refreshJob;


    //private WakeDeviceBindingProperties properties;

    public WakeHubHandler(Thing thing/*, WakeDeviceBindingProperties properties*/) {
        super(thing);
        //this.properties = properties;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("Handle {} with command {}", channelUID.getId(), command.toString());
        if (channelUID.getId().equals(SENSOR_TEMPERATURE)) {
            updateState(channelUID, new DecimalType(2.322)/*getTemperature()*/);
            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        } else if (channelUID.getId().contains(SWITCH_BINARY) ||
                channelUID.getId().contains(SWITCH_MULTILEVEL)) {
            synchronized (ConnectionManager.getPinLockObj()) {
                WakeManager manager = new WakeManager("/dev/ttyS0", 9600, 2000);
                try {
                    String[] nameArray = channelUID.getId().split("-");
                    Integer pin = Integer.valueOf(nameArray[1]);
                    byte[] data = null;
                    manager.open();
                    WakePacket p = new WakePacket();
                    p.setAddress(wakeAddress.byteValue());
                    if (channelUID.getId().contains(SWITCH_BINARY)) {
                        data = new byte[2];
                        p.setCommand((byte) 7);
                        data[1] = (byte) (command.toString().equals("ON") ? 1 : 0);
                    } else if (channelUID.getId().contains(SWITCH_MULTILEVEL)) {
                        data = new byte[3];
                        p.setCommand((byte) 8);
                        Integer intVal = (int) (Integer.valueOf(command.toString()) * 2.55);
                        data[1] = (byte) (intVal & 0xFF);
                        data[2] = (byte) ((intVal >> 8) & 0xFF);
                    }
                    data[0] = pin.byteValue();
                    p.setData(data);
                    logger.debug("send  command to {} : {}", wakeAddress, p.toString());
                    WakePacket result = manager.askLine(p);
                    logger.debug("got result {} : {}", result.getCodeErr(), result.getDataAsString());

                    if (result != null && result.getCodeErr().byteValue() == 0) {
                        updateStatus(ThingStatus.ONLINE);
                    } else if (result != null) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "Returned result " + result.getCodeErr());
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "No answer returned");
                    }
                } catch (Exception e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
                    logger.error("Error: {}: {}", e.getClass().getCanonicalName(), e.getMessage());
                } finally {
                    try {
                        manager.close();
                    } catch (Exception e) {
                        logger.error("Error closing manager {}", e.getMessage());
                    }
                }
            }
        } else if (channelUID.getId().contains(SENSOR_BINARY)) {
            updateSensor(channelUID);
        }
    }

    private void updateSensor(ChannelUID channelUID) {
        synchronized (ConnectionManager.getPinLockObj()) {
            WakeManager manager = new WakeManager("/dev/ttyS0", 9600, 2000);
            try {
                String[] nameArray = channelUID.getId().split("-");
                Integer pin = Integer.valueOf(nameArray[1]);
                byte[] data = new byte[1];
                manager.open();
                WakePacket p = new WakePacket();
                p.setAddress(wakeAddress.byteValue());
                p.setCommand((byte) 9); // READ_BINARY
                data[0] = pin.byteValue();
               // data[1] = 1; // PULLUP
                p.setData(data);
                logger.debug("send  command to {} : {}", wakeAddress, p.toString());
                WakePacket result = manager.askLine(p);
                if (result != null) {
                    logger.debug("got result {} : {}", result.getCodeErr(), result.getDataAsString());
                } else {
                    logger.error("Timeout in getting result!");
                }

                if (result != null && result.getCodeErr().byteValue() == 0) {
                    updateState(channelUID, result.getData().get(0).byteValue() == 0 ? OnOffType.OFF : OnOffType.ON);
                    updateStatus(ThingStatus.ONLINE);
                } else if (result != null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "Returned result " + result.getCodeErr());
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "No answer returned");
                }
            } catch (Exception e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
                logger.error("Error: {}", e);
            } finally {
                try {
                    manager.close();
                } catch (Exception e) {
                    logger.error("Error closing manager {}", e);
                }
            }
        }
    }

    private void startAutomaticRefresh() {
        if (refreshJob == null || refreshJob.isCancelled()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (Channel channel : getThing().getChannels()) {
                            if (isLinked(channel.getUID().getId())) {
                                if (channel.getUID().getId().contains(SENSOR_BINARY)
                                        || channel.getUID().getId().contains(SENSOR_MULTILEVEL)) {
                                    updateSensor(channel.getUID());
                                }
                            }
                        }
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
        logger.info("initialize");
        // Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);
        Configuration conf = this.getConfig();
        Object value;

        value = conf.get(PARAMETER_WAKE_ADDRESS);
        if (value != null) {
            wakeAddress = ((BigDecimal)value).intValue();
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
}
