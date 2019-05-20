package mods.eln.sixnode.electricalentitysensor;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalEntitySensorElement extends SixNodeElement {

    ElectricalEntitySensorDescriptor descriptor;

    public NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");
    public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
    public ElectricalEntitySensorSlowProcess slowProcess = new ElectricalEntitySensorSlowProcess(this);

    private AutoAcceptInventoryProxy inventory = (new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this)))
        .acceptAlways(0, 1, new AutoAcceptInventoryProxy.SimpleItemDropper(sixNode), EntitySensorFilterDescriptor.class);

    public ElectricalEntitySensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);
        slowProcessList.add(slowProcess);
        this.descriptor = (ElectricalEntitySensorDescriptor) descriptor;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (front == lrdu.left()) return outputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu.left()) return NodeBase.MASK_ELECTRICAL_OUTPUT_GATE;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U:", outputGate.getU()) + Utils.plotAmpere("I:", outputGate.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Entity present"), slowProcess.state ? I18N.tr("Yes") : I18N.tr("No"));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Output voltage"), Utils.plotVolt("", outputGate.getU()));
        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void initialize() {
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(entityPlayer)) return true;
        return inventory.take(entityPlayer.getCurrentEquippedItem());
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public IInventory getInventory() {
        if (inventory != null)
            return inventory.getInventory();
        else
            return null;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ElectricalEntitySensorContainer(player, inventory.getInventory());
    }

    @Override
    protected void inventoryChanged() {
        super.inventoryChanged();
        needPublish();
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(slowProcess.state);
            Utils.serialiseItemStack(stream, getInventory().getStackInSlot(ElectricalEntitySensorContainer.filterId));
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}
