package mods.eln.sound;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The looped-sound budget: silent and far loops take no OpenAL source, and a crowd of machines takes MAX_PLAYING. */
public class LoopedSoundPlayerTest {
    @Test
    void silentAndOutOfEarshotTakeNoSource() {
        assertEquals(0f, LoopedSoundPlayer.heard(1f, 16, true));
        assertEquals(0f, LoopedSoundPlayer.heard(1f, 30, true));
        assertEquals(0.5f, LoopedSoundPlayer.heard(1f, 8, true), 1e-6);
        assertEquals(0.3f, LoopedSoundPlayer.heard(0.3f, 30, false), 1e-6);
        float[] heard = {0f, LoopedSoundPlayer.heard(1f, 20, true), 0.001f, 0.2f};
        assertEquals(List.of(3), LoopedSoundPlayer.ranked(heard, LoopedSoundPlayer.MAX_PLAYING));
    }

    @Test
    void aBigPlantPlaysOnlyTheLoudest() {
        // 300 transformers and turbines around the player, at all distances: the pool keeps its ~247 sources
        float[] heard = new float[300];
        for (int i = 0; i < heard.length; i++) heard[i] = LoopedSoundPlayer.heard(0.2f, i * 0.05, true);
        List<Integer> r = LoopedSoundPlayer.ranked(heard, LoopedSoundPlayer.MAX_PLAYING);
        assertEquals(LoopedSoundPlayer.MAX_PLAYING, r.size());
        for (int k = 0; k < r.size(); k++) assertEquals(k, r.get(k));   // the nearest, loudest first
        assertTrue(LoopedSoundPlayer.MAX_PLAYING < 64);
    }

    /** Baughn 2026-11-07: the autominer cut out abruptly. A loop that goes silent fades over about a second, and one
     *  that starts fades in quicker; both reach their level. */
    @Test
    void loopsFadeRatherThanCut() {
        float v = 0.5f;
        int ticks = 0;
        while (v > 0.05f) {
            v = LoopedSoundPlayer.ease(v, 0f);
            ticks++;
        }
        assertTrue(ticks >= 15 && ticks <= 30, ticks + " ticks to fade to a tenth");
        for (int k = 0; k < 200; k++) v = LoopedSoundPlayer.ease(v, 0f);
        assertEquals(0f, v);
        float u = 0f;
        int up = 0;
        while (u < 0.45f) {
            u = LoopedSoundPlayer.ease(u, 0.5f);
            up++;
        }
        assertTrue(up < ticks, up + " ticks to fade in");
        for (int k = 0; k < 200; k++) u = LoopedSoundPlayer.ease(u, 0.5f);
        assertEquals(0.5f, u);
    }
}
