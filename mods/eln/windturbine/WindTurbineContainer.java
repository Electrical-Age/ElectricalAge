package mods.eln.windturbine;


import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.DynamoDescriptor;
import mods.eln.item.ElectricalMotorDescriptor;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.WindRotorDescriptor;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class WindTurbineContainer extends BasicContainer implements INodeContainer{

	NodeBase node = null;
	static final int windRotorSlotId = 0;
	static final int dynamoSlotId = 1;
	
	public WindTurbineContainer(NodeBase node,EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new GenericItemUsingDamageSlot(inventory, windRotorSlotId, 62 +  0,17 + 0,1, WindRotorDescriptor.class,SlotSkin.medium,new String[]{"Wind rotor slot"}),
				new GenericItemUsingDamageSlot(inventory, dynamoSlotId, 62 +  18,17 + 0,1, DynamoDescriptor.class,SlotSkin.medium,new String[]{"Dynamo slot"})

			});
		this.node = node;
		// TODO Auto-generated constructor stub
	}

	@Override
	public NodeBase getNode() {
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