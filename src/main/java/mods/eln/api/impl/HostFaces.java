package mods.eln.api.impl;

import mods.eln.misc.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * A foreign block's front for RelativeFace: its facing property (named facing or horizontal_facing, with Direction
 * values: FACING, HORIZONTAL_FACING or a mod's own), or north without one. Faces then resolve as on an element
 * (Faces.resolve).
 */
public final class HostFaces {
    private HostFaces() {
    }

    /** The block's facing, or null when it has no facing property. */
    public static net.minecraft.core.Direction facing(BlockState state) {
        for (Property<?> p : state.getProperties())
            if ((p.getName().equals("facing") || p.getName().equals("horizontal_facing"))
                && p.getValueClass() == net.minecraft.core.Direction.class)
                return (net.minecraft.core.Direction) state.getValue(p);
        return null;
    }

    /** EA's front for a facing; null (no facing) is north. */
    public static Direction front(net.minecraft.core.Direction facing) {
        return facing == null ? Direction.ZN : Direction.fromEnumFacing(facing);
    }

    /** The front of a block in state. */
    public static Direction front(BlockState state) {
        return front(facing(state));
    }
}
