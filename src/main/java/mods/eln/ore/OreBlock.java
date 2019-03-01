package mods.eln.ore;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.Eln;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class OreBlock extends Block {

    public OreBlock() {
        super(Material.ROCK); //Parameters: Block ID, Block material
    /*	setTextureFile("/TutorialGFX/Blocks.png"); //The texture file used
		setBlockName("DeverionXBlockOre"); //The incode block name
		setCreativeTab(eln.c.tabGems); //The tab it appears in*/
        setHardness(3.0F); //The block hardness
        setResistance(5.0F); //The explosion resistance
    }
	/*//caca1.5.1
	public int getBlockTextureFromSideAndMetadata(int i,int j){
		return Eln.oreItem.getDescriptor(j).getBlockIconId(i, j);
	}*/

    public int damageDropped(int i) { //Makes sure pick block works right
        return i;
    }

/*
    @Override
    public void getSubBlocks(Item i, CreativeTabs tab, List l) { //Puts all sub blocks into the creative inventory
        Eln.oreItem.getSubItems(i, tab, l);
    }
*/

    // TODO(1.10): Fix item rendering.
//    @Override
//    @SideOnly(Side.CLIENT)
//    public IIcon getIcon(int par1, int par2) {
//        OreDescriptor desc = Eln.oreItem.getDescriptor(par2);
//        if (desc == null) return null;
//        return desc.getBlockIconId(par1, par2);
//    }

    public ArrayList<ItemStack> getBlockDropped(World w, int x, int y, int z, int meta, int fortune) { //Specifies the block drop
        OreDescriptor desc = Eln.oreItem.getDescriptor(meta);
        if (desc == null) return new ArrayList<ItemStack>();
        return desc.getBlockDropped(fortune);
    }

    @Override
    public void breakBlock(World par1World, BlockPos pos , IBlockState state) {
        super.breakBlock(par1World, pos, state);
        if (par1World.isRemote) return;
    }
}
