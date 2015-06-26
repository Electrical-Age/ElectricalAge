package mods.eln.node.transparent;

import mods.eln.Eln;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlock;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class TransparentNodeBlock extends NodeBlock {

    public TransparentNodeBlock(Material material,
                                Class tileEntityClass) {
        super(material, tileEntityClass, 0);

    }

	/*@Override
    public TileEntity createNewTileEntity(World world, int meta) {

		if((meta & 0x4) != 0)
			return new TransparentNodeEntityWithSiededInv();
		return super.createNewTileEntity(world, meta);
	}
*/

    //@SideOnly(Side.CLIENT)
    public void getSubBlocks(Item par1, CreativeTabs tab, List subItems) {
        Eln.transparentNodeItem.getSubItems(par1, tab, subItems);
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


    @Override
    public boolean removedByPlayer(World world, EntityPlayer entityPlayer, int x, int y, int z) {
        if (!world.isRemote) {
            NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(x, y, z);
            if (entity != null) {
                NodeBase nodeBase = entity.getNode();
                if (nodeBase instanceof TransparentNode) {
                    TransparentNode t = (TransparentNode) nodeBase;
                    t.removedByPlayer = (EntityPlayerMP) entityPlayer;
                }
            }
        }

        return super.removedByPlayer(world, entityPlayer, x, y, z);

    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        if (world == null)
            return 0;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TransparentNodeEntity)
            return ((TransparentNodeEntity) world.getTileEntity(x, y, z)).getDamageValue(world, x, y, z);
        return 0;
    }


    @Override
    public int getLightOpacity(IBlockAccess world, int x, int y, int z) {
        return (world.getBlockMetadata(x, y, z) & 3) << 6;
    }


    @Override
    public Item getItemDropped(int p_149650_1_, Random p_149650_2_,
                               int p_149650_3_) {
        return null;
    }

    public int quantityDropped(Random par1Random) {
        return 0;
    }


    @Override
    public boolean canPlaceBlockOnSide(World par1World, int par2, int par3, int par4, int par5) {
        return true;
    }


    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB par5AxisAlignedBB, List list, Entity entity) {
        //   this.setBlockBoundsBasedOnState(world,x, y, z);
        //  super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity == null || (tileEntity instanceof TransparentNodeEntity == false)) {
            super.addCollisionBoxesToList(world, x, y, z, par5AxisAlignedBB, list, entity);
        } else {
            ((TransparentNodeEntity) tileEntity).addCollisionBoxesToList(par5AxisAlignedBB, list);
        }
        //Utils.println(list);
    }


    public String getNodeUuid() {

        return "t";
    }


}
