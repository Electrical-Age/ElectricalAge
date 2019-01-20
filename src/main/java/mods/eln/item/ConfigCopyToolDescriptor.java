package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlock;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.sound.SoundCommand;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ConfigCopyToolDescriptor extends GenericItemUsingDamageDescriptor {
    public ConfigCopyToolDescriptor(String name) { super(name); }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float vx, float vy, float vz) {
        if(world.isRemote) return false;

        Block block = world.getBlock(x, y, z);

        if(block instanceof NodeBlock) {
            NodeBase node = NodeManager.instance.getNodeFromCoordonate(new Coordonate(x, y, z, world));
            if(node != null) {
                node.onBlockActivated(player, Direction.fromIntMinecraftSide(side), vx, vy, vz);
            }
            return true;
        }
        return false;
    }
}
