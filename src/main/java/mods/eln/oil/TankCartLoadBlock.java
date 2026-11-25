package mods.eln.oil;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * What a Tank Minecart carries, drawn as vanilla draws a chest minecart's chest: its display block (TankCart
 * getDefaultDisplayBlockState). A horizontal tank on saddles, iron or alloy (ALLOY); the checked model library's model
 * (src/cadmodels TankCartParts). Never placed: it has no item, no drops and is in no tab.
 */
public class TankCartLoadBlock extends Block {
    public static final BooleanProperty ALLOY = BooleanProperty.create("alloy");

    public TankCartLoadBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(ALLOY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ALLOY);
    }
}
