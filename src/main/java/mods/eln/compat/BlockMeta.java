package mods.eln.compat;

import net.minecraft.block.properties.PropertyInteger;

/**
 * The 0..15 "meta" block-state property for EA blocks that used metadata in 1.7.10 (porting guide rule 2).
 * Each such block overrides, keeping "meta" meaning exactly what it meant:
 * <pre>
 * protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, BlockMeta.META); }
 * public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(BlockMeta.META, meta &amp; 15); }
 * public int getMetaFromState(IBlockState state) { return state.getValue(BlockMeta.META); }
 * </pre>
 */
public final class BlockMeta {
    public static final PropertyInteger META = PropertyInteger.create("meta", 0, 15);

    private BlockMeta() {
    }
}
