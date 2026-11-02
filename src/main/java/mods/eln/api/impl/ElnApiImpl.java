package mods.eln.api.impl;

import mods.eln.api.MachineRegistry;
import mods.eln.api.impl.lowlevel.CircuitImpl;
import mods.eln.api.lowlevel.Circuit;
import mods.eln.api.spi.ElnApiProvider;

/** EA's implementation behind the API's entry points, found by name (Implementations.api()). */
public final class ElnApiImpl implements ElnApiProvider {
    public static final ElnApiImpl INSTANCE = new ElnApiImpl();

    private ElnApiImpl() {
    }

    @Override
    public MachineRegistry machines() {
        return HostedMachines.INSTANCE;
    }

    @Override
    public Circuit circuit(String name) {
        return new CircuitImpl(name);
    }
}
