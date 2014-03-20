package mods.eln.electricalentitysensor;



import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.SlotWithSkin;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.MaceratorSorterDescriptor;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalEntitySensorContainer extends BasicContainer{


	public static final int filterId = 0;
	public ElectricalEntitySensorContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{

				new GenericItemUsingDamageSlot(inventory,filterId,184/2-12,8,1,
												EntitySensorFilterDescriptor.class,
												SlotSkin.medium,
												new String[]{"Entity filter slot"}),
				
				
			});

		// TODO Auto-generated constructor stub
	}



}
/*				new SlotFilter(inventory,0,62 +  0,17,new ItemStackFilter[]{new ItemStackFilter(Block.wood,0,0)}),
new SlotFilter(inventory,1,62 + 18,17,new ItemStackFilter[]{new ItemStackFilter(Item.coal,0,0)})
*/