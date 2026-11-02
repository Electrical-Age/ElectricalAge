package mods.eln.api.docs;

import mods.eln.api.BurnHandler;
import mods.eln.api.MachineSpec;
import mods.eln.api.Tier;

/** docs/api.md's burn: a press that overvoltage burns, and its handler, which breaks the block. */
public final class Press {
    // doc:begin burn
    public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V200).ratedWatts(3000)
        .burnsOnOvervoltage(Press::burn)   // overvoltage burns it: it never resets
        .build();

    /** Once, on the server thread: the press breaks and drops itself. Left standing, it would stay tripped. */
    static void burn(BurnHandler.Burn burn) {
        burn.level().destroyBlock(burn.pos(), true);
    }
    // doc:end burn
}
