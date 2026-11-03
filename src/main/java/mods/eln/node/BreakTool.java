package mods.eln.node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The tool that breaks EA's blocks fast (Baughn: a pickaxe for machinery and pipes, shears for cables). A tool mines
 * a machine or pipe as fast as stone and a cable as fast as wool; a node block holding both kinds asks for the one
 * under the crosshair, so a pickaxe takes a cable at bare-hand speed. Baughn: no faster than about one part per
 * quarter second. Vanilla pauses 5 ticks after each block that took more than one tick to break, and none after one
 * broken at once, so a part takes at least two ticks (MAX_PROGRESS): 7 ticks a part at best.
 */
public final class BreakTool {
    private BreakTool() {
    }

    /** The most progress a tick: no part breaks at once (an Efficiency V diamond pickaxe would, every tick). */
    static final float MAX_PROGRESS = 0.5F;

    public enum Kind {
        /** Machines, devices and pipes: a pickaxe. */
        PICKAXE,
        /** Cables: shears. */
        SHEARS;

        /** The vanilla block the tool's speed is taken from. */
        BlockState reference() {
            return this == SHEARS ? Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
    }

    /**
     * Vanilla's progress per tick (BlockBehaviour.getDestroyProgress) with the held tool's speed on kind's reference
     * block in place of its speed on state; everything else (Efficiency, Haste, water, airborne, break-speed events)
     * applies as on any block. At most MAX_PROGRESS.
     */
    public static float progress(BlockState state, Player player, BlockGetter level, BlockPos pos, Kind kind) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness == -1.0F) return 0.0F;
        var held = player.getMainHandItem();
        float efficiency = (float) player.getAttributeValue(Attributes.MINING_EFFICIENCY);
        float speed = player.getDestroySpeed(state, pos) * withEfficiency(held.getDestroySpeed(kind.reference()), efficiency)
            / withEfficiency(held.getDestroySpeed(state), efficiency);
        int modifier = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
        return Math.min(speed / hardness / modifier, MAX_PROGRESS);
    }

    /** A tool speed as Player.getDestroySpeed counts it: Efficiency adds only to a tool that is faster than a hand. */
    private static float withEfficiency(float speed, float efficiency) {
        return speed > 1.0F ? speed + efficiency : speed;
    }

    /** The tool for a six-node element: shears for electrical and thermal cables, else a pickaxe. */
    public static Kind of(mods.eln.node.six.SixNodeDescriptor d) {
        return d instanceof mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
            || d instanceof mods.eln.sixnode.thermalcable.ThermalCableDescriptor ? Kind.SHEARS : Kind.PICKAXE;
    }
}
