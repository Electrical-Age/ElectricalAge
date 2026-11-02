package mods.eln.api;

/**
 * A block face relative to the block's facing (its FACING or HORIZONTAL_FACING property), so a spec says "power at
 * the back" once and it holds however the block is placed. Blocks without a facing are read as facing north.
 */
public enum RelativeFace {
    FRONT, BACK, LEFT, RIGHT, UP, DOWN
}
