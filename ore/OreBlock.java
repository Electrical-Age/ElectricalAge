package mods.eln.ore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import mods.eln.CommonProxy;
import mods.eln.Eln;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class OreBlock extends Block {
	public OreBlock(int ID){
		super(ID,Material.rock); //Parameters: Block ID, Block material
	/*	setTextureFile("/TutorialGFX/Blocks.png"); //The texture file used
		setBlockName("DeverionXBlockOre"); //The incode block name
		setHardness(3.0F); //The block hardness
		setResistance(5.0F); //The explosion resistance
		setCreativeTab(eln.c.tabGems); //The tab it appears in*/
		setHardness(3.0F); //The block hardness
		setResistance(5.0F); //The explosion resistance
	}
	/*//caca1.5.1
	public int getBlockTextureFromSideAndMetadata(int i,int j){
		return Eln.oreItem.getDescriptor(j).getBlockIconId(i, j);
	}*/
	
	public int damageDropped(int i){ //Makes sure pick block works right
	return i;
	}
	
	public void getSubBlocks(int i, CreativeTabs tab, List l){ //Puts all sub blocks into the creative inventory
		Eln.oreItem.getSubItems(i, tab, l);
	}
	
    public void registerIcons(IconRegister par1IconRegister)
    {
    	
    }
    
    public Icon getBlockTextureFromSideAndMetadata(int side, int meta)
    {
    	 return Eln.oreItem.getDescriptor(meta).getBlockIconId(side, meta);
    }


	public ArrayList<ItemStack> getBlockDropped(World w, int x, int y, int z, int meta, int fortune){ //Specifies the block drop
	/*	ArrayList<ItemStack> list = new ArrayList<ItemStack>(); //The list of items
		
		list.add(new ItemStack(meta == 0 ? TutorialMain.ruby : TutorialMain.metaGem,1,meta == 0 ? 0 : meta-1)); //One guaranteed
		
		for(int i=0;i<2+fortune;i++){ //A loop for drops, increased by fortune enchant
			if(w.rand.nextInt(101) > 49){ //50% chance
			list.add(new ItemStack(meta == 0 ? TutorialMain.ruby : TutorialMain.metaGem,1,meta == 0 ? 0 : meta-1)); //Adds the gem
			}
		}
		
		return list; //Returns the finished list :)*/
		return Eln.oreItem.getDescriptor(meta).getBlockDropped(fortune);
	}
	/*//caca1.5.1
	@Override
	public String getTextureFile() {
		// TODO Auto-generated method stub
		return CommonProxy.BLOCK_PNG;
	}
	*/
	
	
	@Override
	public void breakBlock(World par1World, int par2, int par3, int par4,
			int par5, int par6) {
		// TODO Auto-generated method stub
		super.breakBlock(par1World, par2, par3, par4, par5, par6);
		if(par1World.isRemote) return;
		/*
		
		ArrayList<ItemStack> list = Eln.oreItem.getDescriptor(par6).getBlockDropped(0);
		if(list == null)
		{
			dropBlockAsItem_do(par1World, par2, par3, par4, new ItemStack(this, 1, par6));
		}
		else
			{		
			for(ItemStack stack : list)
			{
				dropBlockAsItem_do(par1World, par2, par3, par4, stack);
			}
		}
	*/
	}
}