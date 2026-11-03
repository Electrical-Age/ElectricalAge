package mods.eln.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A device's outlet (Baughn 2026-11-03: the oil devices' drawn outlets did nothing): it pushes its tank into whatever
 * takes fluid through the face it is drawn on, as the block there offers it (Capabilities.Fluid.BLOCK): another mod's
 * pipe, a column base, a flare stack, an EA tank for water. EA pipes carry only water, steam and air (the fluid core),
 * so crude goes into other mods' pipes or straight into the next device.
 */
public final class FluidExport {
    private FluidExport() {
    }

    /** Push up to `max` mB of FROM's fluid out of the block at POS through FACE; what moved (mB). */
    public static int push(ElementFluidHandler from, ServerLevel level, BlockPos pos, Direction face, int max) {
        int have = from.getFluidAmount();
        if (have <= 0 || max <= 0) return 0;
        BlockPos there = pos.relative(face);
        if (!level.isLoaded(there)) return 0;
        var to = level.getCapability(Capabilities.Fluid.BLOCK, there, face.getOpposite());
        if (to == null || to == from) return 0;
        int moved;
        try (var tx = Transaction.openRoot()) {
            moved = to.insert(FluidResource.of(from.getFluid().getFluid()), Math.min(max, have), tx);
            tx.commit();
        }
        if (moved > 0) from.drain(moved, true);
        return moved;
    }
}
