package mods.eln.sixnode.thermalcable;

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
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThermalCableElement extends SixNodeElement {

    ThermalCableDescriptor descriptor;

    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    final mods.eln.misc.Incandescence.Light glow = new mods.eln.misc.Incandescence.Light();
    int glowStep;

    @Override
    public int getLightValue() {
        return glow.level;
    }

    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();

    int color = 0;
    /** Whether it joins only wires of its colour. 1.12 toggled it with the wrench; the toggle is gone (Baughn 2026-11-21:
     *  obsolete, six-node cables are legacy), and a placed cable keeps what it saved, so old networks keep their joins. */
    int colorCare = 1;

    public ThermalCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (ThermalCableDescriptor) descriptor;

        thermalLoadList.add(thermalLoad);

        slowProcessList.add(thermalWatchdog);
        // hot, it glows and lights its block (Incandescence): publish when the glow's look changes
        slowProcessList.add(t -> {
            double c = mods.eln.misc.Incandescence.celsius(thermalLoad.Tc);
            int step = mods.eln.misc.Incandescence.step(c);
            if (step != glowStep) {
                glowStep = step;
                needPublish();
            }
            if (glow.update(sixNode.coordonate.world(), c)) sixNode.recalculateLightValue();
        });

        thermalWatchdog
            .set(thermalLoad)
            .setLimit(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
            .set(new Failure(this).cable());
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte b = nbt.getByteOr("color", (byte) 0);
        color = b & 0xF;
        colorCare = (b >> 4) & 1;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("color", (byte) (color + (colorCare << 4)));
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return thermalLoad;
    }

    /** Its temperature (C above ambient): the voxel-cable converter. */
    public double thermalTc() {
        return thermalLoad.Tc;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return NodeBase.maskThermalWire + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        return mods.eln.compat.jade.ProbeLines.of()
            .headline(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.getT()))
            .headline(I18N.tr("Thermal power"), Utils.plotPower("", thermalLoad.getPower()));
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
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (Utils.isPlayerUsingWrench(entityPlayer)) return true;   // (a wire has nothing to turn; the click is used, as on every device)
        color = brushPaint(entityPlayer, color);
        return false;
    }
}
