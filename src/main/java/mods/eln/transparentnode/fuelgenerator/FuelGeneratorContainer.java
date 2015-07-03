package mods.eln.transparentnode.fuelgenerator;

import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class FuelGeneratorContainer extends BasicContainer implements INodeContainer {
    private NodeBase node = null;

    public FuelGeneratorContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{});
        this.node = node;
    }

    @Override
    public NodeBase getNode() {
        return node;
    }

    @Override
    public int getRefreshRateDivider() {
        return 4;   // TODO: What is this?
    }
}
