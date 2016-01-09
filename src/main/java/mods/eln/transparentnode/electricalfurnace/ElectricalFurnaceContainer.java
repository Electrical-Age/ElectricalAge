package mods.eln.transparentnode.electricalfurnace;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class ElectricalFurnaceContainer extends BasicContainer implements INodeContainer {

	NodeBase node = null;

	public ElectricalFurnaceContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new SlotWithSkin(inventory, ElectricalFurnaceElement.outSlotId, 84, 58, SlotSkin.big),
				new SlotWithSkin(inventory, ElectricalFurnaceElement.inSlotId, 7, 58, SlotSkin.medium),
				
				new GenericItemUsingDamageSlot(inventory, 2, 70, 6 + 20 + 6, 1, HeatingCorpElement.class, SlotSkin.medium,
								new String[]{tr("Heating corp slot")}),
				new GenericItemUsingDamageSlot(inventory, 3, 80 + 18, -2000, 1, ThermalIsolatorElement.class, SlotSkin
								.medium, new String[]{tr("Thermal isolator slot")}),
				new RegulatorSlot(inventory, 4, 70 + 18, 6 + 20 + 6, 1, new RegulatorType[]{RegulatorType.OnOff,
								RegulatorType.Analog}, SlotSkin.medium)
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
