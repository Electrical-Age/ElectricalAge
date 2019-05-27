package mods.eln.sixnode.genericcable;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.item.ItemStack;

public abstract class GenericCableDescriptor extends SixNodeDescriptor {

    public double electricalNominalVoltage;
    public double electricalMaximalVoltage;
    public double electricalMaximalCurrent;
    public double electricalNominalPower;
    public double electricalRs;

    // TODO: remove later
    public double thermalRp;
    public double thermalC;
    public double thermalRs;
    public double thermalWarmLimit;
    public double thermalCoolLimit;

    public CableRenderDescriptor render;

    public GenericCableDescriptor(String name, Class ElementClass, Class RenderClass) {
        super(name, ElementClass, RenderClass);
    }

    public int getNodeMask() {
        return NodeBase.MASK_ELECTRICAL_POWER;
    }

    public abstract void applyTo(ElectricalLoad electricalLoad, double rsFactor);

    public abstract void applyTo(ElectricalLoad electricalLoad);

    public abstract void applyTo(Resistor resistor);

    public abstract void applyTo(Resistor resistor, double factor);

    public abstract void applyTo(ThermalLoad thermalLoad);

    public static CableRenderDescriptor getCableRender(ItemStack cable) {
        if(cable == null) return null;
        GenericItemBlockUsingDamageDescriptor desc = GenericCableDescriptor.getDescriptor(cable);
        if (desc instanceof GenericCableDescriptor)
            return ((GenericCableDescriptor) desc).render;
        else
            return null;
    }
}
