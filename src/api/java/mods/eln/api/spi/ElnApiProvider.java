package mods.eln.api.spi;

import mods.eln.api.MachineRegistry;
import mods.eln.api.lowlevel.Circuit;

/**
 * EA's side of the API's entry points (ElnApi.machines(), Circuit.create). EA implements it; mods neither call nor
 * implement it. Implementations.api() finds it by name at first use.
 */
public interface ElnApiProvider {
    /** ElnApi.machines(). */
    MachineRegistry machines();

    /** Circuit.create(name). */
    Circuit circuit(String name);
}
