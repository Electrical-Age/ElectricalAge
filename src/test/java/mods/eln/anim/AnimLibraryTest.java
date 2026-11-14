package mods.eln.anim;

import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The animation library's frame-rate-proof parts: Smooth, Spin, Blink, Emitter, Progress. */
class AnimLibraryTest {
    @Test
    void smoothNeverOvershootsAtAnyFrameRate() {
        for (double dt : new double[]{0.001, 0.016, 0.05, 0.1, 1.0}) {
            Smooth s = new Smooth(0.05).target(1);
            double last = 0;
            for (int i = 0; i < (int) Math.ceil(5 / dt); i++) {
                double v = s.step(dt);
                assertTrue(v <= 1 + 1e-12 && v >= last - 1e-12, "dt " + dt + ": " + v + " after " + last);
                last = v;
            }
            assertEquals(1, s.get(), 1e-6, "settles at dt " + dt);
        }
    }

    @Test
    void smoothIsFrameRateIndependent() {
        Smooth a = new Smooth(0.5).target(1), b = new Smooth(0.5).target(1);
        for (int i = 0; i < 10; i++) a.step(0.1);
        for (int i = 0; i < 100; i++) b.step(0.01);
        assertEquals(a.get(), b.get(), 1e-9);
        assertEquals(1 - Math.exp(-2), a.get(), 1e-9);
    }

    @Test
    void spinWrapsBothWaysAndEases() {
        Spin s = new Spin(360, 0).speed(1);
        for (int i = 0; i < 25; i++) s.step(0.1);
        assertEquals(180, s.deg(), 1e-3);
        s.speed(-1);
        for (int i = 0; i < 27; i++) s.step(0.1);
        assertTrue(s.deg() >= 0 && s.deg() < 360, "" + s.deg());
        assertEquals(288, s.deg(), 1e-3);
        Spin eased = new Spin(360, 1).speed(1);
        eased.step(0.1);
        assertTrue(eased.speed.get() < 0.2, "spins up, not at once");
        Spin paused = new Spin(360, 0).speed(1);
        paused.step(0);
        assertEquals(0, paused.deg(), 0, "frame time 0 (paused) stops it");
    }

    @Test
    void blinkKeepsItsDutyWhateverTheFrames() {
        Blink b = Blink.every(1.0, 0.25);
        double on = 0;
        for (int i = 0; i < 10000; i++) if (b.step(0.007)) on += 0.007;
        assertEquals(0.25, on / 70, 0.02);
    }

    @Test
    void emitterCountsAtItsRate() {
        Emitter e = new Emitter();
        int n = 0;
        for (int i = 0; i < 1000; i++) n += e.step(0.013, 8);
        assertEquals(104, n, 1);
        assertEquals(0, e.step(0.1, 0));
    }

    @Test
    void ventFollowsTheFacing() {
        BlockPos p = new BlockPos(10, 64, -5);
        // the model's front is XN: a vent 0.4 in front of the centre comes out of the front face whichever way it faces
        var xn = Emitter.vent(p, Direction.XN, -0.4, 0.3, 0);
        assertEquals(10.1, xn.x, 1e-9);
        assertEquals(64.8, xn.y, 1e-9);
        assertEquals(-4.5, xn.z, 1e-9);
        var xp = Emitter.vent(p, Direction.XP, -0.4, 0, 0);
        assertEquals(10.9, xp.x, 1e-9);
        var zp = Emitter.vent(p, Direction.ZP, -0.4, 0, 0);
        assertEquals(-4.1, zp.z, 1e-9);
        var zn = Emitter.vent(p, Direction.ZN, -0.4, 0, 0);
        assertEquals(-4.9, zn.z, 1e-9);
    }

    @Test
    void progressRunsOnAndRestartsForTheNextPiece() {
        Progress p = new Progress();
        p.set(0, 0.5);
        for (int i = 0; i < 30; i++) p.step(0.05);
        assertEquals(0.75, p.state(), 1e-9);
        assertTrue(p.shown() <= p.state() + 1e-9 && p.shown() > 0.6);
        for (int i = 0; i < 30; i++) p.step(0.05);
        assertEquals(1, p.state(), 1e-9);
        p.set(0, 0.5);
        assertEquals(0, p.shown(), 1e-9, "a new piece starts from the top");
    }

    /** The same output looks the same wherever it comes out: the kind is the fluid's alone. */
    @Test
    void plumeByWhatComesOut() {
        assertEquals(Plume.STEAM, Plume.vented("steam"));
        assertEquals(Plume.SMOKE, Plume.vented("flue_gas"));
        assertEquals(Plume.NONE, Plume.vented("diesel"), "nothing to see of an unlit fuel");
        // (at a flame: what doesn't burn goes up as it would anywhere)
        assertEquals(Plume.STEAM, Plume.burnt("water"));
        assertEquals(Plume.SMOKE, Plume.burnt("flue_gas"));
        assertEquals(Plume.FLAME_GAS, Plume.burnt("naturalgas"));
        assertEquals(Plume.FLAME_LIGHT, Plume.burnt("gasoline"));
        assertEquals(Plume.FLAME_HEAVY, Plume.burnt("residue"));
        assertEquals(Plume.NONE, Plume.burnt(""));
        for (Plume p : Plume.values())
            assertEquals(p.name().startsWith("FLAME"), p.flame(), p.name());
    }
}
