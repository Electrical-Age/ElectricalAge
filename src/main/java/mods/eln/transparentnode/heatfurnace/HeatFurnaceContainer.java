package mods.eln.transparentnode.heatfurnace;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkinAndComment;
import mods.eln.item.CombustionChamber;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class HeatFurnaceContainer extends BasicContainer implements INodeContainer {

	public static final int combustibleId = 0;
	public static final int regulatorId = 1;
	public static final int isolatorId = 2;
	public static final int combustrionChamberId = 3;
	
	NodeBase node;
    
	public HeatFurnaceContainer(NodeBase node, EntityPlayer player, IInventory inventory, HeatFurnaceDescriptor descriptor) {
		super(player, inventory, new Slot[]{
				new SlotWithSkinAndComment(inventory, combustibleId,70 , 58, SlotSkin.medium,
					new String[]{tr("Fuel slot")}),
			//	new RegulatorSlot(inventory, regulatorId,62 + 0, 17 + 18, 1, new RegulatorType[]{),
				new GenericItemUsingDamageSlot(inventory, regulatorId, 8, 58, 1, IRegulatorDescriptor.class,
					SlotSkin.medium,
					new String[]{tr("Regulator slot")}),
				new GenericItemUsingDamageSlot(inventory, isolatorId, 8 + 18, -2000, 1,
					ThermalIsolatorElement.class, SlotSkin.medium,
					new String[]{tr("Thermal isolator slot")}),
				new GenericItemUsingDamageSlot(inventory, combustrionChamberId, 8 + 18, 58,
					descriptor.combustionChamberMax, CombustionChamber.class, SlotSkin.medium,
					new String[]{tr("Combustion chamber slot")}),
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
