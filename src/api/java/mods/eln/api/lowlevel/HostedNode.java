package mods.eln.api.lowlevel;

import mods.eln.api.RelativeFace;
import net.minecraft.core.BlockPos;

/**
 * The node a circuit is hosted at (Circuit.hostAt): EA's presence at the block entity's block, unsaved, for as long as
 * the block entity lasts. Its faces follow the block's facing (a property named facing or horizontal_facing; without
 * one the block counts as facing north), and turning the block reconnects them.
 */
public interface HostedNode {
    BlockPos blockPos();

    /** In the world now: joined (from the tick after hostAt) and not yet retired. */
    boolean hosted();

    /** Retired: the block entity went, and the circuit with it. */
    boolean retired();

    /** Is a cable or EA device linked at face? */
    boolean connected(RelativeFace face);
}
