package mods.eln.transparentnode.solarpanel;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class SolarPanelContainer extends BasicContainer implements INodeContainer {

  NodeBase node = null;
  static final int trackerSlotId = 0;

  public SolarPanelContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
    super(player, inventory, new Slot[]{
            new GenericItemUsingDamageSlot(inventory, trackerSlotId, 176 / 2 - 20, 35, 1,
                SolarTrackerDescriptor.class, SlotSkin.medium,
                new String[]{tr("Solar tracker slot")})

    });
    this.node = node;

  }

  @Override
  public NodeBase getNode() {
    return node;
  }

  @Override
  public int getRefreshRateDivider() {
    return 0;
  }
}
