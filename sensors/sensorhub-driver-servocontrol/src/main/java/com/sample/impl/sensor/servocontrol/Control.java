package com.sample.impl.sensor.servocontrol;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Quantity;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataChoiceImpl;
import org.vast.swe.SWEHelper;

public class Control extends AbstractSensorControl<Sensor> {
    private static final Logger logger = LoggerFactory.getLogger(Output.class);
    private static final double MIN_ANGLE = 20.0;

    private static final double MAX_ANGLE = 40.0;
    protected DataRecord commandDataStruct;

    protected Control(String name, Sensor parentSensor) {
        super(name, parentSensor);
    }

    protected void doInit() {
        SWEHelper factory = new SWEHelper();
        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("Servo"))
                .label("Servo")
                .description("A servo")
                .addField("Angle",
                        factory.createQuantity()
                                .updatable(true)
                                .definition(SWEHelper.getPropertyUri("servo-angle"))
                                .description("The angle in degrees to which the servo is to turn")
                                .addAllowedInterval(MIN_ANGLE, MAX_ANGLE)
                                .uomCode("deg")
                                .value(0.0)
                                .build())
                .build();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException {
        boolean commandExecuted = true;
        try {
            DataRecord commandData = commandDataStruct.copy();

            commandData.setData(cmdData);

            Quantity component = (Quantity) commandData.getField("Angle");

            double angle = component.getValue();
            angle = (angle <= MIN_ANGLE) ? MIN_ANGLE : Math.min(angle, MAX_ANGLE);

            logger.debug("Angle:" + angle);

        } catch (Exception e) {
            throw new CommandException("Failed to command the CameraSensor module: ", e);
        }

        return commandExecuted;
    }
}
