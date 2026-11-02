package mods.eln.robotics.servo;

import mods.eln.i18n.I18N;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * A Station Marker: a call box fixed to the side of a Servo Rail (or of its drive), its back on the rail. It makes a
 * stop of the car at its own height (the car's floor level with the marker's block), and a click calls the car there.
 * FACING points away from the rail.
 */
public class StationMarkerBlock extends DirectionalBlock {
    static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.box(3, 3, 14, 13, 13, 16));

    public StationMarkerBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** The drive whose rail this marker is fixed to: down the rail from the block behind it, or that block itself. */
    public static LinearServoDriveElement driveOf(Level level, BlockPos pos, BlockState state) {
        BlockPos p = pos.relative(state.getValue(FACING).getOpposite());
        for (int i = 0; i <= LinearServoDriveDescriptor.MAX_TRAVEL + 1; i++) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof ServoRailBlock) p = p.relative(s.getValue(FACING).getOpposite());
            else return LinearServoDriveElement.at(level, p);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        LinearServoDriveElement d = driveOf(level, pos, state);
        if (d == null) player.sendOverlayMessage(Component.literal(I18N.tr("Not on the rail of a Linear Servo Drive")));
        else d.callTo(player, pos);
        return InteractionResult.SUCCESS;
    }
}
