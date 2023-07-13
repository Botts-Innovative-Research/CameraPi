/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.pager;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sensor extends AbstractSensorModule<Config> {
    private static final Logger logger = LoggerFactory.getLogger(Sensor.class);

    ICommProvider<?> commProvider;
    Output output;

    public Sensor() {

    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:rsi:pager:", config.serialNumber);
        generateXmlID("rsi_pager_", config.serialNumber);

        // Create and initialize output
        output = new Output(this);
        addOutput(output, false);
        output.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {
        // init comm provider
        if (commProvider == null) {
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                // start comm provider
                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
                if (commProvider.getCurrentError() != null)
                    throw (SensorHubException) commProvider.getCurrentError();
            } catch (Exception e) {
                commProvider = null;
                throw e;
            }
        }

        if (null != output) {
            output.doStart(commProvider);
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        // stop comm provider
        if (commProvider != null) {
            commProvider.stop();
            commProvider = null;
        }

        if (null != output) {
            output.doStop();
        }
    }

    @Override
    public boolean isConnected() {
        // Determine if sensor is connected
        return output.isAlive();
    }
}