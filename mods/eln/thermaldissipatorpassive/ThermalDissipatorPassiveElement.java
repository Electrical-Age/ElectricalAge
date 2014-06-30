package mods.eln.thermaldissipatorpassive;



import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ThermalDissipatorPassiveElement extends TransparentNodeElement{
	ThermalDissipatorPassiveDescriptor descriptor;
	NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	

	
	public ThermalDissipatorPassiveElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		
		thermalLoadList.add(thermalLoad);
		this.descriptor = (ThermalDissipatorPassiveDescriptor) descriptor;

	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(side == Direction.YN || side == Direction.YP || lrdu != lrdu.Down) return null;
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(side == Direction.YN || side == Direction.YP || lrdu != lrdu.Down) return 0;
		return node.maskThermal;
	}

	@Override
	public String multiMeterString(Direction side) {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T : ", thermalLoad.Tc) + Utils.plotPower("P : ",thermalLoad.getPower());
	}

	@Override
	public void initialize() {
		descriptor.applyTo(thermalLoad);
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		ItemStack stack = entityPlayer.getCurrentEquippedItem();
		if(stack == null) return false;
		if(stack.getItem() == Items.water_bucket)
		{
			thermalLoad.Tc *= 0.5;
			
			entityPlayer.inventory.setInventorySlotContents(entityPlayer.inventory.currentItem, new ItemStack(Items.bucket));
			return true;
		}
		if(stack.getItem() == Item.getItemFromBlock(Blocks.ice))
		{
			thermalLoad.Tc *= 0.2;
			if(stack.stackSize != 0)
				stack.stackSize--;
			else
				entityPlayer.inventory.setInventorySlotContents(entityPlayer.inventory.currentItem, null);
			return true;
		}
		return false;
	}

}
