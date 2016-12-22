package mods.eln.gridnode.transformer;

import mods.eln.Eln;
import mods.eln.gridnode.GridElement;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.TransformerInterSystemProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundLooper;
import net.minecraft.util.Vec3;

/**
 * Created by svein on 07/08/15.
 */
public class GridTransformerElement extends GridElement {
    public NbtElectricalLoad primaryLoad = new NbtElectricalLoad("primaryLoad");
    public NbtElectricalLoad secondaryLoad = new NbtElectricalLoad("secondaryLoad");
    public VoltageSource primaryVoltageSource = new VoltageSource("primaryVoltageSource", primaryLoad, null);
    public VoltageSource secondaryVoltageSource = new VoltageSource("secondaryVoltageSource", secondaryLoad, null);
    public TransformerInterSystemProcess interSystemProcess = new TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource);
    GridTransformerDescriptor desc;
    float primaryMaxCurrent = 0;
    float secondaryMaxCurrent = 0;
    SoundLooper highLoadSoundLooper;

    VoltageStateWatchDog voltagePrimaryWatchdog = new VoltageStateWatchDog(), voltageSecondaryWatchdog = new VoltageStateWatchDog();

    //TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);

    public GridTransformerElement(TransparentNode node, final TransparentNodeDescriptor descriptor) {
        super(node, descriptor, 4);
        desc = (GridTransformerDescriptor) descriptor;

        electricalLoadList.add(primaryLoad);
        electricalLoadList.add(secondaryLoad);
        electricalComponentList.add(primaryVoltageSource);
        electricalComponentList.add(secondaryVoltageSource);
        WorldExplosion exp = new WorldExplosion(this).machineExplosion();
        slowProcessList.add(voltagePrimaryWatchdog.set(primaryLoad).set(exp));
        slowProcessList.add(voltageSecondaryWatchdog.set(secondaryLoad).set(exp));

        desc.cableDescriptor.applyTo(primaryLoad);
        desc.cableDescriptor.applyTo(secondaryLoad, 4);

        highLoadSoundLooper = new SoundLooper(this) {
            @Override
            public SoundCommand mustStart() {
                if (primaryMaxCurrent != 0 && secondaryMaxCurrent != 0) {
                    float load = (float) Math.max(primaryLoad.getI() / primaryMaxCurrent, secondaryLoad.getI() / secondaryMaxCurrent);
                    if (load > desc.minimalLoadToHum)
                        return desc.highLoadSound.copy().mulVolume(0.2f * (load - desc.minimalLoadToHum) / (1 - desc.minimalLoadToHum), 1f).smallRange();
                }
                return null;
            }
        };
        slowProcessList.add(highLoadSoundLooper);
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        Eln.simulator.mna.removeProcess(interSystemProcess);
    }

    @Override
    public void connectJob() {
        Eln.simulator.mna.addProcess(interSystemProcess);
        super.connectJob();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == front.up()) return primaryLoad;
        return secondaryLoad;
    }

    // TODO: Factor this against super.
    @Override
    public Vec3 getCablePoint(Direction side, int i) {
        if (i >= 2) throw new AssertionError("Invalid cable point index");
        int idx = side == front.up() ? 1 : 0;
        Obj3D.Obj3DPart part = (i == 0 ? desc.plus : desc.gnd).get(idx);
        BoundingBox bb = part.boundingBox();
        return bb.centre();
    }

    public ElectricalLoad getGridElectricalLoad(Direction side) {
        return getElectricalLoad(side, LRDU.Down);
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public String multiMeterString(Direction side) {
        if (side == front.up())
            return Utils.plotVolt("UP+:", primaryLoad.getU()) + Utils.plotAmpere("IP+:", primaryLoad.getCurrent());
        if (side == front.left())
            return Utils.plotVolt("US+:", secondaryLoad.getU()) + Utils.plotAmpere("IS+:", secondaryLoad.getCurrent());

        return Utils.plotVolt("UP+:", primaryLoad.getU()) + Utils.plotAmpere("IP+:", primaryLoad.getCurrent())
                + Utils.plotVolt("  US+:", secondaryLoad.getU()) + Utils.plotAmpere("IS+:", secondaryLoad.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        computeInventory();
        super.initialize();
    }

    public void computeInventory() {
        // TODO: Maybe later actually *have* an inventory, and all that stuff.
        // For now it'll just be a fixed 1:4.
        // Factoring out the common parts of the two transformers would also be nice!
        // God I miss mixins.
        // Can I have mixins?
        // Maybe later.

        voltagePrimaryWatchdog.setUNominal(12800);
        voltageSecondaryWatchdog.setUNominal(12800 * 4);
        primaryMaxCurrent = (float) desc.cableDescriptor.electricalMaximalCurrent;
        secondaryMaxCurrent = (float) desc.cableDescriptor.electricalMaximalCurrent;

        interSystemProcess.setRatio(0.25);
    }

    public float getLightOpacity() {
        return 1.0f;
    }
}


