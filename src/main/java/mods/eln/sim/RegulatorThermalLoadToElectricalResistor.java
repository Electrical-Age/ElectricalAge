package mods.eln.sim;

import mods.eln.sim.mna.component.Resistor;

public class RegulatorThermalLoadToElectricalResistor extends RegulatorProcess {

    ThermalLoad thermalLoad;
    Resistor electricalResistor;

    double Rmin;
    /** What it holds, if not its thermal load's temperature (the Pipe Heater: its oil's, in the same units as Tc). */
    java.util.function.DoubleSupplier sensed;

    /** Hold this instead of the thermal load's temperature (relative to Tref, as Tc). */
    public void sense(java.util.function.DoubleSupplier sensed) {
        this.sensed = sensed;
    }

    public void setRmin(double Rmin) {
        this.Rmin = Rmin;
    }

    public RegulatorThermalLoadToElectricalResistor(String name, ThermalLoad thermalLoad, Resistor electricalResistor) {
        super(name);
        this.thermalLoad = thermalLoad;
        this.electricalResistor = electricalResistor;
    }

    @Override
    protected double getHit() {
        return sensed != null ? sensed.getAsDouble() : thermalLoad.Tc;
    }

    @Override
    protected void setCmd(double cmd) {
        if (cmd <= 0.001) {
            electricalResistor.highImpedance();
        } else if (cmd >= 1.0) {
            electricalResistor.setR(Rmin);
        } else {
            electricalResistor.setR(Rmin / cmd);
        }
    }
}
