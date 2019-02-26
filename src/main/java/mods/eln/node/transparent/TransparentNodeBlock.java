package mods.eln.node.transparent;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlock;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
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
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return state.getRenderType();
    }


    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer entityPlayer, boolean willHarvest) {
        if (!world.isRemote) {
            NodeBlockEntity entity = (NodeBlockEntity) world.getTileEntity(pos);
            if (entity != null) {
                NodeBase nodeBase = entity.getNode();
                if (nodeBase instanceof TransparentNode) {
                    TransparentNode t = (TransparentNode) nodeBase;
                    t.removedByPlayer = (EntityPlayerMP) entityPlayer;
                }
            }
        }

        return super.removedByPlayer(state, world, pos, entityPlayer, willHarvest);
    }

    // TOOD(1.10): Was this important?
//    @Override
//    public int getDamageValue(World world, BlockPos pos) {
//        if (world == null)
//            return 0;
//        TileEntity tile = world.getTileEntity(pos);
//        if (tile != null && tile instanceof TransparentNodeEntity)
//            return ((TransparentNodeEntity) world.getTileEntity(pos)).getDamageValue(world, pos);
//        return 0;
//    }

    @Override
    public int getLightValue(IBlockAccess world, BlockPos pos) {
        return (world.getBlockState(pos).getBlock().getMetaFromState(world.getBlockState(pos)) & 3) << 6;
    }

    @Nullable
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    public int quantityDropped(Random par1Random) {
        return 0;
    }

    public void addCollisionBoxesToList(World world, BlockPos pos, AxisAlignedBB par5AxisAlignedBB, List list, Entity entity) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null || (tileEntity instanceof TransparentNodeEntity == false)) {
            super.addCollisionBoxToList(world.getBlockState(pos), world, pos, par5AxisAlignedBB, list, entity);
        } else {
            ((TransparentNodeEntity) tileEntity).addCollisionBoxesToList(par5AxisAlignedBB, list, null);
        }
    }

    @Override
    public TileEntity createTileEntity(World var1, IBlockState state) {
        try {
            for (EntityMetaTag tag : EntityMetaTag.values()) {
                if (tag.meta == state.getBlock().getMetaFromState(state)) {
                    return (TileEntity) tag.cls.getConstructor().newInstance();
                }
            }
            // Sadly, this will happen a lot with pre-metatag worlds.
            // Only real fix is to replace the blocks, but there should be no
            // serious downside to getting the wrong subclass so long as they really
            // wanted the superclass.
            System.out.println("Unknown block meta-tag: " + state.getBlock().getMetaFromState(state));
            return (TileEntity) EntityMetaTag.Basic.cls.getConstructor().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        while (true) ;
    }

    public String getNodeUuid() {

        return "t";
    }


}
