package mods.eln.sixnode.powersocket;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A wireless outlet (AGE's power socket, 3f3b28c5; 1.12 left it a copy of the lamp supply that did nothing): it takes
 * power from the nearest lamp supply on its channel, within that supply's range and while the channel is on, and puts
 * it on the cable at its front. An internal source follows the supply's voltage through LINK_R; what the socket's
 * cable draws is charged to the supply as a load (as lamp sockets are). Without a supply it is a dead cable end.
 */
public class PowerSocketElement extends SixNodeElement {

    public static final HashMap<String, ArrayList<PowerSocketElement>> channelMap = new HashMap<String, ArrayList<PowerSocketElement>>();

    //NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate");
    public PowerSocketDescriptor descriptor;

    public NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    /** The supply's side: a source at its voltage, linked to the socket's cable through LINK_R (open: no supply). */
    final NbtElectricalLoad sourceLoad = new NbtElectricalLoad("sourceLoad");
    final mods.eln.sim.mna.component.VoltageSource source = new mods.eln.sim.mna.component.VoltageSource("source", sourceLoad, null);
    public final Resistor link = new Resistor(sourceLoad, powerLoad);
    public static final double LINK_R = 0.1;
    /** The supply feeding it at the last look, or null. */
    public mods.eln.sixnode.lampsupply.LampSupplyElement supply;
    public IProcess PowerSocketSlowProcess = new PowerSocketSlowProcess();

    /**
     * 1.12's cable slot index (never in a 1.12 container; Baughn 2026-10-17: the socket has a built-in cable). Kept,
     * hidden, so a cable put there by an older world stays in the saved data and drops when the socket is broken.
     */
    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public String channel = "Default channel";

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    public static final byte setChannelId = 1;


    @Override
    public Container getInventory() {
        return inventory;
    }

    public PowerSocketElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(powerLoad);
        electricalLoadList.add(sourceLoad);
        electricalComponentList.add(source);
        electricalComponentList.add(link);
        slowProcessList.add(PowerSocketSlowProcess);
        link.highImpedance();
        this.descriptor = (PowerSocketDescriptor) descriptor;

        slowProcessList.add(voltageWatchdog);
        voltageWatchdog
            .set(powerLoad)
            .set(new Failure(this).electronics());

        channelRegister(this);
    }

    class PowerSocketSlowProcess implements IProcess {

        @Override
        public void process(double time) {
            var best = nearestSupply();
            supply = best == null ? null : best.element;
            if (best != null && best.element.getChannelState(best.id)) {
                double u = best.element.powerLoad.getU();
                source.setU(u);
                link.setR(LINK_R);
                // what the cable draws, charged to the supply as a resistance (U / I)
                double i = link.getCurrent();
                if (i > 1e-6 && u > 1e-3) best.element.addToRp(u / i);
            } else {
                link.highImpedance();
                source.setU(0);
            }
        }
    }

    /** The nearest live lamp supply on this channel within its range (as LampSocketProcess picks one). */
    mods.eln.sixnode.lampsupply.LampSupplyElement.PowerSupplyChannelHandle nearestSupply() {
        var list = mods.eln.sixnode.lampsupply.LampSupplyElement.channelMap.get(channel);
        if (list == null) return null;
        mods.eln.sixnode.lampsupply.LampSupplyElement.PowerSupplyChannelHandle best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var s : list) {
            if (s.element.isBurnt() || s.element.sixNode == null || !s.element.sixNode.isAdded() || s.element.sixNode.isDestructing()) continue;
            double d = s.element.sixNode.coordonate.trueDistanceTo(sixNode.coordonate);
            if (d < bestDistance && d <= s.element.getRange()) {
                bestDistance = d;
                best = s;
            }
        }
        return best;
    }

    static void channelRegister(PowerSocketElement tx) {
        String channel = tx.channel;
        ArrayList<PowerSocketElement> list = channelMap.get(channel);
        if (list == null)
            channelMap.put(channel, list = new ArrayList<PowerSocketElement>());
        list.add(tx);
    }

    static void channelRemove(PowerSocketElement tx) {
        String channel = tx.channel;
        List<PowerSocketElement> list = channelMap.get(channel);
        if (list == null) return;
        list.remove(tx);
        if (list.isEmpty())
            channelMap.remove(channel);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return powerLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return NodeBase.maskElectricalPower;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(powerLoad.getU(), powerLoad.getCurrent());
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
        setupFromInventory();
    }

    @Override
    public void destroy(ServerPlayer entityPlayer) {
        super.destroy(entityPlayer);
        unregister();
    }

    @Override
    public void unload() {
        super.unload();
        channelRemove(this);
    }

    void unregister() {
        channelRemove(this);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putString("channel", channel);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        channelRemove(this);

        super.readFromNBT(nbt);
        channel = nbt.getStringOr("channel", "");

        channelRegister(this);
    }

    void setupFromInventory() {
        // the built-in cable's resistance, without its watchdog (the 10 kV nominal of 1.12's empty slot: off)
        descriptor.cable().applyTo(powerLoad);
        voltageWatchdog.setUNominal(10000);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);

        try {
            switch (stream.readByte()) {
                case setChannelId:
                    channelRemove(this);
                    channel = stream.readUTF();
                    needPublish();
                    channelRegister(this);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(channel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getRange() {
        return descriptor.range;
    }
}
