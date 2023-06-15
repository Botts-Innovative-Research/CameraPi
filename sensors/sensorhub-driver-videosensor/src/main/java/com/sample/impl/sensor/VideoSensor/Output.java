package com.sample.impl.sensor.VideoSensor;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
//import java.io.StringWriter;

public class Output extends AbstractSensorOutput<Sensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "VideoOutput";
    private static final String SENSOR_OUTPUT_LABEL = "CameraSensor";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Video feed from Camera Sensor";
    private static final String VIDEO_FORMAT = "h264";
    private final Logger logger = LoggerFactory.getLogger(Output.class);
    private FrameGrabber frameGrabber;
    protected long lastDataFrameTimeMillis;
    protected Thread workerThread;
    protected int dataFrameCount = 0;
    // private DataRecord dataStruct;

    protected AtomicBoolean doWork = new AtomicBoolean(false);
    private DataComponent dataStruct;
    private DataEncoding dataEncoding;
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private CommandData stringWriter;

    public Output(Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);
        workerThread = new Thread(this, this.getClass().getSimpleName()+"-Worker-"+ parentSensor.getConfiguration().serialNumber);
        logger.debug("{} thread created...", workerThread.getName());
    }

    protected void init() throws SensorException {

        logger.debug("Initializing");
        lastDataFrameTimeMillis = System.currentTimeMillis();

        try {

            frameGrabber = FrameGrabber.createDefault(0);

        } catch (FrameGrabber.Exception e) {

            logger.debug("Failed to establish connection with camera\n{}", e.getMessage());

            throw new SensorException("Failed to establish connection with camera", e);
        }

        frameGrabber.setFormat(VIDEO_FORMAT);

        frameGrabber.setImageHeight(parentSensor.getConfiguration().videoResolution.videoFrameHeight);

        int videoFrameHeight = frameGrabber.getImageHeight();

        frameGrabber.setImageWidth(parentSensor.getConfiguration().videoResolution.videoFrameWidth);

        int videoFrameWidth = frameGrabber.getImageWidth();

        // Get an instance of SWE Factory suitable to build components
        VideoCamHelper sweFactory = new VideoCamHelper();

        DataStream outputDef = sweFactory.newVideoOutputMJPEG(getName(), videoFrameWidth, videoFrameHeight);

        dataStruct = outputDef.getElementType();

        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);

        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        dataEncoding = outputDef.getEncoding();

        logger.debug("Initialized");
    }

    protected void start() throws SensorException {

        logger.debug("starting");
        if (null != frameGrabber) {

            try {

                frameGrabber.start();

                doWork.set(true);

                workerThread.start();

            } catch (FrameGrabber.Exception e) {

                e.printStackTrace();

                logger.error("Failed to start FrameGrabber");

                throw new SensorException("Failed to start FrameGrabber");
            }

        } else {

            logger.error("Failed to create FrameGrabber");

            throw new SensorException("Failed to create FrameGrabber");
        }
        logger.debug("started");
    }

    protected void stop() {
        logger.debug("stopping");
        if (null != frameGrabber) {

            try {

                doWork.set(false);

                workerThread.join();

                frameGrabber.stop();

            } catch (FrameGrabber.Exception e) {

                logger.error("Failed to stop FrameGrabber");

            } catch (InterruptedException e) {

                logger.error("Failed to stop {} thread due to exception {}", workerThread.getName(), e.getMessage());
            }

        } else {

            logger.error("Failed to stop FrameGrabber");
        }
        logger.debug("stopped");
    }
    // @Override
    // public DataRecord getRecordDescription() {

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }
    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    public boolean isAlive() {

        return workerThread.isAlive();
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


        logger.debug("Starting worker {}", Thread.currentThread().getName());

        try {

            while (doWork.get()) {

                Frame frame = frameGrabber.grab();
                synchronized (histogramLock) {

                    int dataFrameIndex = dataFrameCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[dataFrameIndex] = System.currentTimeMillis() - lastDataFrameTimeMillis;

                    // Set latest sampling time to now
                    lastDataFrameTimeMillis = timingHistogram[dataFrameIndex];
                }

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                double sampleTime = System.currentTimeMillis() / 1000.0;

                dataBlock.setDoubleValue(0, sampleTime);

                // Set underlying video frame data
                AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];

                BufferedImage image = new Java2DFrameConverter().convert(frame);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                byte[] imageData;

                ImageIO.write(image, "jpg", byteArrayOutputStream);

                byteArrayOutputStream.flush();

                imageData = byteArrayOutputStream.toByteArray();

                byteArrayOutputStream.close();

                frameData.setUnderlyingObject(imageData);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, Output.this, dataBlock));
            }


        } catch (IOException e) {

            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());

        } finally {

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}






















