package mods.eln.node.simple;

import mods.eln.misc.DescriptorBase;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

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
			node.onBlockPlacedBy(new Coordinate(x,y,z,w), getFrontForPlacement(e), e, stack);
		}
	}*/

    protected abstract SimpleNode newNode();


    SimpleNode getNode(World world, BlockPos pos) {
        SimpleNodeEntity entity = (SimpleNodeEntity) world.getTileEntity(pos);
        if (entity != null) {
            return entity.getNode();
        }
        return null;
    }

    public SimpleNodeEntity getEntity(World world, BlockPos pos) {
        SimpleNodeEntity entity = (SimpleNodeEntity) world.getTileEntity(pos);
        return entity;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer entityPlayer, boolean willHarvest) {
        if (!world.isRemote) {
            SimpleNode node = getNode(world, pos);
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
		SimpleNodeEntity tileEntity = (SimpleNodeEntity) world.getTileEntity(x, y, z);
		tileEntity.onBlockPlacedBy(front, entityLiving, metadata);
	}*/

    // server
    @Override
    public void onBlockAdded(World par1World, BlockPos pos, IBlockState state) {
        if (!par1World.isRemote) {
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
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        if (!Utils.isRemote(world)) {
            SimpleNodeEntity entity = (SimpleNodeEntity) world.getTileEntity(pos);
            entity.onNeighborBlockChange();
        }
    }

    // client server

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        SimpleNodeEntity entity = (SimpleNodeEntity) world.getTileEntity(pos);
        return entity.onBlockActivated(playerIn, Direction.fromFacing(facing), hitX, hitY, hitZ);
    }

}
