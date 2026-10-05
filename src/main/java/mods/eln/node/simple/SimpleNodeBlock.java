package mods.eln.node.simple;


import mods.eln.compat.WorldCompat;
import mods.eln.misc.DescriptorBase;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class SimpleNodeBlock extends BlockContainer {

    protected SimpleNodeBlock(Material material) {
        super(material);
    }

    String descriptorKey;

    public SimpleNodeBlock setDescriptorKey(String descriptorKey) {
        this.descriptorKey = descriptorKey;
        return this;
    }

    public SimpleNodeBlock setDescriptor(DescriptorBase descriptor) {
        this.descriptorKey = descriptor.descriptorKey;
        return this;
    }


    Direction getFrontForPlacement(EntityLivingBase e) {
        return Utils.entityLivingViewDirection(e).getInverse();
    }

	/*@Override
    public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase e, ItemStack stack) {
		if(w.isRemote == false){
			SimpleNode node = newNode();
			node.setDescriptorKey(descriptorKey);
			node.onBlockPlacedBy(new Coordonate(x,y,z,w), getFrontForPlacement(e), e, stack);
		}
	}*/

    protected abstract SimpleNode newNode();


    SimpleNode getNode(World world, int x, int y, int z) {
        SimpleNodeEntity entity = (SimpleNodeEntity) WorldCompat.getTileEntity(world, x, y, z);
        if (entity != null) {
            return entity.getNode();
        }
        return null;
    }

    public SimpleNodeEntity getEntity(World world, int x, int y, int z) {
        SimpleNodeEntity entity = (SimpleNodeEntity) WorldCompat.getTileEntity(world, x, y, z);
        return entity;
    }

    /** 1.7.10 BlockContainer rendered as a normal block; 1.12's defaults to INVISIBLE. TODO(1.12 WP6): models. */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer entityPlayer, boolean willHarvest) {
        if (!world.isRemote) {
            SimpleNode node = getNode(world, pos.getX(), pos.getY(), pos.getZ());
            if (node != null) {
                node.removedByPlayer = (EntityPlayerMP) entityPlayer;
            }
        }
        return super.removedByPlayer(state, world, pos, entityPlayer, willHarvest);
    }

    // client server
	/*onblockplaced
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, Direction front, EntityLivingBase entityLiving, int metadata)
	{
		SimpleNodeEntity tileEntity = (SimpleNodeEntity) WorldCompat.getTileEntity(world, x, y, z);
		tileEntity.onBlockPlacedBy(front, entityLiving, metadata);
	}*/

    // server
    @Override
    public void onBlockAdded(World par1World, BlockPos pos, IBlockState state) {
        if (par1World.isRemote == false) {
            SimpleNodeEntity entity = (SimpleNodeEntity) par1World.getTileEntity(pos);
            entity.onBlockAdded();
        }
    }

    // server
    @Override
    public void breakBlock(World par1World, BlockPos pos, IBlockState state) {
        SimpleNodeEntity entity = (SimpleNodeEntity) par1World.getTileEntity(pos);
        entity.onBreakBlock();
        super.breakBlock(par1World, pos, state);

    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block b, BlockPos fromPos) {
        if (Utils.isRemote(world) == false) {
            SimpleNodeEntity entity = (SimpleNodeEntity) world.getTileEntity(pos);
            entity.onNeighborBlockChange();
        }
    }

    // client server
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityPlayer, EnumHand hand, EnumFacing side, float vx, float vy, float vz) {
        // 1.7.10 had one hand; EA code reads the main hand, so only react once (main hand).
        if (hand != EnumHand.MAIN_HAND) return false;
        SimpleNodeEntity entity = (SimpleNodeEntity) world.getTileEntity(pos);
        return entity.onBlockActivated(entityPlayer, Direction.fromIntMinecraftSide(side.getIndex()), vx, vy, vz);
    }

}
