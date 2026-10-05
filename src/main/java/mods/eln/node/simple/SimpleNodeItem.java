package mods.eln.node.simple;


import mods.eln.misc.Coordonate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class SimpleNodeItem extends ItemBlock {
    SimpleNodeBlock block;

    public SimpleNodeItem(Block b) {
        super(b);
        block = (SimpleNodeBlock) b;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        SimpleNode node = null;
        if (world.isRemote == false) {
            node = block.newNode();
            node.setDescriptorKey(block.descriptorKey);
            node.onBlockPlacedBy(new Coordonate(x, y, z, world), block.getFrontForPlacement(player), player, stack);
        }

        if (!world.setBlockState(pos, newState, 3)) {
            if (node != null) node.onBreakBlock();
            return false;
        }


        if (world.getBlockState(pos).getBlock() == block) {
            block.onBlockPlacedBy(world, pos, newState, player, stack);
            // 1.7.10 also called block.onPostBlockPlaced (no-op for EA blocks; gone in 1.12)
        }

        return true;
    }
}
