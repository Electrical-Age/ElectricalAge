package mods.eln.electricalfurnace;


import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
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
				new Slot(inventory,0,62 +  0,17),
				new Slot(inventory,1,62 + 18,17),
				
				new GenericItemUsingDamageSlot(inventory, 2, 0 +  0,0,1, HeatingCorpElement.class),
				new GenericItemUsingDamageSlot(inventory, 3, 62 +  18,17 + 18,1, ThermalIsolatorElement.class),
				new RegulatorSlot(inventory, 4, 62 +  36,17 + 18,1, new RegulatorType[]{RegulatorType.onOff,RegulatorType.analog})
				 
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
		return 0;
	}



}
/*				new SlotFilter(inventory,0,62 +  0,17,new ItemStackFilter[]{new ItemStackFilter(Block.wood,0,0)}),
new SlotFilter(inventory,1,62 + 18,17,new ItemStackFilter[]{new ItemStackFilter(Item.coal,0,0)})
*/