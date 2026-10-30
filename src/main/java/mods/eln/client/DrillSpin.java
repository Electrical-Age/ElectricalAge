package mods.eln.client;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalitem.ElectricalPickaxe;
import mods.eln.item.electricalitem.ElectricalPickaxeKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * The hand drill's spin (Baughn 2026-10-30: it turns when used, and it gets sounds): while the local player digs with a
 * charged drill in the main hand (the attack key held on a block), the bit spins up over half a second, and runs down
 * over a second after; the bit's angle goes to the item draw (ElectricalPickaxeKt.spinAngle). A whirring grind loop
 * plays while it turns, louder and higher with the spin. Other players' drills are drawn still.
 */
final class DrillSpin {
    private DrillSpin() {
    }

    /** Spin 0..1 and its rate at full spin (degrees a tick: 4 turns a second). */
    static float spin, prevAngle, angle;
    static final float FULL = 72;
    static Sound playing;

    static boolean digging(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null) return false;
        var stack = mc.player.getMainHandItem();
        if (!(GenericItemUsingDamageDescriptor.getDescriptor(stack) instanceof ElectricalPickaxe drill) || !drill.canDig(stack)) return false;
        return mc.options.keyAttack.isDown() && mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
    }

    static void tick() {
        var mc = Minecraft.getInstance();
        boolean on = digging(mc);
        spin = on ? Math.min(1, spin + 0.1f) : Math.max(0, spin - 0.05f);
        prevAngle = angle;
        angle = (angle + FULL * spin * spin) % 360;
        if (spin > 0 && (playing == null || playing.isStopped()) && mc.player != null) {
            var e = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.fromNamespaceAndPath("eln", "drill"));
            if (e != null) mc.getSoundManager().play(playing = new Sound(e));
        }
    }

    /** Each frame: the angle between ticks. */
    static void frame(float partial) {
        float a = angle < prevAngle ? angle + 360 : angle;
        ElectricalPickaxeKt.spinAngle = prevAngle + (a - prevAngle) * partial;
    }

    static final class Sound extends AbstractTickableSoundInstance {
        Sound(net.minecraft.sounds.SoundEvent e) {
            super(e, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            volume = 0;
            follow();
        }

        void follow() {
            var p = Minecraft.getInstance().player;
            if (p == null) return;
            x = p.getX();
            y = p.getEyeY() - 0.3;
            z = p.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().player == null || spin <= 0) {
                stop();
                return;
            }
            follow();
            volume = 0.45f * spin;
            pitch = 0.6f + 0.5f * spin;
        }
    }
}
