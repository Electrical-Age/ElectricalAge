package mods.eln.api;

import mods.eln.api.spi.Implementations;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Electrical Age's API for other mods: machines on EA's circuits. Its classes come with EA; a mod compiles against
 * the eln-api jar (compileOnly) and, if EA is optional for it, checks ModList.get().isLoaded("eln") before it
 * touches any of them. Without EA the entry points (machines(), Circuit.create) throw IllegalStateException.
 */
public final class ElnApi {
    private ElnApi() {
    }

    /**
     * The API's version (semantic: major.minor.patch), also in the eln-api jar's manifest (Eln-Api-Version). Within
     * a major version changes are additive: a mod compiled against 1.x runs on any EA with API 1.y, y at least x.
     * A constant: javac copies it into your classes, so it is the version you compiled against; apiVersion() is the
     * installed EA's.
     */
    public static final String API_VERSION = "1.2.0";

    /** The installed EA's API version (API_VERSION as EA was built). */
    public static String apiVersion() {
        return API_VERSION;
    }

    /**
     * eln:machine, a MachineView at a block: every block entity type registered with machines(), and EA's own devices
     * that run on the Machine API. On the client, a hosted machine's synced view (see MachineRegistry.of). The side is
     * ignored (null is fine).
     */
    public static final BlockCapability<MachineView, Direction> MACHINE =
        BlockCapability.createSided(Identifier.fromNamespaceAndPath("eln", "machine"), MachineView.class);

    /**
     * Machines hosted at other mods' block entities: register a type once, then ask for a block entity's machine.
     * Throws IllegalStateException without EA.
     */
    public static MachineRegistry machines() {
        return Implementations.api().machines();
    }
}
