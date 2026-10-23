package mods.eln.transparentnode.energyconverter;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The exporter's block (EA -> FE): a transparent-node block drawn by its baked model rather than the element render.
 * FRONT = the node's front as a vanilla direction index, 6 = unknown; set when placed (the front never changes). The
 * property and the registry names are those of the simple-node block it replaces, so saved chunks load unchanged.
 * Models: front face elntoic2lvu_eln, back face elntoic2lvu_ic2, sides elntoic2lvu_side.
 */
public class EnergyConverterElnToOtherBlock extends TransparentNodeBlock {

    public static final IntegerProperty FRONT = IntegerProperty.create("front", 0, 6);

    public EnergyConverterElnToOtherBlock(Properties properties, EnergyConverterElnToOtherDescriptor descriptor) {
        super(properties, descriptor);
        registerDefaultState(stateDefinition.any().setValue(FRONT, 6));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState stateForFront(Direction front) {
        return defaultBlockState().setValue(FRONT, front == null ? 6 : front.toEnumFacing().get3DDataValue());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRONT);
    }
}
