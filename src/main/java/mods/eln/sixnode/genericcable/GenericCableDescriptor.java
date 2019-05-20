package mods.eln.sixnode.genericcable;

import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;

public abstract class GenericCableDescriptor extends SixNodeDescriptor {

    public double electricalNominalVoltage;
    public double electricalMaximalCurrent;

    public GenericCableDescriptor(String name, Class ElementClass, Class RenderClass) {
        super(name, ElementClass, RenderClass);
    }

    public abstract void applyTo(ElectricalLoad electricalLoad, double rsFactor);

    public abstract void applyTo(ElectricalLoad electricalLoad);

    public abstract void applyTo(Resistor resistor);

    public abstract void applyTo(Resistor resistor, double factor);

    public abstract void applyTo(ThermalLoad thermalLoad);
}
