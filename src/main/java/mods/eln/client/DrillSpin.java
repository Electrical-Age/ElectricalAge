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
 * The hand drill's spin (Baughn 2026-10-30: it turns when used, and it gets sounds): while a player digs with a charged
 * drill in the main hand, the bit spins up over half a second, and runs down over a second after. The local player
 * digs while the attack key holds on a block; another player while their arm swings (the server shows every player's
 * digging swing). Each drill's bit angle goes to the item draw by its stack (ElectricalPickaxeKt.spinAngles), so it
 * turns in the first person view, in third person, and in other players' hands. While the local player's drill turns,
 * the first-person view holds it steady with a slight shake instead of the swing (the swing hid the bit: nobody had seen
 * it turn). A whirring grind loop plays while the local drill turns, louder and higher with the spin.
 */
final class DrillSpin {
    private DrillSpin() {
    }

    /** Rate at full spin (degrees a tick: 4 turns a second). */
    static final float FULL = 72;
    /** Each player's drill: spin 0..1, the bit's angle last tick and now. */
    static final java.util.Map<java.util.UUID, float[]> spins = new java.util.HashMap<>();
    /** The local player's spin (for the sound and the steady hand). */
    static float spin;
    static Sound playing;

    static ElectricalPickaxe drill(net.minecraft.world.item.ItemStack stack) {
        return GenericItemUsingDamageDescriptor.getDescriptor(stack) instanceof ElectricalPickaxe d ? d : null;
    }

    /** Is the local player digging with a charged drill (the attack key held on a block)? */
    static boolean digging(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null) return false;
        var stack = mc.player.getMainHandItem();
        var drill = drill(stack);
        if (drill == null || !drill.canDig(stack)) return false;
        return mc.options.keyAttack.isDown() && mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
    }

    static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            spins.clear();
            spin = 0;
            return;
        }
        java.util.Set<java.util.UUID> here = new java.util.HashSet<>();
        for (var p : mc.level.players()) {
            var stack = p.getMainHandItem();
            if (drill(stack) == null && !spins.containsKey(p.getUUID())) continue;
            here.add(p.getUUID());
            boolean on = p == mc.player ? digging(mc) : drill(stack) != null && p.getCurrentSwing() != null;
            float[] st = spins.computeIfAbsent(p.getUUID(), k -> new float[3]);
            st[0] = on ? Math.min(1, st[0] + 0.1f) : Math.max(0, st[0] - 0.05f);
            st[1] = st[2];
            st[2] = (st[2] + FULL * st[0] * st[0]) % 360;
        }
        spins.keySet().removeIf(u -> !here.contains(u));
        float[] mine = mc.player == null ? null : spins.get(mc.player.getUUID());
        spin = mine == null ? 0 : mine[0];
        if (spin > 0 && (playing == null || playing.isStopped()) && mc.player != null) {
            var e = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.fromNamespaceAndPath("eln", "drill"));
            if (e != null) mc.getSoundManager().play(playing = new Sound(e));
        }
    }

    /** Each frame: each drill's angle between ticks, by the stack in its player's hand. */
    static void frame(float partial) {
        var mc = Minecraft.getInstance();
        ElectricalPickaxeKt.spinAngles.clear();
        if (mc.level == null) return;
        for (var p : mc.level.players()) {
            float[] st = spins.get(p.getUUID());
            if (st == null) continue;
            float a = st[2] < st[1] ? st[2] + 360 : st[2];
            ElectricalPickaxeKt.spinAngles.put(p.getMainHandItem(), st[1] + (a - st[1]) * partial);
        }
    }

    /**
     * The first-person hold while the local drill turns: where the arm holds it at rest (vanilla's arm transform), no
     * swing, a slight shake with the spin; else vanilla's (false).
     */
    static boolean handTransform(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.world.entity.HumanoidArm arm, net.minecraft.world.item.ItemStack stack,
                                 float partial, float equip) {
        if (spin <= 0.05f || drill(stack) == null) return false;
        int invert = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        float t = (net.minecraft.util.Util.getMillis() % 100000) / 1000f;
        float shake = 0.006f * spin;
        pose.translate(invert * 0.56F + shake * (float) Math.sin(t * 91), -0.52F + equip * -0.6F + shake * (float) Math.sin(t * 77), -0.72F - 0.04f * spin);
        return true;
    }

    /** Registers the steady hold for the drills' items. */
    static void registerExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) {
        java.util.List<net.minecraft.world.item.Item> items = new java.util.ArrayList<>();
        for (var d : mods.eln.Eln.sharedItemStackOne.all().values())
            if (d instanceof ElectricalPickaxe && d.parentItem != null) items.add(d.parentItem);
        if (items.isEmpty()) return;
        e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.state.level.PlayerRenderState state,
                                                   net.minecraft.world.entity.HumanoidArm arm, net.minecraft.world.item.ItemStack stack, float partial, float equip, float swing) {
                return handTransform(pose, arm, stack, partial, equip);
            }
        }, items.toArray(new net.minecraft.world.item.Item[0]));
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
