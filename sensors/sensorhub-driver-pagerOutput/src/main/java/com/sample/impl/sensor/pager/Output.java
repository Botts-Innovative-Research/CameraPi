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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.io.*;

public class Output extends AbstractSensorOutput<Sensor> implements Runnable {
    private static final String SENSOR_OUTPUT_NAME = "RS350 Output";

    private static final Logger logger = LoggerFactory.getLogger(Output.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    Thread worker;

    boolean sendData;
    BufferedReader msgReader;

    public Output(Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // SWE Common data structure
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .addSamplingTimeIsoUTC("time")
                .addField("test", sweFactory.createText()
                        .definition(SWEHelper.getPropertyUri("test"))
                        .label("test")
                        .description("test"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        worker = new Thread(this, this.name);

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart(ICommProvider<?> commProvider) {
        // connect to data stream
        try {
            msgReader = new BufferedReader(new InputStreamReader(commProvider.getInputStream()));
            parentSensor.getLogger().info("Connected to data stream");
        } catch (IOException e) {
            throw new RuntimeException("Error while initializing communications ", e);
        }

        logger.info("Starting worker thread: {}", worker.getName());
        worker.start();
    }

    /**
     * Terminates processing data for output
     */
    public void doStop() {
        synchronized (processingLock) {
            stopProcessing = true;
        }
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {
        return worker.isAlive();
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        long accumulator = 0;

        synchronized (histogramLock) {
            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {
                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (processSets) {

                DataBlock dataBlock;
                if (latestRecord == null) {
                    dataBlock = dataStruct.createDataBlock();
                } else {
                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {
                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastSetTimeMillis = timingHistogram[setIndex];
                }

                ++setCount;

                double time = System.currentTimeMillis() / 1000;

                StringBuilder test = new StringBuilder();

                String line;
                while ((line = msgReader.readLine()) != null) {
                    logger.info(line);
                    test.append(line);
                    test.append("\n");
                    if (line.contains("</RadInstrumentData>")) {
                        break;
                    }
                }

                dataBlock.setDoubleValue(0, time);
                dataBlock.setStringValue(1, test.toString());

                latestRecord = dataBlock;
                latestRecordTime = System.currentTimeMillis();
                eventHandler.publish(new DataEvent(latestRecordTime, Output.this, dataBlock));

                synchronized (processingLock) {
                    processSets = !stopProcessing;
                }
            }
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());
        } finally {
            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}