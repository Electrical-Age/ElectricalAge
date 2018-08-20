package mods.eln.node.simple;

import mods.eln.misc.Coordinate;
import net.minecraft.block.Block;
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
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        SimpleNode node = null;
        if (world.isRemote == false) {
            node = block.newNode();
            node.setDescriptorKey(block.descriptorKey);
            node.onBlockPlacedBy(new Coordinate(x, y, z, world), block.getFrontForPlacement(player), player, stack);
        }

        if (!world.setBlock(x, y, z, field_150939_a, metadata, 3)) {
            if (node != null) node.onBreakBlock();
            return false;
        }


        if (world.getBlock(x, y, z) == field_150939_a) {
            field_150939_a.onBlockPlacedBy(world, x, y, z, player, stack);
            field_150939_a.onPostBlockPlaced(world, x, y, z, metadata);
        }

        return true;
    }
}
