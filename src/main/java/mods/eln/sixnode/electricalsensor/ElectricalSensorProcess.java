package mods.eln.sixnode.electricalsensor;

import mods.eln.Eln;
import mods.eln.sim.IProcess;

public class ElectricalSensorProcess implements IProcess {

    ElectricalSensorElement sensor;

    public ElectricalSensorProcess(ElectricalSensorElement sensor) {
        this.sensor = sensor;
    }

    @Override
    public void process(double time) {
        if (sensor.typeOfSensor == sensor.voltageType) {
            setOutput(sensor.aLoad.getU());
        } else if (sensor.typeOfSensor == sensor.currantType) {
            double output = 0;
            switch (sensor.dirType) {
                case ElectricalSensorElement.dirNone:
                    output = Math.abs(sensor.resistor.getCurrent());
                    break;
                case ElectricalSensorElement.dirAB:
                    output = (sensor.resistor.getCurrent());
                    break;
                case ElectricalSensorElement.dirBA:
                    output = (-sensor.resistor.getCurrent());
                    break;
            }

            setOutput(output);
        } else if (sensor.typeOfSensor == sensor.powerType) {
            double output = 0;
            switch (sensor.dirType) {
                case ElectricalSensorElement.dirNone:
                    output = Math.abs(sensor.resistor.getCurrent() * sensor.aLoad.getU());
                    break;
                case ElectricalSensorElement.dirAB:
                    output = (sensor.resistor.getCurrent() * sensor.aLoad.getU());
                    break;
                case ElectricalSensorElement.dirBA:
                    output = (-sensor.resistor.getCurrent() * sensor.aLoad.getU());
                    break;
            }

            setOutput(output);
        }
    }

    void setOutput(double physical) {
        // (0 to SVU; a 0/0 when the bounds meet gives 0, not a NaN on the signal)
        sensor.outputGateProcess.setUSafe((physical - sensor.lowValue) / (sensor.highValue - sensor.lowValue) * Eln.SVU);
    }
}
