package mods.eln.sixnode.currentcable;

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
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CurrentCableElement extends SixNodeElement {

    public NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");

    ElectricalLoadHeatThermalLoad heater = new ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad);
    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();

    CurrentCableDescriptor descriptor;

    int color;
    int colorCare;

    public CurrentCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (CurrentCableDescriptor) descriptor;

        color = 0;
        colorCare = 1;
        electricalLoad.setCanBeSimplifiedByLine(true);
        electricalLoadList.add(electricalLoad);

        thermalLoadList.add(thermalLoad);
        thermalSlowProcessList.add(heater);
        thermalLoad.setAsSlow();
        slowProcessList.add(thermalWatchdog);
        thermalWatchdog.set(thermalLoad).setLimit(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit).set(new WorldExplosion(this).cableExplosion());
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
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return electricalLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return descriptor.getNodeMask() /*+ NodeBase.maskElectricalWire*/ + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
    }

    @Override
    public void initialize() {
        descriptor.applyTo(electricalLoad);
        descriptor.applyTo(thermalLoad);
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(electricalLoad.getU(), electricalLoad.getI());
    }

    @Override
    public String thermoMeterString() {
        return Utils.plotCelsius("T", thermalLoad.Tc);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();

        info.put(I18N.tr("Current"), Utils.plotAmpere("", electricalLoad.getI()));
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.getT()));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Voltage"), Utils.plotVolt("", electricalLoad.getU()));
        }

        return info;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(color << 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                if (brushColor != color && brush.use(currentItemStack, entityPlayer)) {
                    color = brushColor;
                    sixNode.reconnect();
                }
            }
        }
        return false;
    }
}
