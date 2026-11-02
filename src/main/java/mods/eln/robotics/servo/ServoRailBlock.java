package mods.eln.robotics.servo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * A Servo Rail: one block of steel guide mast. Laid in a straight line from a Linear Servo Drive (its FACING is the
 * line's direction: up for an elevator), the drive's carriage runs beside it. Only the mast's core is solid, so a rider
 * at the car's edge doesn't catch on its guide fins.
 */
public class ServoRailBlock extends RodBlock {
    /** The fins' cross (outline) and the core post (collision), for each axis. */
    static final Map<Direction.Axis, VoxelShape> OUTLINE = Shapes.rotateAllAxis(Shapes.or(Block.box(1, 7, 0, 15, 9, 16), Block.box(7, 1, 0, 9, 15, 16)));
    static final Map<Direction.Axis, VoxelShape> CORE = Shapes.rotateAllAxis(Block.cube(4, 4, 16));

    public ServoRailBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    /** Continues the rail it's placed against; else it points away from the face clicked. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()));
        Direction f = against.getBlock() instanceof ServoRailBlock ? against.getValue(FACING) : ctx.getClickedFace();
        return defaultBlockState().setValue(FACING, f);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(OUTLINE.get(state.getValue(FACING).getAxis()), CORE.get(state.getValue(FACING).getAxis()));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE.get(state.getValue(FACING).getAxis());
    }
}
