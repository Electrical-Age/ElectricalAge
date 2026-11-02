package mods.eln.api.docs;

import mods.eln.api.CablePosition;
import mods.eln.api.Machine;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.Tier;

/** docs/api.md's signals and bus: a sorter that runs while enabled and says when it is busy. */
public final class Gate {
    // doc:begin spec
    public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V50).ratedWatts(200)
        .signalIn("enable", RelativeFace.LEFT, 1)    // reads 1 while no cable is connected there
        .signalOut("busy", RelativeFace.RIGHT)       // floats (high impedance) until first set
        .signalIn("speed", RelativeFace.FRONT, CablePosition.FLOOR, 1)
        .bus(RelativeFace.UP)
        .power(RelativeFace.BACK)
        .build();
    // doc:end spec

    /** The bus channel it marks while busy (0: white). */
    public static final int BUSY_CHANNEL = 0;

    // doc:begin tick
    public static void tick(Machine m) {
        double got = 0;
        if (m.signal("enable") >= 0.5)
            got = m.takeJoules(m.ratedJoulesPerTick() * m.signal("speed"));
        if (got > 0) m.setSignal("busy", 1);
        else m.releaseSignal("busy");                // let the wire float again
        m.bus().write(BUSY_CHANNEL, got > 0 ? 1 : 0); // a bus write lasts one tick: write every tick
    }
    // doc:end tick
}
