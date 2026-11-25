package mods.eln.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Tank Loading Station (Baughn 2026-11-25, todo #611): a straight rail with a pipe flange on each side, across the
 * track (TankStationEntity: one block loads and unloads). It is a powered rail without the boost unless powered
 * (PoweredRailBlock): unpowered it brakes and holds a cart standing on it; powered it lets the cart go, as a powered rail
 * launches one, and powers along with powered rails beside it. A comparator reads the docked cart (TankStationEntity
 * signal: 0 none, 1 empty, 2..14 by its liquid, 15 at least 90 % liquid).
 */
public class TankStationBlock extends PoweredRailBlock implements EntityBlock {
    public TankStationBlock(Properties p) {
        super(p, true);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankStationEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TankStationEntity s ? s.signal() : 0;
    }
}
