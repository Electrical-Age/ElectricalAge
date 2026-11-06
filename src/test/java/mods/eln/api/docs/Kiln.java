package mods.eln.api.docs;

import mods.eln.api.Machine;
import mods.eln.api.MachineSpec;
import mods.eln.api.Tier;

/** docs/api.md's heat: a kiln whose losses heat it, with an overheat trip. */
public final class Kiln {
    // doc:begin kiln
    public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V230).ratedWatts(4000)
        .heatCapacityJPerK(20000)      // J/K: with the loss, a time constant of 20000 / 50 = 400 s
        .heatLossWPerK(50)             // W/K to the ambient where it stands
        .overheatAbsoluteCelsius(300)  // trips OVERHEAT_TRIPPED at 300 C, whatever its ambient; resets 10 K under that
        .build();

    public static void tick(Machine m) {
        double got = m.takeJoules(m.ratedJoulesPerTick());
        m.heatJoules(0.3 * got);       // 30 % of its work ends up as its own heat
    }
    // doc:end kiln
}
