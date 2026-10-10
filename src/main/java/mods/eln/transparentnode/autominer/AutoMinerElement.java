package mods.eln.transparentnode.autominer;

import mods.eln.i18n.I18N;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AutoMinerElement extends TransparentNodeElement {

    AutoAcceptInventoryProxy inventory =
        (new AutoAcceptInventoryProxy(new TransparentNodeElementInventory(AutoMinerContainer.inventorySize, 64, this)))
            .acceptIfIncrement(2, 64, MiningPipeDescriptor.class)
            .acceptIfEmpty(0, ElectricalDrillDescriptor.class);

    NbtElectricalLoad inPowerLoad = new NbtElectricalLoad("inPowerLoad");
    AutoMinerSlowProcess slowProcess = new AutoMinerSlowProcess(this);
    Resistor powerResistor = new Resistor(inPowerLoad, null);

    final AutoMinerDescriptor descriptor;

    Coordonate lightCoordonate;

    private final VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    private final ArrayList<AutoMinerPowerNode> powerNodeList = new ArrayList<AutoMinerPowerNode>();

    boolean powerOk = false;

    // Network IDs.
    public static final byte pushLogId = 1;
    public static final byte toggleSilkTouch = 2;

    public AutoMinerElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (AutoMinerDescriptor) descriptor;
        electricalLoadList.add(inPowerLoad);
        electricalComponentList.add(powerResistor);
        slowProcessList.add(slowProcess);

        Failure exp = new Failure(this).machine();
        slowProcessList.add(voltageWatchdog.set(inPowerLoad).setUNominal(this.descriptor.nominalVoltage).set(exp));
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return inPowerLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return NodeBase.maskElectricalPower;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(inPowerLoad.getU(), inPowerLoad.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    @Override
    public void initialize() {
        lightCoordonate = new Coordonate(this.descriptor.lightCoord);
        lightCoordonate.applyTransformation(front, node.coordonate);

        int idx = 0;
        for (Coordonate c : descriptor.getPowerCoordonate(node.coordonate.world())) {
            AutoMinerPowerNode n = new AutoMinerPowerNode();
            n.setElement(this);
            c.applyTransformation(front, node.coordonate);

            Direction dir;
            if (idx != 0)
                dir = front.left();
            else
                dir = front.right();

            n.onBlockPlacedBy(c, dir, null, null);

            powerNodeList.add(n);
            idx++;
        }

        descriptor.applyTo(inPowerLoad);

        connect();
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        slowProcess.onBreakElement();

        for (AutoMinerPowerNode n : powerNodeList) {
            n.onBreakBlock();
        }
        powerNodeList.clear();
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return inventory.take(entityPlayer.getMainHandItem());
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu newContainer(Direction side, Player player) {
        return null; // AutoMinerContainer: menus come with P4
    }

    @Override
    public Container getInventory() {
        return inventory.getInventory();
    }

    @Override
    public void ghostDestroyed(int UUID) {
        if (UUID == descriptor.getGhostGroupUuid()) {
            super.ghostDestroyed(UUID);
        }
        slowProcess.ghostDestroyed();
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeShort(slowProcess.pipeLength);
            stream.writeByte(slowProcess.job.ordinal());
            stream.writeBoolean(powerOk);
            stream.writeBoolean(slowProcess.silkTouch);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPowerOk(boolean b) {
        if (powerOk != (powerOk = b)) {
            needPublish();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putBoolean("powerOk", powerOk);
        nbt.putBoolean("silkTouch", slowProcess.silkTouch);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        powerOk = nbt.getBooleanOr("powerOk", false);
        slowProcess.silkTouch = nbt.getBooleanOr("silkTouch", false);
    }

    void pushLog(String log) {
        sendStringToAllClient(pushLogId, log);
    }

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte packetType = super.networkUnserialize(stream);
        switch (packetType) {
            case toggleSilkTouch:
                slowProcess.toggleSilkTouch();
                needPublish();
                break;
            default:
                return packetType;
        }
        return unserializeNulldId;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Silk touch"), slowProcess.silkTouch ? I18N.tr("Yes") : I18N.tr("No"));
        info.put(I18N.tr("Depth"), Utils.plotValue(slowProcess.pipeLength, "m "));
        return info;
    }
}
