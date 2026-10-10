package mods.eln.ore;


import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;

public class OreBlock extends Block {
    /**
     * 1.7.10 metadata = ore type (OreDescriptor.metadata: 1 copper, 4 lead, 5 tungsten, 6 cinnabar). Own property
     * instead of compat.BlockMeta.META: the client maps META blocks to a single "normal" variant, but each ore needs
     * its own texture (blockstates/ore.json, one variant per value).
     */
    public static final PropertyInteger TYPE = PropertyInteger.create("type", 0, 15);

    public OreBlock() {
        super(Material.ROCK); //Parameters: Block ID, Block material
    /*	setTextureFile("/TutorialGFX/Blocks.png"); //The texture file used
		setBlockName("DeverionXBlockOre"); //The incode block name
		setCreativeTab(eln.c.tabGems); //The tab it appears in*/
        setHardness(3.0F); //The block hardness
        setResistance(5.0F); //The explosion resistance
        setDefaultState(blockState.getBaseState().withProperty(TYPE, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, meta & 15);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE);
    }

	/*//caca1.5.1
	public int getBlockTextureFromSideAndMetadata(int i,int j){
		return mods.eln.registry.batch.Wp12Content.oreItem.getDescriptor(j).getBlockIconId(i, j);
	}*/

    @Override
    public int damageDropped(IBlockState state) { //Makes sure pick block works right
        return getMetaFromState(state);
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> l) { //Puts all sub blocks into the creative inventory
        mods.eln.registry.batch.Wp12Content.oreItem.getSubItems(tab, l);
    }

    // TODO(1.12 WP6 icon): getIcon(side, meta) removed; textures come from blockstates/ore.json.

    /** Not a Block override (not in 1.7.10 either: there the hook was getDrops); kept as it was. Drops come from damageDropped. */
    public ArrayList<ItemStack> getBlockDropped(World w, int x, int y, int z, int meta, int fortune) { //Specifies the block drop
        OreDescriptor desc = mods.eln.registry.batch.Wp12Content.oreItem.getDescriptor(meta);
        if (desc == null) return new ArrayList<ItemStack>();
        return desc.getBlockDropped(fortune);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        if (world.isRemote) return;
    }
}
