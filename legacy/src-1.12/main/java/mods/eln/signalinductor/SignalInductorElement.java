package mods.eln.signalinductor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;

public class SignalInductorElement extends SixNodeElement {

    public SignalInductorDescriptor descriptor;
    public NbtElectricalLoad postiveLoad = new NbtElectricalLoad("postiveLoad");
    public NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
    public Inductor inductor = new Inductor("inductor", postiveLoad, negativeLoad);

    public SignalInductorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(postiveLoad);
        electricalLoadList.add(negativeLoad);
        electricalComponentList.add(inductor);
        postiveLoad.setAsMustBeFarFromInterSystem();
        this.descriptor = (SignalInductorDescriptor) descriptor;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return postiveLoad;
        if (front.inverse() == lrdu) return negativeLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return descriptor.cable.getNodeMask();
        if (front.inverse() == lrdu) return descriptor.cable.getNodeMask();
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotAmpere("I", inductor.getCurrent());
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void initialize() {
        descriptor.applyTo(negativeLoad);
        descriptor.applyTo(postiveLoad);
        descriptor.applyTo(inductor);
    }
}
