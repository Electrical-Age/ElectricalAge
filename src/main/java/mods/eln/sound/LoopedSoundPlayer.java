package mods.eln.sound;

import mods.eln.node.NodeBlockEntity;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Plays the element renders' looped sounds (1.12 LoopedSoundManager; client only): every half second each loaded
 * node's loops within 32 blocks get a ticking sound instance, which follows the loop's volume and pitch and stops when
 * the loop or its block entity goes.
 */
public final class LoopedSoundPlayer {
    private LoopedSoundPlayer() {
    }

    static final double RANGE_SQ = 32 * 32;
    static final Map<LoopedSound, Instance> PLAYING = new WeakHashMap<>();
    static float timer;

    static final class Instance extends AbstractTickableSoundInstance {
        final LoopedSound loop;
        final NodeBlockEntity owner;

        Instance(LoopedSound loop, NodeBlockEntity owner, SoundEvent event) {
            super(event, ElnSounds.category(loop.getSample()), SoundInstance.createUnseededRandom());
            this.loop = loop;
            this.owner = owner;
            this.looping = true;
            this.delay = 0;
            this.x = loop.getCoord().x + 0.5;
            this.y = loop.getCoord().y + 0.5;
            this.z = loop.getCoord().z + 0.5;
            this.attenuation = loop.getAttenuation() == LoopedSound.Attenuation.NONE ? Attenuation.NONE : Attenuation.LINEAR;
            tick();
        }

        /** A loop starts silent (machine idle) and rises: the engine must not drop it at volume 0. */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (!loop.getActive() || owner.isRemoved()) {
                stop();
                return;
            }
            volume = Math.max(0f, loop.getVolume());
            pitch = Math.max(0.01f, loop.getPitch());
        }
    }

    static List<LoopedSound> loops(NodeBlockEntity be) {
        List<LoopedSound> out = new ArrayList<>();
        if (be instanceof SixNodeEntity six) {
            for (SixNodeElementRender r : six.elementRenderList) if (r != null) out.addAll(r.loopedSounds);
        } else if (be instanceof TransparentNodeEntity t && t.elementRender != null) {
            out.addAll(t.elementRender.loopedSounds);
        }
        return out;
    }

    /** From the per-frame refresh. */
    public static void update(float deltaT) {
        timer += deltaT;
        if (timer < 0.5f) return;
        timer = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PLAYING.values().removeIf(Instance::isStopped);
        for (NodeBlockEntity be : new ArrayList<>(NodeBlockEntity.clientList)) {
            if (be.isRemoved()) continue;
            if (be.getBlockPos().distToCenterSqr(mc.player.position()) > RANGE_SQ) continue;
            for (LoopedSound loop : loops(be)) {
                if (!loop.getActive() || PLAYING.containsKey(loop)) continue;
                SoundEvent e = SoundClient.event(loop.getSample());
                if (e == null) continue;
                Instance i = new Instance(loop, be, e);
                PLAYING.put(loop, i);
                mc.getSoundManager().play(i);
            }
        }
    }
}
