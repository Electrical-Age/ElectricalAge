	package mods.eln.ghost;
	
	import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
	
	
	public class GhostBlock extends Block{
	
		public GhostBlock(int id) {
			super(id,Material.iron);
		}
	
	/*@Override
	public TileEntity createNewTileEntity(World var1) {
		// TODO Auto-generated method stub
		return new GhostEntity();
	}*/
	@Override
	@SideOnly(Side.CLIENT)
	public int idPicked(World par1World, int par2, int par3, int par4) {
		// TODO Auto-generated method stub
		return Block.dirt.blockID;
	}

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
	
    public void registerIcons(IconRegister par1IconRegister)
    {
    	
    }
    
	@Override
	public boolean isBlockSolidOnSide(World world, int x, int y, int z,
			ForgeDirection side) {
		// TODO Auto-generated method stub
			return false;
	}
	
	 @Override
	public void breakBlock(World world, int x, int y, int z,
			int par5, int par6) {
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
		return false;
	}
	
	GhostElement getElement(World world, int x, int y, int z)
	{
		return Eln.ghostManager.getGhost(new Coordonate(x,y,z,world));
	}
}
