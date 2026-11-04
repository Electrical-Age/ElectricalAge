package mods.eln.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What a burn means for a machine at another mod's block entity (MachineSpec.Builder.burnsOnOvervoltage(handler)).
 * Baughn: "a callback. Let the API user choose how to handle it"; EA can't burn another mod's block in place.
 * <p>
 * Called once per burn, on the server thread, at the end of the simulator's tick (where changing the world is safe),
 * if the block entity is still in place. The machine is OVERVOLTAGE_TRIPPED for good whatever the handler does: it
 * draws nothing, its takes return 0, and it stays so through saves until its block entity goes. A handler that
 * throws is logged; the machine stays burnt.
 */
@FunctionalInterface
public interface BurnHandler {
    void onBurn(Burn burn);

    /** The burnt machine: its level, position and block entity, and the trip that burnt it (cause.burns() is true). */
    record Burn(ServerLevel level, BlockPos pos, BlockEntity blockEntity, TripCause cause) {
    }
}
