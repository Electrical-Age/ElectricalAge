package mods.eln.sixnode.groundcable;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GroundCableElement extends SixNodeElement {

    NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    VoltageSource ground = new VoltageSource("ground", electricalLoad, null);
    //ElectricalSourceRefGroundProcess groundProcess = new ElectricalSourceRefGroundProcess(electricalLoad, 0);

    int color = 0;
    int colorCare = 0;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public GroundCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(electricalLoad);
        electricalComponentList.add(ground);
        ground.setU(0);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte b = nbt.getByteOr("color", (byte) 0);
        color = b & 0xF;
        colorCare = (b >> 4) & 1;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("color", (byte) (color + (colorCare << 4)));
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        return electricalLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        //if (Utils.isEmpty(inventory.getItem(0 /* 1.12 GroundCableContainer.cableSlotId */))) return 0;
        return NodeBase.maskElectricalPower + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U:", electricalLoad.getU()) + Utils.plotAmpere("I:", electricalLoad.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Current"), Utils.plotAmpere("", electricalLoad.getI()));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(color << 4);
            Utils.serialiseItemStack(stream, inventory.getItem(0 /* 1.12 GroundCableContainer.cableSlotId */));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        ElnDeviceRegistry.applySmallRs(electricalLoad);
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack currentItemStack = entityPlayer.getMainHandItem();
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            colorCare = colorCare ^ 1;
            Utils.sendMessage(entityPlayer, "Wire color care " + colorCare);
            sixNode.reconnect();
        } else if (!Utils.isEmpty(currentItemStack)) {
            Item item = currentItemStack.getItem();

            GenericItemUsingDamageDescriptor gen = BrushDescriptor.getDescriptor(currentItemStack);
            if (gen != null && gen instanceof BrushDescriptor) {
                BrushDescriptor brush = (BrushDescriptor) gen;
                int brushColor = brush.getColor(currentItemStack);
                if (brushColor != color && brush.use(currentItemStack, entityPlayer)) {
                    color = brushColor;
                    sixNode.reconnect();
                }
            }
        }
        return false;
    }

    @Override
    protected void inventoryChanged() {
        super.inventoryChanged();
        reconnect();
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container newContainer(Direction side, Player player) {
        return null; // GroundCableContainer: menus come with P4
    }
}
