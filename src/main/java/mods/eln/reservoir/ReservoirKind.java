package mods.eln.reservoir;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * What a reservoir holds and how its cells look: oil (EA's crude lakes) or water (Watershed's lakes). Registered by id
 * (Reservoirs.registerKind), so this package needs nothing from the rest of EA.
 */
public interface ReservoirKind {
    /** A cell at level 1-8 (8 full). */
    BlockState cell(int level);

    /** A cell of this reservoir's fluid as it should be (any level), or something that belongs in it at that level
     *  (water plants, bubble columns in a full water cell). */
    boolean ours(BlockState s);

    /** Can the sweep write a cell over it (air, plants, snow layers)? Anything else in a cell is a dam. */
    default boolean fillable(BlockState s) {
        return s.isAir() || s.canBeReplaced() && !s.hasBlockEntity() && s.getFluidState().isEmpty();
    }

    /** Does a waterlogged-able block belong to the lake (water: yes; it holds water below the surface)? */
    default boolean waterlogs() {
        return false;
    }

    /** A foreign fluid block that joins the record when found in a cell (crude poured back: 1000 mB), else 0. */
    default long poured(BlockState s) {
        return 0;
    }

    /** Is the state right for a cell at level lvl (0: none)? */
    default boolean matches(BlockState s, int lvl) {
        if (lvl == 0) return s.isAir();
        return s == cell(lvl);
    }

    static boolean waterloggable(BlockState s) {
        return s.hasProperty(BlockStateProperties.WATERLOGGED);
    }
}
