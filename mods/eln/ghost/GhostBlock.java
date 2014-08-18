package mods.eln.ghost;

import java.util.List;
import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class GhostBlock extends Block {

	public static final int tCube = 0;
	public static final int tFloor = 1;
	public static final int tLadder = 2;

	public GhostBlock() {
		super(Material.iron);
	}

	@Override
	public Item getItemDropped(int p_149650_1_, Random p_149650_2_,
			int p_149650_3_) {

		return null;
	}

	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB par5AxisAlignedBB, List list, Entity entity)
	{
		int meta = world.getBlockMetadata(x, y, z);
		switch (meta) {
		case tFloor:
			AxisAlignedBB axisalignedbb1 = AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
			if (axisalignedbb1 != null && par5AxisAlignedBB.intersectsWith(axisalignedbb1)){
				list.add(axisalignedbb1);
			}
			break;

		case tLadder:
			
			break;
		default:
			super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
			break;
		}

	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
		int meta = w.getBlockMetadata(x, y, z);
		switch (meta) {
		case tFloor:
			return AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z, (double) x + 1, (double) y + 0.0625, (double) z + 1);
			
		case tLadder:
			return AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z, (double) x + 0, (double) y + 0.0, (double) z + 0);
		default:
			return super.getSelectedBoundingBoxFromPool(w, x, y, z);
		}
	}

	
	@Override
	public MovingObjectPosition collisionRayTrace(World p_149731_1_, int p_149731_2_, int p_149731_3_, int p_149731_4_, Vec3 p_149731_5_, Vec3 p_149731_6_) {
		int meta = p_149731_1_.getBlockMetadata(p_149731_2_, p_149731_3_, p_149731_4_);
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

		}
		MovingObjectPosition m =  super.collisionRayTrace(p_149731_1_, p_149731_2_, p_149731_3_, p_149731_4_, p_149731_5_, p_149731_6_);
		
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

		}		
		return m;
	}
	
	
	@Override
	public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
		return world.getBlockMetadata(x, y, z) == tLadder;
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
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return -1;
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
		return null;
	}

	public boolean isBlockSolid(IBlockAccess p_149747_1_, int p_149747_2_, int p_149747_3_, int p_149747_4_, int p_149747_5_) {
		return false;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z,
			Block par5, int par6) {
		if (world.isRemote == false)
		{
			GhostElement element = getElement(world, x, y, z);
			if (element != null) element.breakBlock();

		}
		super.breakBlock(world, x, y, z, par5, par6);
	}

	public boolean onBlockActivated(World world, int x, int y, int z, net.minecraft.entity.player.EntityPlayer player, int side, float vx, float vy, float vz)
	{
		if (world.isRemote == false)
		{
			GhostElement element = getElement(world, x, y, z);
			if (element != null) return element.onBlockActivated(player, Direction.fromIntMinecraftSide(side), vx, vy, vz);
		}
		return true;
	}

	GhostElement getElement(World world, int x, int y, int z)
	{
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
