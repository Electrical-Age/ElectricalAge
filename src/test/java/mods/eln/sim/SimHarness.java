package mods.eln.sim;

import mods.eln.Eln;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** A Simulator configured like Eln's defaults (Eln.java: 20 Hz electrical, oversampling 50, 400 Hz fast thermal). */
public class SimHarness {
    public static final double TICK = 0.05;
    public static final double ELECTRICAL_PERIOD = 1 / 20.0;
    public static final double THERMAL_PERIOD = 1 / 400.0;

    public final Simulator sim = new Simulator(TICK, ELECTRICAL_PERIOD, 50, THERMAL_PERIOD);

    public SimHarness() {
        sim.init();
        Eln.simulator = sim;
    }

    public void tick() {
        sim.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
    }

    public void ticks(int n) {
        for (int i = 0; i < n; i++) tick();
    }

    public void seconds(double s) {
        ticks((int) Math.round(s / TICK));
    }
}
