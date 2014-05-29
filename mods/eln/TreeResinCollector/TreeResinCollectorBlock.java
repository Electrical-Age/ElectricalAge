package mods.eln.TreeResinCollector;


import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class TreeResinCollectorBlock extends BlockContainer{

	public TreeResinCollectorBlock(int id) {
		super(Material.wood);
		setBlockName("TreeResinCollector"); 
		// TODO Auto-generated constructor stub
	}
	@Override
	public boolean isOpaqueCube() {
	  return false;
	}
	
    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4)
    {
        return null;
    }
	
	@Override
	public TileEntity createNewTileEntity(World world,int a) {
		// TODO Auto-generated method stub
		return new TreeResinCollectorTileEntity();
	}
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side)
    {
    	//trololol fait chier
    	/*Side sideCS = FMLCommonHandler.instance().getEffectiveSide();
		if (sideCS == Side.CLIENT) return true;
    	Direction direction = Direction.fromIntMinecraftSide(side);
    	if(direction == Direction.YN || direction == Direction.YP) return false;
    	Coordonate coord = new Coordonate(x, y, z, world);
    	int blockId = direction.getInverse().getBlockId(coord);

    	Block block = Block.blocksList[blockID];
    	if(blockId == Block.wood.blockID) return true;
    	*/
    	return true;

    }
    
    @Override
    public int onBlockPlaced(World world, int x, int y, int z,
    		int side, float par6, float par7, float par8, int par9) {
    	// TODO Auto-generated method stub

    //	world.setBlockMetadataWithNotify(x, y, z, side, 0);
    //	((TreeResinCollectorTileEntity)world.getBlockTileEntity(x, y, z)).setWoodDirection(Direction.fromIntMinecraftSide(side));
    	//return super.onBlockPlaced(world, x, y, z, side, par6, par7, par8,
    	//		par9);
    	return side;

    }
    
    
    @Override
    public boolean onBlockActivated(World par1World, int x, int y,
    		int z, EntityPlayer par5EntityPlayer, int par6, float par7,
    		float par8, float par9) {
    	return ((TreeResinCollectorTileEntity)par1World.getTileEntity(x, y, z)).onBlockActivated();
    }
    
    @Override
    public void onNeighborBlockChange(World world, int x,
    		int y, int z, Block b) {
    	// TODO Auto-generated method stub
    	super.onNeighborBlockChange(world, x, y, z,b);
    	if(! canPlaceBlockOnSide(world, x, y, z, world.getBlockMetadata(x, y, z)))
    	{
    	//Utils.println("WOOOOOOD down");
    		
    		dropBlockAsItem(world, x, y, z, new ItemStack(this));
    		world.setBlockToAir(x, y, z);
    	}
    }

}
