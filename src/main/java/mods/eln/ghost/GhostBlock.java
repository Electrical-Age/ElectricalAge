package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

public class GhostBlock extends Block {

    public static final int tCube = 0;
    public static final int tFloor = 1;
    public static final int tLadder = 2;

    public GhostBlock() {
        super(Material.IRON);
    }

    @Nullable
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    // TODO(1.10): Needs to be done by block states.
//    @Override
//    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn) {
//        int meta = world.getBlockMetadata(x, y, z);
//
//        switch (meta) {
//            case tFloor:
//                AxisAlignedBB axisalignedbb1 = AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
//                if (axisalignedbb1 != null && par5AxisAlignedBB.intersectsWith(axisalignedbb1)) {
//                    list.add(axisalignedbb1);
//                }
//                break;
//            case tLadder:
//
//                break;
//            default:
//                GhostElement element = getElement(world, x, y, z);
//                Coordinate coord = element == null ? null : element.observatorCoordinate;
//                TileEntity te = coord == null ? null : coord.getTileEntity();
//                if (te != null && te instanceof TransparentNodeEntity) {
//                    ((TransparentNodeEntity) te).addCollisionBoxesToList(par5AxisAlignedBB, list, element.elementCoordinate);
//                } else {
//                    super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
//                }
//                break;
//        }
//    }

//    @Override
//    @SideOnly(Side.CLIENT)
//    public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
//        int meta = w.getBlockMetadata(x, y, z);
//
//        switch (meta) {
//            case tFloor:
//                return AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
//            case tLadder:
//                return AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z, (double) x + 0, (double) y + 0.0, (double) z + 0);
//            default:
//                return super.getSelectedBoundingBoxFromPool(w, x, y, z);
//        }
//    }
//
//    @Override
//    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3d startVec, Vec3d endVec) {
//        int meta = world.getBlockMetadata(x, y, z);
//
//        switch (meta) {
//            case tFloor:
//                this.maxY = 0.0625;
//                break;
//            case tLadder:
//                this.maxX = 0.01;
//                this.maxY = 0.01;
//                this.maxZ = 0.01;
//                break;
//            default:
//                break;
//        }
//
//        MovingObjectPosition m = super.collisionRayTrace(world, x, y, z, startVec, endVec);
//
//        switch (meta) {
//            case tFloor:
//                this.maxY = 1;
//                break;
//            case tLadder:
//                this.maxX = 1;
//                this.maxY = 1;
//                this.maxZ = 1;
//                break;
//            default:
//                break;
//        }
//
//        return m;
//    }
//
//    @Override
//    public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
//        return world.getBlockMetadata(x, y, z) == tLadder;
//    }


    // TODO(1.10): ...but block states should do this.
    @Override
    public boolean isFullyOpaque(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return null;
    }

    @Override
    public boolean isBlockSolid(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            GhostElement element = getElement(world, pos);
            if (element != null) element.breakBlock();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            GhostElement element = getElement(world, getBedSpawnPosition(state, world, pos, player));
            if (element != null)
                return element.onBlockActivated(player, Direction.fromFacing(side), hitX, hitY, hitZ);
        }
        return true;
    }

    private GhostElement getElement(World world, BlockPos pos) {
        return Eln.ghostManager.getGhost(new Coordinate(pos, world));
    }

    @Override
    public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos) {
        return 0.5f;
    }

    public String getNodeUuid() {
        return "g";
    }
}
