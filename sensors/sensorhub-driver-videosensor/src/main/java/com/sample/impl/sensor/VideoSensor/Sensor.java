/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.VideoSensor;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class Sensor extends AbstractSensorModule<Config>{

    private static final Logger logger = LoggerFactory.getLogger(Sensor.class);
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private Output output;


    @Override
    public void doInit() throws SensorHubException {

        super.doInit();
        logger.debug("Initializing");

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:videoSensor:", config.serialNumber);
        generateXmlID("VIDEO_SENSOR", config.serialNumber);

        // Create and initialize output
        output= new Output(this);

        try {

            addOutput(output, false);
            output.init();

        } catch (SensorException e) {

            logger.error("Failed to initialize {}", output.getName());

            throw new SensorHubException("Failed to initialize " + output.getName());
        }

    }

    @Override
    public boolean isConnected() {
        return output.isAlive();
    }

    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("HD Camera");

                SMLHelper helper = new SMLHelper(sensorDescription);

                helper.addSerialNumber(config.serialNumber);
            }
        }
    }

    @Override
    public void doStart() throws SensorHubException {

        logger.debug("Starting");
        super.doStart();

        if (null != output) {
            try{
                output.start();
            } catch(SensorException e){
                logger.error("Failed to start {} due to { } ", output.getName(),e);
            }
        }

        isConnected.set(true);
        logger.debug("Started");
    }

    @Override
    public void doStop() throws SensorHubException {
        logger.debug("Stopping");
        super.doStop();

        if (null != output) {

            output.stop();
        }
        isConnected.set(false);
        logger.debug("Stopped");
    }
}
