package mods.eln.ghost;


import mods.eln.compat.WorldCompat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
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

    @Override
    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
        return null;
    }

    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB par5AxisAlignedBB, List list, Entity entity) {
        int meta = WorldCompat.getMeta(world, x, y, z);

        switch (meta) {
            case tFloor:
                AxisAlignedBB axisalignedbb1 = new AxisAlignedBB((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
                if (axisalignedbb1 != null && par5AxisAlignedBB.intersectsWith(axisalignedbb1)) {
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
                    super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
                }
                break;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
        int meta = WorldCompat.getMeta(w, x, y, z);

        switch (meta) {
            case tFloor:
                return new AxisAlignedBB((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
            case tLadder:
                return new AxisAlignedBB((double) x, (double) y, (double) z, (double) x + 0, (double) y + 0.0, (double) z + 0);
            default:
                return super.getSelectedBoundingBoxFromPool(w, x, y, z);
        }
    }

    @Override
    public RayTraceResult collisionRayTrace(World world, int x, int y, int z, Vec3d startVec, Vec3d endVec) {
        int meta = WorldCompat.getMeta(world, x, y, z);

        switch (meta) {
            case tFloor:
                this.maxY = 0.0625;
                break;
            case tLadder:
                this.maxX = 0.01;
                this.maxY = 0.01;
                this.maxZ = 0.01;
                break;
            default:
                break;
        }

        RayTraceResult m = super.collisionRayTrace(world, x, y, z, startVec, endVec);

        switch (meta) {
            case tFloor:
                this.maxY = 1;
                break;
            case tLadder:
                this.maxX = 1;
                this.maxY = 1;
                this.maxZ = 1;
                break;
            default:
                break;
        }

        return m;
    }

    @Override
    public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
        return WorldCompat.getMeta(world, x, y, z) == tLadder;
    }

	/*
	 * @Override
	 * 
	 * @SideOnly(Side.CLIENT) public int idPicked(World par1World, int par2, int par3, int par4) {
	 * 
	 * return Block.dirt.blockID; }
	 */

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public ItemStack getPickBlock(RayTraceResult target, World world, int x, int y, int z) {
        return null;
    }

    public boolean isBlockSolid(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return false;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block par5, int par6) {
        if (world.isRemote == false) {
            GhostElement element = getElement(world, x, y, z);
            if (element != null) element.breakBlock();
        }
        super.breakBlock(world, x, y, z, par5, par6);
    }

    public boolean onBlockActivated(World world, int x, int y, int z, net.minecraft.entity.player.EntityPlayer player, int side, float vx, float vy, float vz) {
        if (world.isRemote == false) {
            GhostElement element = getElement(world, x, y, z);
            if (element != null)
                return element.onBlockActivated(player, Direction.fromIntMinecraftSide(side), vx, vy, vz);
        }
        return true;
    }

    GhostElement getElement(World world, int x, int y, int z) {
        return Eln.ghostManager.getGhost(new Coordonate(x, y, z, world));
    }

    @Override
    public float getBlockHardness(World par1World, int par2, int par3, int par4) {
        return 0.5f;
    }

    public String getNodeUuid() {
        return "g";
    }
}
