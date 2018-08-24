package mods.eln.sixnode.TreeResinCollector;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TreeResinCollectorBlock extends BlockContainer {

    public TreeResinCollectorBlock(int id) {
        super(Material.WOOD);
        setRegistryName("TreeResinCollector");
    }

    @Override
    public boolean isVisuallyOpaque() {
        return false;
    }

    @NotNull
    @Override
    public TileEntity createNewTileEntity(World world, int a) {
        return new TreeResinCollectorTileEntity();
    }

//    @Override
//    public int onBlockPlaced(World world, int x, int y, int z, int side, float par6, float par7, float par8, int par9) {
//        //	world.setBlockMetadataWithNotify(x, y, z, side, 0);
//        //	((TreeResinCollectorTileEntity)world.getBlockTileEntity(x, y, z)).setWoodDirection(Direction.fromIntMinecraftSide(side));
//        //return super.onBlockPlaced(world, x, y, z, side, par6, par7, par8,
//        //		par9);
//        return side;
//    }


    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        return ((TreeResinCollectorTileEntity) worldIn.getTileEntity(pos)).onBlockActivated();
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(world, pos, neighbor);
        // TODO(1.10): Should implement this. (But it wasn't there in 1.7...)
//        if (!canPlaceBlockOnSide(world, x, y, z, world.getBlockMetadata(x, y, z))) {
//            //Utils.println("WOOOOOOD down");
//            dropBlockAsItem(world, x, y, z, new ItemStack(this));
//            world.setBlockToAir(x, y, z);
//        }
    }
}
