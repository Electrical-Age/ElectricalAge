package mods.eln.sixnode.busdevice;

import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/** The Addressable Button (AddressableButtonDescriptor). */
public class AddressableButtonElement extends BusDeviceElement {
    /** Ticks the button stays pushed in after a press (the Signal Button's 0.21 s, rounded up). */
    static final int PUSHED_TICKS = 5;

    /** Its lamp: the last power sent to it, over half. Saved as "lit". */
    boolean lit;
    int pushed;
    /** Presses sent (tests). */
    public int presses;

    public AddressableButtonElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
    }

    @Override
    protected void handle(Packet p) {
        if (p.type() == PacketType.POWER && p.target() == address() && (p.value() > 0.5f) != lit) {
            lit = p.value() > 0.5f;
            needPublish();
        }
    }

    @Override
    protected void tick() {
        if (pushed > 0 && --pushed == 0) needPublish();
    }

    /** Press it: `press` (index 0) to every device, from its address. */
    public void press() {
        send(new Packet(Packet.BROADCAST, address(), PacketType.PRESS, 0, 0));
        presses++;
        pushed = PUSHED_TICKS;
        needPublish();
    }

    public boolean lit() {
        return lit;
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(player)) return true;
        if (!player.getMainHandItem().isEmpty() || player.isShiftKeyDown()) return false;   // (sneaking: its screen)
        if (player.level().isClientSide()) return true;
        press();
        play(player);
        return true;
    }

    void play(Player player) {
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,
            net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.2f);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(pushed > 0);
            stream.writeBoolean(lit);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lit = nbt.getBooleanOr("lit", false);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (lit) nbt.putBoolean("lit", true);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Lamp"), lit ? I18N.tr("ON") : I18N.tr("OFF"));
        return info;
    }
}
