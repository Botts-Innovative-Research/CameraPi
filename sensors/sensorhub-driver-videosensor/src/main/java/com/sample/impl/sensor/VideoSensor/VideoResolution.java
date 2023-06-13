package com.sample.impl.sensor.VideoSensor;

import org.sensorhub.api.config.DisplayInfo;

public class VideoResolution {
    @DisplayInfo.Required
    @DisplayInfo(label= "", desc = "Width of video frames")
    public  int videoFrameWidth=640;

    @DisplayInfo.Required
    @DisplayInfo(label= "", desc= "Height of video frames")
    public  int videoFrameHeight=480;
}
