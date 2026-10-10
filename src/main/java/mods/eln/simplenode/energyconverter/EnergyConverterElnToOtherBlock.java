package mods.eln.simplenode.energyconverter;

import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The energy converter block (EA -> FE). FRONT = the node's front as a vanilla direction index, 6 = unknown (all faces
 * "side"); 1.12 derived it in getActualState from the entity, 26.x stores it in the state when the node publishes.
 * Models: front face elntoic2lvu_eln, back face elntoic2lvu_ic2, sides elntoic2lvu_side (P3).
 */
public class EnergyConverterElnToOtherBlock extends SimpleNodeBlock {

    public static final IntegerProperty FRONT = IntegerProperty.create("front", 0, 6);

    private final EnergyConverterElnToOtherDescriptor descriptor;

    public EnergyConverterElnToOtherBlock(Properties properties, EnergyConverterElnToOtherDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
        setDescriptor(descriptor);
        registerDefaultState(stateDefinition.any().setValue(FRONT, 6));
    }

    public EnergyConverterElnToOtherDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConverterElnToOtherEntity(pos, state);
    }

    @Override
    protected SimpleNode newNode() {
        return new EnergyConverterElnToOtherNode();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FRONT);
    }
}
