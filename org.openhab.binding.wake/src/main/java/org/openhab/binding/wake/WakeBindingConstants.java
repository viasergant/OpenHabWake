/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wake;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * The {@link WakeBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Serhiy Piddubchak - Initial contribution
 */
public class WakeBindingConstants {

    private static final String BINDING_ID = "wake";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SENSOR = new ThingTypeUID(BINDING_ID, "wakeSensor");
    public static final ThingTypeUID THING_TYPE_HUB = new ThingTypeUID(BINDING_ID, "wakeHub");

    public static final String PARAMETER_ID = "id";
    public static final String PARAMETER_WAKE_ADDRESS = "wakeAddress";
    public static final String PARAMETER_DEVICE_ID = "deviceId";
    public static final String PARAMETER_REFRESH = "refresh";

    public static final String PARAMETER_PORT = "port";
    //public static final String PARAMETER_RETRY = "retry";
    public static final String PARAMETER_TIMEOUT = "timeout";
    public static final String PARAMETER_REFRESH_INTERVAL = "refresh_interval";


    // List of all Channel ids
    public static final String TEMPERATURE_CHANNEL_1 = "t1";
    public static final String SENSOR_TEMPERATURE = "sensorTemperature";
    public static final String SENSOR_HUMIDITY = "sensorHumidity";
    public static final String SENSOR_BINARY = "sensorBinary";
    public static final String SWITCH_BINARY = "switchBinary";
    public static final String SWITCH_MULTILEVEL = "switchMultilevel";
    public static final String SENSOR_MULTILEVEL = "sensorMultilevel";



    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_HUB,THING_TYPE_SENSOR);


}
