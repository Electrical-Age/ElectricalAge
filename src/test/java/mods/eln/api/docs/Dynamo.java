package mods.eln.api.docs;

import mods.eln.api.Machine;
import mods.eln.api.MachineSpec;
import mods.eln.api.Tier;

/** docs/api.md's producer: a dynamo that burns fuel. */
public final class Dynamo {
    // doc:begin dynamo
    public static final MachineSpec SPEC = MachineSpec.producer(Tier.V230).ratedWatts(1500).build();

    /** Energy left in the fuel, J. */
    private double fuelJoules;

    public void addFuel(double joules) {
        fuelJoules += joules;
    }

    /** One server tick: offer what the fuel holds; giveJoules accepts what fits in the machine's queue. */
    public void tick(Machine m) {
        fuelJoules -= m.giveJoules(Math.min(fuelJoules, m.bufferCapacityJoules()));
    }
    // doc:end dynamo

    public double fuelJoules() {
        return fuelJoules;
    }
}
