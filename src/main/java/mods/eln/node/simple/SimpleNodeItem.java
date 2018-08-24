package mods.eln.node.simple;

import mods.eln.misc.Coordinate;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SimpleNodeItem extends ItemBlock {
    SimpleNodeBlock block;

    public SimpleNodeItem(Block b) {
        super(b);
        block = (SimpleNodeBlock) b;
    }


    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, int side, float hitX, float hitY, float hitZ, int metadata) {
        SimpleNode node = null;
        if (world.isRemote) {
            node = block.newNode();
            node.setDescriptorKey(block.descriptorKey);
            node.onBlockPlacedBy(new Coordinate(pos, world), block.getFrontForPlacement(player), player, stack);
        }

        if (!world.setBlockState(pos, field_150939_a, metadata, 3)) {
            if (node != null) node.onBreakBlock();
            return false;
        }


        if (world.getBlockState(pos) == field_150939_a) {
            field_150939_a.onBlockPlacedBy(world, pos, player, stack);
            field_150939_a.onPostBlockPlaced(world, pos, metadata);
        }

        return true;
    }
}
