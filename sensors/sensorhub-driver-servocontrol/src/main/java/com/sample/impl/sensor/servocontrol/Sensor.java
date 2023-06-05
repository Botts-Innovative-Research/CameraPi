/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.servocontrol;

import com.pi4j.io.gpio.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class Sensor extends AbstractSensorModule<Config> {
    private static final Logger logger = LoggerFactory.getLogger(Sensor.class);

    Control control;
    Output output;

    Object syncTimeLock = new Object();

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

        // Create and initialize output
//        output = new Output(this);
//        addOutput(output, false);
//        output.doInit();

        // Create and initialize controls
        control = new Control("sensor", this);
        addControlInput(control);
        control.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != output) {
            // Allocate necessary resources and start outputs
            output.doStart();
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {
            output.doStop();
        }

        // TODO: Perform other shutdown procedures
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}
