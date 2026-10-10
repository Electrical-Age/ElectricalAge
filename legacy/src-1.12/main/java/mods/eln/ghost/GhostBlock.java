package mods.eln.ghost;


import mods.eln.compat.BlockMeta;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class GhostBlock extends Block {

    public static final int tCube = 0;
    public static final int tFloor = 1;
    public static final int tLadder = 2;

    public GhostBlock() {
        super(Material.IRON);
    }

    // "meta" block state (porting guide rule 2): tCube / tFloor / tLadder
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BlockMeta.META);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(BlockMeta.META, meta & 15);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BlockMeta.META);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random p_149650_2_, int p_149650_3_) {
        return Items.AIR;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB par5AxisAlignedBB, List<AxisAlignedBB> list, Entity entity, boolean isActualState) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int meta = getMetaFromState(state);

        switch (meta) {
            case tFloor:
                AxisAlignedBB axisalignedbb1 = new AxisAlignedBB((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
                if (axisalignedbb1 != null && par5AxisAlignedBB.intersects(axisalignedbb1)) {
                    list.add(axisalignedbb1);
                }
                break;
            case tLadder:

                break;
            default:
                GhostElement element = getElement(world, x, y, z);
                Coordonate coord = element == null ? null : element.observatorCoordonate;
                TileEntity te = coord == null ? null : coord.getTileEntity();
                if (te != null && te instanceof TransparentNodeEntity) {
                    ((TransparentNodeEntity) te).addCollisionBoxesToList(par5AxisAlignedBB, list, element.elementCoordonate);
                } else {
                    super.addCollisionBoxToList(state, world, pos, par5AxisAlignedBB, list, entity, isActualState);
                }
                break;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World w, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int meta = getMetaFromState(state);

        switch (meta) {
            case tFloor:
                return new AxisAlignedBB((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
            case tLadder:
                return new AxisAlignedBB((double) x, (double) y, (double) z, (double) x + 0, (double) y + 0.0, (double) z + 0);
            default:
                return super.getSelectedBoundingBox(state, w, pos);
        }
    }

    private static final AxisAlignedBB FLOOR_AABB = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);
    private static final AxisAlignedBB LADDER_AABB = new AxisAlignedBB(0, 0, 0, 0.01, 0.01, 0.01);

    /** 1.7.10 temporarily shrank the block bounds (maxX/Y/Z) around super.collisionRayTrace. */
    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d startVec, Vec3d endVec) {
        switch (getMetaFromState(state)) {
            case tFloor:
                return rayTrace(pos, startVec, endVec, FLOOR_AABB);
            case tLadder:
                return rayTrace(pos, startVec, endVec, LADDER_AABB);
            default:
                return super.collisionRayTrace(state, world, pos, startVec, endVec);
        }
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return getMetaFromState(state) == tLadder;
    }

	/*
	 * @Override
	 * 
	 * @SideOnly(Side.CLIENT) public int idPicked(World par1World, int par2, int par3, int par4) {
	 * 
	 * return Block.dirt.blockID; }
	 */

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }

    /** 1.7.10 isBlockSolid(..) = false: nothing attaches to ghost blocks. */
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote == false) {
            GhostElement element = getElement(world, pos.getX(), pos.getY(), pos.getZ());
            if (element != null) element.breakBlock();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float vx, float vy, float vz) {
        // 1.7.10 had one hand: only react once (main hand)
        if (hand != EnumHand.MAIN_HAND) return true;
        if (world.isRemote == false) {
            GhostElement element = getElement(world, pos.getX(), pos.getY(), pos.getZ());
            if (element != null)
                return element.onBlockActivated(player, Direction.fromIntMinecraftSide(side.getIndex()), vx, vy, vz);
        }
        return true;
    }

    GhostElement getElement(World world, int x, int y, int z) {
        return Eln.ghostManager.getGhost(new Coordonate(x, y, z, world));
    }

    @Override
    public float getBlockHardness(IBlockState state, World par1World, BlockPos pos) {
        return 0.5f;
    }

    public String getNodeUuid() {
        return "g";
    }
}
