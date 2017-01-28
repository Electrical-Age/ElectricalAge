package mods.eln.item;

import mods.eln.misc.VoltageLevelColor;

public class MachineBoosterDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public MachineBoosterDescriptor(String name) {
        super(name);
        voltageLevelColor = VoltageLevelColor.Neutral;
    }
}
