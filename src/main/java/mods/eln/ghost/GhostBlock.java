package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.compat.BlockMeta;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The invisible blocks that make up a multiblock device around its node (1.12 GhostBlock). META: tCube (full block),
 * tFloor (thin floor plate), tLadder (climbable, no shape). Breaking or using one is forwarded to its GhostElement,
 * i.e. to the device. 1.12's per-device collision boxes for tCube ghosts come with the multiblock batch (P2).
 */
public class GhostBlock extends Block {
    public static final int tCube = 0;
    public static final int tFloor = 1;
    public static final int tLadder = 2;

    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1, 16);

    public GhostBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockMeta.META, tCube));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockMeta.META);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch (state.getValue(BlockMeta.META)) {
            case tFloor:
                return FLOOR;
            case tLadder:
                return Shapes.empty();
            default:
                return Shapes.block();
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean isLadder(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        return state.getValue(BlockMeta.META) == tLadder;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        GhostElement element = getElement(level, pos);
        if (element != null) element.breakBlock();
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return activate(level, pos, player, hit);
    }

    private InteractionResult activate(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            GhostElement element = getElement(level, pos);
            if (element != null) {
                float vx = (float) (hit.getLocation().x - pos.getX()), vy = (float) (hit.getLocation().y - pos.getY()),
                    vz = (float) (hit.getLocation().z - pos.getZ());
                element.onBlockActivated(player, Direction.fromIntMinecraftSide(hit.getDirection().get3DDataValue()), vx, vy, vz);
            }
        }
        return InteractionResult.SUCCESS;
    }

    GhostElement getElement(Level world, BlockPos pos) {
        return Eln.ghostManager.getGhost(new Coordonate(pos, world));
    }

    public String getNodeUuid() {
        return "g";
    }
}
