package mods.eln.electricalfurnace;


import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.node.INodeContainer;
import mods.eln.node.Node;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalFurnaceContainer extends BasicContainer implements INodeContainer{

	Node node = null;
	
	public ElectricalFurnaceContainer(Node node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SlotWithSkin(inventory,0,176/2 - 35 - 18 +  0,28,SlotSkin.medium),
				new SlotWithSkin(inventory,1,176/2 + 15 ,28,SlotSkin.big),
				
				new GenericItemUsingDamageSlot(inventory, 2, 80,59,1, HeatingCorpElement.class,SlotSkin.medium,new String[]{"Heating corp slot"}),
				new GenericItemUsingDamageSlot(inventory, 3, 80 +  18,59,1, ThermalIsolatorElement.class,SlotSkin.medium,new String[]{"Thermal isolator slot"}),
				new RegulatorSlot(inventory, 4, 80 +  36,59,1, new RegulatorType[]{RegulatorType.onOff,RegulatorType.analog},SlotSkin.medium)
				 
				//new SlotFilter(inventory,2,62 +  0,17 + 18,1,new ItemStackFilter[]{new ItemStackFilter(Eln.heatingCorpItem)}),
				//new SlotFilter(inventory,3,62 + 18,17 + 18,1,new ItemStackFilter[]{new ItemStackFilter(Eln.thermalIsolatorItem)}),
				//new SlotFilter(inventory,4,62 + 36,17 + 18,1,new ItemStackFilter[]{new ItemStackFilter(Eln.regulatorItem)})

			});
		this.node = node;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Node getNode() {
		// TODO Auto-generated method stub
		return node;
	}

	@Override
	public int getRefreshRateDivider() {
		// TODO Auto-generated method stub
		return 1;
	}



}
/*				new SlotFilter(inventory,0,62 +  0,17,new ItemStackFilter[]{new ItemStackFilter(Block.wood,0,0)}),
new SlotFilter(inventory,1,62 + 18,17,new ItemStackFilter[]{new ItemStackFilter(Item.coal,0,0)})
*/