package mods.eln.simplenode.test;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.simple.SimpleNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;

public class TestNode extends SimpleNode {

    NbtElectricalLoad load = new NbtElectricalLoad("load");
    Resistor resistor = new Resistor(load, null);

    @Override
    public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
        return MASK_ELECTRICAL_POWER;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA, int mask) {
        return null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB, int mask) {
        return load;
    }

    @Override
    public String getNodeUuid() {
        return getNodeUuidStatic();
    }

    public static String getNodeUuidStatic() {
        return "eln.TestNode";
    }

    @Override
    public void initialize() {
        electricalLoadList.add(load);
        electricalComponentList.add(resistor);

        load.setRs(10);
        resistor.setR(90);

        connect();
    }
}
