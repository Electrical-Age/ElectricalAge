	package mods.eln.ghost;
	
	import java.util.Random;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
	
public class GhostBlock extends Block {

	public GhostBlock() {
		super(Material.iron);
	}


	@Override
	public Item getItemDropped(int p_149650_1_, Random p_149650_2_,
			int p_149650_3_) {
		
		return null;
	}	
		
	/*@Override
	@SideOnly(Side.CLIENT)
	public int idPicked(World par1World, int par2, int par3, int par4) {
		
		return Block.dirt.blockID;
	}*/

	@Override
	public boolean isOpaqueCube() {
	  return false;
	}
	@Override
	public boolean renderAsNormalBlock() {
	  return false;
	}
	@Override
	public int getRenderType() {
	  return -1;
	}
	
	
	
	public boolean isBlockSolid(IBlockAccess p_149747_1_, int p_149747_2_, int p_149747_3_, int p_149747_4_, int p_149747_5_){
		return false;
	}

	
	@Override
	public void breakBlock(World world, int x, int y, int z,
			Block par5, int par6) {
		if(world.isRemote == false)
		{
			GhostElement element = getElement(world, x, y, z);
			if(element != null) element.breakBlock();
			
		}
		super.breakBlock(world, x, y, z, par5, par6);		
	}
	public boolean onBlockActivated(World world, int x, int y, int z, net.minecraft.entity.player.EntityPlayer player, int side, float vx, float vy, float vz)
	{
		if(world.isRemote == false)
		{
			GhostElement element = getElement(world, x, y, z);
			if(element != null)  return element.onBlockActivated(player, Direction.fromIntMinecraftSide(side), vx, vy, vz);
		}
		return true;
	}
	
	GhostElement getElement(World world, int x, int y, int z)
	{
		return Eln.ghostManager.getGhost(new Coordonate(x,y,z,world));
	}
	@Override
	public float getBlockHardness(World par1World, int par2, int par3, int par4) {
		
		return 0.5f;
	}




	public String getNodeUuid() {	
		return "g";
	}
}
