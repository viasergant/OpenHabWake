/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wake.internal;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.wake.handler.WakeHubHandler;
import org.openhab.binding.wake.handler.WakeSensorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Dictionary;

import static org.openhab.binding.wake.WakeBindingConstants.*;

/**
 * The {@link WakeDeviceHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Serhiy Piddubchak - Initial contribution
 */
public class WakeDeviceHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(WakeDeviceHandlerFactory.class);
    private WakeDeviceBindingProperties bindingProperties;

/*
    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        logger.debug("Activating WakeDeviceHandlerFactory");
        Dictionary<String, Object> properties = componentContext.getProperties();
        bindingProperties = new WakeDeviceBindingProperties(properties);
    }
*/

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        Boolean result = SUPPORTED_DEVICE_THING_TYPES_UIDS.contains(thingTypeUID);
        return result;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_SENSOR)) {
            return new WakeSensorHandler(thing/*,bindingProperties*/);
        }

        if (thingTypeUID.equals(THING_TYPE_HUB)) {
            return new WakeHubHandler(thing/*,bindingProperties*/);
        }
        return null;
    }
}
