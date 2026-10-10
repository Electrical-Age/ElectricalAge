package mods.eln.node;

import mods.eln.misc.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * Opening a node's GUI. 1.12: player.openGui(Eln, GuiHandler.nodeBaseOpen + side, ...) with GuiHandler building the
 * Container/GuiScreen. 26.x: a MenuProvider per node side (P4). Until then nothing opens.
 */
public final class NodeMenus {
    private NodeMenus() {
    }

    public static boolean open(NodeBase node, Player player, Direction side) {
        return false;
    }
}
