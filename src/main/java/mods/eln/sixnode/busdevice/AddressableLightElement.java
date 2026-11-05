package mods.eln.sixnode.busdevice;

import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.nbt.CompoundTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/** The Addressable Light (AddressableLightDescriptor). */
public class AddressableLightElement extends BusDeviceElement {
    /** The last power sent to it, 0..1. Saved as "level". */
    float level;

    public AddressableLightElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
    }

    @Override
    protected void handle(Packet p) {
        if (p.type() == PacketType.POWER && p.target() == address() && p.value() != level) {
            level = p.value();
            sixNode.recalculateLightValue();
            needPublish();
        }
    }

    public float level() {
        return level;
    }

    @Override
    public int getLightValue() {
        return Math.round(level * AddressableLightDescriptor.MAX_LIGHT);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat(level);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        level = Math.max(0, Math.min(1, nbt.getFloatOr("level", 0)));
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (level != 0) nbt.putFloat("level", level);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Power"), Math.round(level * 100) + "%");
        return info;
    }
}
