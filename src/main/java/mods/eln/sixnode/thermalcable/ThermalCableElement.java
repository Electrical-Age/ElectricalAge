package mods.eln.sixnode.thermalcable;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataOutputStream;
import java.io.IOException;

public class ThermalCableElement extends SixNodeElement {

    ThermalCableDescriptor descriptor;

    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");

    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();

    int color = 0;
    int colorCare = 1;

    public ThermalCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (ThermalCableDescriptor) descriptor;

        thermalLoadList.add(thermalLoad);

        slowProcessList.add(thermalWatchdog);

        thermalWatchdog
                .set(thermalLoad)
                .setLimit(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
                .set(new WorldExplosion(this).cableExplosion());
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte b = nbt.getByte("color");
        color = b & 0xF;
        colorCare = (b >> 4) & 1;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("color", (byte) (color + (colorCare << 4)));
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return NodeBase.maskThermalWire + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @Override
    public String thermoMeterString() {
        return Utils.plotCelsius("T", thermalLoad.Tc) + Utils.plotPower("P", thermalLoad.getPower());
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte((color << 4));
            stream.writeShort((short) (thermalLoad.Tc * NodeBase.networkSerializeTFactor));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        descriptor.setThermalLoad(thermalLoad);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            colorCare = colorCare ^ 1;
            Utils.addChatMessage(entityPlayer, "Wire color care " + colorCare);
            sixNode.reconnect();
        } else if (currentItemStack != null) {
            Item item = currentItemStack.getItem();

            GenericItemUsingDamageDescriptor gen = BrushDescriptor.getDescriptor(currentItemStack);
            if (gen != null && gen instanceof BrushDescriptor) {
                BrushDescriptor brush = (BrushDescriptor) gen;
                int brushColor = brush.getColor(currentItemStack);
                if (brushColor != color) {
                    if (brush.use(currentItemStack)) {
                        color = brushColor;
                        sixNode.reconnect();
                    } else {
                        Utils.addChatMessage(entityPlayer, "Brush is empty!");
                    }
                }
            }
        }
        return false;
    }
}
