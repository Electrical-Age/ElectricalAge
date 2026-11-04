package mods.eln.api.impl;

import mods.eln.api.BurnHandler;
import mods.eln.api.MachineSpec;

/**
 * Machines.BURNS_OUT: the burn of EA's own elements, which burn out through EA's failure path until repaired
 * (ElementSite.burn), so its onBurn is never called there. The only handler Machines.forElement accepts. At another
 * mod's block entity (only EA's code can put it there) it does nothing: the machine stays burnt, the block stays.
 */
public enum ElementBurn implements BurnHandler {
    INSTANCE;

    @Override
    public void onBurn(Burn burn) {
    }

    /** Does spec burn EA's way (BURNS_OUT)? */
    public static boolean of(MachineSpec spec) {
        return spec.burnHandler().orElse(null) == INSTANCE;
    }
}
