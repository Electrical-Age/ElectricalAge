package mods.eln.transparentnode.electricalmachine;

import mods.eln.Eln;
import mods.eln.api.Brownout;
import mods.eln.api.element.ElementMachine;
import mods.eln.api.MachineSpec;
import mods.eln.api.element.Machines;
import mods.eln.api.RelativeFace;
import mods.eln.i18n.I18N;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ElectricalStackMachineProcess.ElectricalStackMachineProcessObserver;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The macerator, compressor, magnetizer and plate machine (and the tar sand separator): a machine on the Machine API
 * (Machines.forElement). Its spec is the descriptor's nominal volts and power times its speed-up (boosters, an
 * acceleration field), redone with respec when that changes; it runs slower on a sagging line (Brownout.SLOW, as its
 * resistor did) and burns on overvoltage, as before. The recipe work (ElectricalStackMachineProcess) takes a tick's
 * rated energy each tick it runs.
 */
public class ElectricalMachineElement extends TransparentNodeElement implements ElectricalStackMachineProcessObserver {
    private final TransparentNodeElementInventory inventory;
    AutoAcceptInventoryProxy booterAccepter;

    /** The machine; its parts are in this element's lists. */
    public final ElementMachine machine;

    protected final ElectricalStackMachineProcess slowRefreshProcess;

    private final ElectricalMachineSlowProcess slowProcess = new ElectricalMachineSlowProcess(this);
    private boolean powerOn = false;
    final ElectricalMachineDescriptor descriptor;
    public int inSlotId = 0;
    public final int outSlotId = 0;
    private int boosterSlotId = 1;

    public ElectricalMachineElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (ElectricalMachineDescriptor) descriptor;
        inSlotId += this.descriptor.outStackCount;
        boosterSlotId += this.descriptor.outStackCount;
        inventory = new ElectricalMachineInventory(2 + this.descriptor.outStackCount, 64, this);
        booterAccepter = new AutoAcceptInventoryProxy(inventory)
            .acceptIfIncrement(this.descriptor.outStackCount + 1, 5, MachineBoosterDescriptor.class);

        machine = Machines.forElement(this, spec(1));
        slowRefreshProcess = new ElectricalStackMachineProcess(
            inventory, inSlotId, outSlotId, this.descriptor.outStackCount, machine, this.descriptor.recipe);

        slowProcessList.add(slowRefreshProcess);
        slowProcessList.add(slowProcess);
        slowProcessList.add(fieldSpeedProcess);
        slowRefreshProcess.setObserver(this);
        slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 2, 1));
    }

    /**
     * The spec at a speed-up: nominal power times speedUp. Power on the descriptor's power faces. Either polarity
     * runs it, and either burns over 1.3 x nominal (Baughn: fed negative it burned as "overvoltage").
     */
    public MachineSpec spec(double speedUp) {
        return spec(descriptor, speedUp);
    }

    /** The spec of a machine of descriptor at a speed-up (see spec(double)). */
    public static MachineSpec spec(ElectricalMachineDescriptor descriptor, double speedUp) {
        java.util.List<RelativeFace> faces = new java.util.ArrayList<>();
        for (RelativeFace f : RelativeFace.values())
            if (descriptor.powerLrdu(mods.eln.api.impl.Faces.resolve(f, Direction.XN), Direction.XN)) faces.add(f);
        return MachineSpec.consumer(descriptor.nominalU).ratedWatts(descriptor.nominalP * speedUp)
            .onBrownout(Brownout.SLOW).burnsOnOvervoltage().power(faces.toArray(new RelativeFace[0])).build();
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.powerLoad();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        return machine.connectionMask(side);
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(machine.volts(), machine.amps());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;//Utils.plotCelsius("T", thermalLoad.Tc);
    }

    @Override
    public void initialize() {
        inventoryChange(getInventory());
        connect();
    }

    @Override
    public void inventoryChange(Container inventory) {
        super.inventoryChange(inventory);
        setPhysicalValue();
        needPublish();
    }

    /** The speed of time here (NodeBase.timeScale), checked once a second: it scales the machine's power and pace. */
    double fieldSpeed = 1;
    private double fieldCheck;

    private final mods.eln.sim.IProcess fieldSpeedProcess = time -> {
        fieldCheck += time;
        if (fieldCheck < 1) return;
        fieldCheck = 0;
        double s = node.timeScale();
        if (s != fieldSpeed) {
            fieldSpeed = s;
            setPhysicalValue();
        }
    };

    private void setPhysicalValue() {
        ItemStack stack;

        int boosterCount = 0;
        stack = getInventory().getItem(boosterSlotId);
        if (!Utils.isEmpty(stack)) {
            boosterCount = stack.getCount();
        }
        double speedUp = Math.pow(descriptor.boosterSpeedUp, boosterCount) * fieldSpeed;   // (an acceleration field: NodeBase.timeScale)
        slowRefreshProcess.setEfficiency(Math.pow(descriptor.boosterEfficiency, boosterCount));
        if (machine.spec().ratedWatts() != descriptor.nominalP * speedUp) machine.respec(spec(speedUp));
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return booterAccepter.take(entityPlayer.getMainHandItem(), this, false, true);
    }

    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        double fPower = machine.watts() / descriptor.nominalP;
        if (machine.watts() < 11) fPower = 0.0;
        if (fPower > 1.9) fPower = 1.9;
        try {
            stream.writeByte((int) (fPower * 64));
            serialiseItemStack(stream, inventory.getItem(inSlotId));
            serialiseItemStack(stream, inventory.getItem(outSlotId));
            stream.writeFloat((float) slowRefreshProcess.processState());
            stream.writeFloat((float) slowRefreshProcess.processStatePerSecond());
            node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
            stream.writeFloat((float) (machine.volts() / descriptor.nominalU));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putBoolean("powerOn", powerOn);
        double progress = slowRefreshProcess.progressJoules();
        if (progress > 0) nbt.putDouble("progressJ", progress);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        powerOn = nbt.getBooleanOr("powerOn", false);
        slowRefreshProcess.resume(nbt.getDoubleOr("progressJ", 0)); // (saves before 26.x's machine API kept no progress)
    }

    @Override
    public void done(ElectricalStackMachineProcess who) {
        needPublish();
        if (descriptor.endSound != null)
            play(descriptor.endSound);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.putAll(machine.wailaLines());   // volts, the power drawn, and the machine's state when it isn't running
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Power provided"), Utils.plotPower("", machine.watts()));
        }
        return info;
    }
}
