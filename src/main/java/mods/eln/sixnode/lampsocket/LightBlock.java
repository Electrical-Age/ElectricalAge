package mods.eln.sixnode.lampsocket;

import mods.eln.misc.Coordinate;
import mods.eln.sixnode.lampsocket.LightBlockEntity.LightBlockObserver;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class LightBlock extends BlockContainer {
  
    public LightBlock() {
        super(Material.AIR);
    }

//    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3d start, Vec3d end) {
//        return null;
//    }
//
//    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
//        return null;
//    }

    @Override
    public int quantityDropped(Random par1Random) {
        return 0;
    }

    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    // TODO(1.10): Block states for light levels? Or setLightLevel?
//    @Override
//    public int getLightValue(IBlockAccess world, int x, int y, int z) {
//        /*if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
//    		Utils.println("Light at " + x + ":" + y + ":" + z + " " + FMLCommonHandler.instance().getEffectiveSide().toString() + " get light " + world.getBlockMetadata(x, y, z));
//    	}*/
//        //	Utils.println("Light at " + x + ":" + y + ":" + z + " " + FMLCommonHandler.instance().getEffectiveSide().toString() + " get light " + world.getBlockMetadata(x, y, z));
//        return world.getBlockMetadata(x, y, z);
//
//        //return ((LightBlockEntity)world.getBlockTileEntity(x, y, z)).getClientLight();
//    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new LightBlockEntity();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        Coordinate coord = new Coordinate(pos, worldIn);
        for (LightBlockObserver o : LightBlockEntity.observers) {
            o.lightBlockDestructor(coord);
        }
        super.breakBlock(worldIn, pos, state);
    }
}
