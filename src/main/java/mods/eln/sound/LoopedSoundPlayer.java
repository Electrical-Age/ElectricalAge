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
 * Plays the element renders' looped sounds (1.12 LoopedSoundManager; client only). OpenAL has a fixed pool of sources
 * (about 247 for sounds like these) and a full pool silences every new sound, vanilla's too: so every half second
 * the loops that are audible here (volume, fading to nothing over the 16 blocks of linear attenuation) are ranked and
 * the MAX_PLAYING loudest get a ticking sound instance. An instance follows its loop's volume and pitch, and stops when
 * its loop or block entity goes, when it has been silent SILENT_TICKS, or when it drops out of the ranking.
 */
public final class LoopedSoundPlayer {
    private LoopedSoundPlayer() {
    }

    static final double RANGE = 16, RANGE_SQ = 32 * 32;
    /** At most this many EA loops at once (Baughn: 1.7.10 EA ran the pool dry); and below AUDIBLE a loop is silence. */
    public static final int MAX_PLAYING = 24;
    public static final float AUDIBLE = 0.002f;
    static final int SILENT_TICKS = 40;
    static final Map<LoopedSound, Instance> PLAYING = new WeakHashMap<>();
    static float timer;

    static final class Instance extends AbstractTickableSoundInstance {
        final LoopedSound loop;
        final NodeBlockEntity owner;
        int silent;
        /** Out of the ranking: stop at the next tick. */
        boolean evicted;

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

        /** A loop fades in from near silence: the engine must not drop it at volume 0. */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (evicted || !loop.getActive() || owner.isRemoved()) {
                stop();
                return;
            }
            volume = Math.max(0f, loop.getVolume());
            pitch = Math.max(0.01f, loop.getPitch());
            silent = volume < AUDIBLE ? silent + 1 : 0;
            if (silent > SILENT_TICKS) stop();
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

    /** How loud a loop is at this distance (blocks): its volume, faded linearly to nothing at RANGE unless it doesn't fade. */
    public static float heard(float volume, double distance, boolean attenuates) {
        return attenuates ? volume * (float) Math.max(0, 1 - distance / RANGE) : volume;
    }

    /** The indices of the at most max loudest of these levels that are audible, loudest first. */
    public static List<Integer> ranked(float[] heard, int max) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < heard.length; i++) if (heard[i] >= AUDIBLE) idx.add(i);
        idx.sort((a, b) -> Float.compare(heard[b], heard[a]));
        return idx.size() > max ? new ArrayList<>(idx.subList(0, max)) : idx;
    }

    /** From the per-frame refresh. */
    public static void update(float deltaT) {
        timer += deltaT;
        if (timer < 0.5f) return;
        timer = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PLAYING.values().removeIf(Instance::isStopped);
        List<LoopedSound> found = new ArrayList<>();
        List<NodeBlockEntity> owners = new ArrayList<>();
        for (NodeBlockEntity be : new ArrayList<>(NodeBlockEntity.clientList)) {
            if (be.isRemoved()) continue;
            if (be.getBlockPos().distToCenterSqr(mc.player.position()) > RANGE_SQ) continue;
            for (LoopedSound loop : loops(be)) {
                if (!loop.getActive()) continue;
                found.add(loop);
                owners.add(be);
            }
        }
        float[] heard = new float[found.size()];
        for (int i = 0; i < heard.length; i++) {
            LoopedSound loop = found.get(i);
            double d = Math.sqrt(owners.get(i).getBlockPos().distToCenterSqr(mc.player.position()));
            heard[i] = heard(loop.getVolume(), d, loop.getAttenuation() != LoopedSound.Attenuation.NONE);
        }
        java.util.Set<LoopedSound> keep = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (int i : ranked(heard, MAX_PLAYING)) keep.add(found.get(i));
        for (Instance i : PLAYING.values()) if (!keep.contains(i.loop)) i.evicted = true;
        for (int k = 0; k < found.size(); k++) {
            LoopedSound loop = found.get(k);
            if (!keep.contains(loop) || PLAYING.containsKey(loop)) continue;
            SoundEvent e = SoundClient.event(loop.getSample());
            if (e == null) continue;
            Instance i = new Instance(loop, owners.get(k), e);
            PLAYING.put(loop, i);
            mc.getSoundManager().play(i);
        }
    }
}
