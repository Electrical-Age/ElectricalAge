package mods.eln.sixnode.groundcable;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class GroundCableElement extends SixNodeElement{

	
	
	
	public GroundCableElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
	
		electricalLoadList.add(electricalLoad);
		electricalComponentList.add(ground);
		ground.setU(0);
	}
 



	NodeElectricalLoad electricalLoad = new NodeElectricalLoad("electricalLoad");
	VoltageSource ground = new VoltageSource("ground",electricalLoad,null);
	//ElectricalSourceRefGroundProcess groundProcess = new ElectricalSourceRefGroundProcess(electricalLoad, 0);
	
	int color = 0;
	int colorCare = 0;

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		byte b = nbt.getByte( "color");
		color = b & 0xF;
		colorCare = (b >> 4) & 1;
	}
	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		nbt.setByte( "color",(byte) (color + (colorCare << 4)));
	}
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return electricalLoad;
	}
	
	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub
		//if(inventory.getStackInSlot(GroundCableContainer.cableSlotId) == null) return 0;
		return NodeBase.maskElectricalPower + (color << NodeBase.maskColorShift) +(colorCare << NodeBase.maskColorCareShift);
	}


	
	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotVolt("U:",electricalLoad.getU()) + Utils.plotAmpere("I:",electricalLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return"";
	}


	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeByte( (color<<4));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(GroundCableContainer.cableSlotId));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		Eln.applySmallRs(electricalLoad);
		


	}



	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		if(Utils.isPlayerUsingWrench(entityPlayer))
		{
			colorCare = colorCare ^ 1;
			Utils.addChatMessage(entityPlayer,"Wire color care " + colorCare);
			sixNode.reconnect();
		}
		else if(currentItemStack != null)
		{
			Item item = currentItemStack.getItem();

			GenericItemUsingDamageDescriptor gen = BrushDescriptor.getDescriptor(currentItemStack);
			if(gen != null && gen instanceof BrushDescriptor) 
			{
				BrushDescriptor brush = (BrushDescriptor) gen;
				int brushColor = brush.getColor(currentItemStack);
				if(brushColor != color)
				{
					if(brush.use(currentItemStack))
					{
						color = brushColor;
						sixNode.reconnect();
					}
					else
					{
						Utils.addChatMessage(entityPlayer,"Brush is empty!");
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void inventoryChanged() {
		super.inventoryChanged();
		reconnect();
	}
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new GroundCableContainer(player, inventory);
	}
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	
}
