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
    private final GpioController gpio = GpioFactory.getInstance();
    private GpioPinDigitalOutput tiltPin;
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

        if (config.tiltServoPin != GpioEnum.PIN_UNSET) {
            tiltPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.tiltServoPin.getValue()));
        } else {
            tiltPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13);
        }
        tiltPin.setShutdownOptions(true, PinState.LOW);
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {
            output.doStop();
        }

        if (tiltPin != null) {
            tiltPin.setState(PinState.LOW);
            gpio.unprovisionPin(tiltPin);
            tiltPin = null;
        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }

    /**
     * Maps angle to PWM signal for SG90 Servos
     * 20 ms (50Hz) PWM Period
     * 1 - 2 MS Duty Cycle
     * 1.0 ms pulse -  90.0° - maps to   0° (right)
     * 1.5 ms pulse -   0.0° - maps to  90° (center)
     * 2.0 ms pulse - -90.0° - maps to 180° (left)
     *
     * Using busy wait as Java Sleep Timer for threads calls exceed
     * desired sleep time due to invocation time.  In addition,
     * granularity of sleep is bound by thread scheduler's interrupt
     * period (1ms in Linux and approx 10-15 ms in Windows).
     *
     * High level of pulse is calculated between 0.5 ms and 2.5ms.
     *
     * @param servoPin The pin to operate on
     * @param angle The angle of rotation in range [0 - 180]
     */
    private void rotateTo(GpioPinDigitalOutput servoPin, double angle) {

        logger.info("pin: " + servoPin.getName() + " angle: " + angle);

        long pulseWidthMicros = Math.round(angle * 11) + 500;

        logger.info("pulseWidth: " + pulseWidthMicros);

        for (int i = 0; i <= 15; ++i) {

            servoPin.setState(PinState.HIGH);

            long start = System.nanoTime();
            while (System.nanoTime() - start < pulseWidthMicros * 1000) ;

            servoPin.setState(PinState.LOW);

            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < (20 - pulseWidthMicros / 1000)) ;
        }
    }

    /**
     * Rotates the tilt servo to prescribed angle
     * @param angle angle to turn to
     */
    public void tiltTo(double angle) {
        rotateTo(tiltPin, angle);
    }
}
