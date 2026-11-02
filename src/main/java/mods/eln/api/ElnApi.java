package mods.eln.api;

import mods.eln.api.impl.HostedMachines;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

/** Electrical Age's API for other mods: machines on EA's circuits. */
public final class ElnApi {
    private ElnApi() {
    }

    /**
     * eln:machine, a MachineView at a block: every block entity type registered with machines(), and EA's own devices
     * that run on the Machine API. On the client, a hosted machine's synced view (see MachineRegistry.of). The side is
     * ignored (null is fine).
     */
    public static final BlockCapability<MachineView, Direction> MACHINE =
        BlockCapability.createSided(Identifier.fromNamespaceAndPath("eln", "machine"), MachineView.class);

    /** Machines hosted at other mods' block entities: register a type once, then ask for a block entity's machine. */
    public static MachineRegistry machines() {
        return HostedMachines.INSTANCE;
    }
}
