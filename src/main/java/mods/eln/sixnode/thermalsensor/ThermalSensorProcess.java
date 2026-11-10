package mods.eln.sixnode.thermalsensor;

import mods.eln.Eln;
import mods.eln.sim.IProcess;

public class ThermalSensorProcess implements IProcess {

    ThermalSensorElement sensor;

    public ThermalSensorProcess(ThermalSensorElement sensor) {
        this.sensor = sensor;
    }

    @Override
    public void process(double time) {
        if (sensor.typeOfSensor == sensor.temperatureType) {
            setOutput(sensor.thermalLoad.Tc);
        } else if (sensor.typeOfSensor == sensor.powerType) {
            setOutput(sensor.thermalLoad.getPower());
        }
    }

    void setOutput(double physical) {
        // (0 to SVU; a 0/0 when the bounds meet gives 0, not a NaN on the signal)
        sensor.outputGateProcess.setUSafe((physical - sensor.lowValue) / (sensor.highValue - sensor.lowValue) * Eln.SVU);
    }
}
