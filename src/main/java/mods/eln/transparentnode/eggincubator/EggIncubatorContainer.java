package mods.eln.transparentnode.eggincubator;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.ItemStackFilter;
import mods.eln.gui.SlotFilter;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.Node;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class EggIncubatorContainer extends BasicContainer implements INodeContainer {

    public static final int EggSlotId = 0;
    private Node node;

    public EggIncubatorContainer(EntityPlayer player, IInventory inventory, Node node) {
        super(player, inventory, new Slot[]{
                new SlotFilter(inventory, EggSlotId, 176 / 2 - 8, 7, 64, new ItemStackFilter[]{new ItemStackFilter(Items.egg)}, SlotSkin.medium, new String[]{"Egg slot"})
                //	new SlotFilter(inventory, 1, 62 + 18, 17, 1, new ItemStackFilter[]{new ItemStackFilter(Eln.sixNodeBlock, 0xFF, Eln.electricalCableId)})
        });
        this.node = node;
    }

    @Override
    public NodeBase getNode() {
        return node;
    }

    @Override
    public int getRefreshRateDivider() {
        return 1;
    }
}
