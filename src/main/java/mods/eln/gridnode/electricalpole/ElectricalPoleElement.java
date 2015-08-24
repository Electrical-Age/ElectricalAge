package mods.eln.gridnode.electricalpole;

import mods.eln.Eln;
import mods.eln.gridnode.GridElement;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.TransformerInterSystemProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundLooper;

// TODO: I should probably just make the transformer variant a subclass.
public class ElectricalPoleElement extends GridElement {
    public NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    public NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    // Below elements are only used for transformer mode.
    public NbtElectricalLoad secondaryLoad;
    public VoltageSource primaryVoltageSource;
    public VoltageSource secondaryVoltageSource;
    public TransformerInterSystemProcess interSystemProcess;
    ElectricalPoleDescriptor desc;
    ElectricalLoadHeatThermalLoad heater = new ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad);
    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();
    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();
    float secondaryMaxCurrent = 0;
    SoundLooper highLoadSoundLooper;
    VoltageStateWatchDog voltageSecondaryWatchdog;

    public ElectricalPoleElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor, 24);
        desc = (ElectricalPoleDescriptor) descriptor;

        electricalLoad.setCanBeSimplifiedByLine(true);
        // Most of the resistance is in the cable, which is handled in GridLink.
        // We put some of it here, thereby allowing the thermal watchdog to work.
        desc.cableDescriptor.applyTo(electricalLoad);
        desc.cableDescriptor.applyTo(thermalLoad);
        electricalLoadList.add(electricalLoad);

        thermalLoadList.add(thermalLoad);
        slowProcessList.add(heater);
        thermalLoad.setAsSlow();
        slowProcessList.add(thermalWatchdog);
        thermalWatchdog
                .set(thermalLoad)
                .setLimit(desc.cableDescriptor.thermalWarmLimit, desc.cableDescriptor.thermalCoolLimit)
                .set(new WorldExplosion(this).cableExplosion());

        slowProcessList.add(voltageWatchdog);
        // Electrical poles can handle higher voltages, due to air insulation.
        // This puts utility poles at 4 * Very High Voltage.
        // There'll be a second tier of pole later, handling 16x.
        WorldExplosion exp;
        if (desc.includeTransformer) {
            exp = new WorldExplosion(this).machineExplosion();
        } else {
            exp = new WorldExplosion(this).cableExplosion();
        }
        voltageWatchdog
                .set(electricalLoad)
                .setUMaxMin(desc.cableDescriptor.electricalNominalVoltage * 16)
                .set(exp);

        if (desc.includeTransformer) {
            secondaryLoad = new NbtElectricalLoad("secondaryLoad");
            desc.cableDescriptor.applyTo(secondaryLoad, 4);
            primaryVoltageSource = new VoltageSource("primaryVoltageSource", electricalLoad, null);
            secondaryVoltageSource = new VoltageSource("secondaryVoltageSource", secondaryLoad, null);
            interSystemProcess = new TransformerInterSystemProcess(electricalLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource);
            voltageSecondaryWatchdog = new VoltageStateWatchDog();

            electricalLoadList.add(secondaryLoad);
            electricalComponentList.add(primaryVoltageSource);
            electricalComponentList.add(secondaryVoltageSource);
            slowProcessList.add(voltageSecondaryWatchdog.set(secondaryLoad).set(exp));

            highLoadSoundLooper = new SoundLooper(this) {
                @Override
                public SoundCommand mustStart() {
                    if (secondaryMaxCurrent != 0) {
                        float load = (float) (secondaryLoad.getI() / secondaryMaxCurrent);
                        if (load > desc.minimalLoadToHum)
                            return desc.highLoadSound.copy().mulVolume(0.2f * (load - desc.minimalLoadToHum) / (1 - desc.minimalLoadToHum), 1f).smallRange();
                    }
                    return null;
                }
            };
            slowProcessList.add(highLoadSoundLooper);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        if (desc.includeTransformer)
            Eln.simulator.mna.removeProcess(interSystemProcess);
    }

    @Override
    public void connectJob() {
        if (desc.includeTransformer)
            Eln.simulator.mna.addProcess(interSystemProcess);
        super.connectJob();
    }

    @Override
    public String multiMeterString(Direction side) {
        if (desc.includeTransformer) {
            return Utils.plotVolt("GridU:", electricalLoad.getU()) + Utils.plotAmpere("GridP:", electricalLoad.getCurrent())
                    + Utils.plotVolt("  GroundU:", secondaryLoad.getU()) + Utils.plotAmpere("GroundP:", secondaryLoad.getCurrent());
        } else {
            return super.multiMeterString(side);
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return secondaryLoad;
    }

    @Override
    public ElectricalLoad getGridElectricalLoad(Direction side) {
        return electricalLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (desc.includeTransformer) {
            return NodeBase.maskElectricalPower;
        } else {
            return 0;
        }
    }

    @Override
    public void initialize() {
        if (desc.includeTransformer) {
            setupTransformer();
        }
        super.initialize();
    }

    public void setupTransformer() {
        voltageSecondaryWatchdog.setUNominal(3200);
        secondaryMaxCurrent = (float) desc.cableDescriptor.electricalMaximalCurrent;

        interSystemProcess.setRatio(0.25);
    }

}
