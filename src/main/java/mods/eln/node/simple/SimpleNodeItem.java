package mods.eln.node.simple;

import mods.eln.misc.Coordinate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SimpleNodeItem extends ItemBlock {
    SimpleNodeBlock block;

    public SimpleNodeItem(Block b) {
        super(b);
        block = (SimpleNodeBlock) b;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        SimpleNode node = null;
        if (!world.isRemote) {
            node = block.newNode();
            node.setDescriptorKey(block.descriptorKey);
            node.onBlockPlacedBy(new Coordinate(pos, world), block.getFrontForPlacement(player), player, stack);
        }

        if (!world.setBlockState(pos, newState, 3)) {
            if (node != null) node.onBreakBlock();
            return false;
        }

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == this.block) {
            this.block.onBlockPlacedBy(world, pos, state, player, stack);
        }

        return true;
    }
}
