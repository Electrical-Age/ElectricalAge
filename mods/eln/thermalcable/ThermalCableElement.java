package mods.eln.thermalcable;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;


import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class ThermalCableElement extends SixNodeElement  implements IThermalDestructorDescriptor , ITemperatureWatchdogDescriptor{

	
	ThermalCableDescriptor descriptor;
	
	public ThermalCableElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (ThermalCableDescriptor) descriptor;
		// TODO Auto-generated constructor stub		
		thermalLoadList.add(thermalLoad);
		slowProcessList.add(thermalWatchdogProcess);
	}


	NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");

	NodeThermalWatchdogProcess thermalWatchdogProcess = new NodeThermalWatchdogProcess(sixNode,this,this,thermalLoad);
	
	int color = 0;
	int colorCare = 1;
	



	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		byte b = nbt.getByte("color");
		color = b & 0xF;
		colorCare = (b >> 4) & 1;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		nbt.setByte("color",(byte) (color + (colorCare << 4)));
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub
		return NodeBase.maskThermalWire + (color << NodeBase.maskColorShift) +(colorCare << NodeBase.maskColorCareShift);
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T",thermalLoad.Tc) + Utils.plotPower("P", thermalLoad.getPower());
	}


	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeByte( (color<<4));
	    	stream.writeShort((short) (thermalLoad.Tc*NodeBase.networkSerializeTFactor));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void initialize() {


		descriptor.setThermalLoad(thermalLoad);

	}


	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		if(Eln.playerManager.get(entityPlayer).getInteractEnable())
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
	public double getTmax() {
		// TODO Auto-generated method stub
		return descriptor.thermalWarmLimit;
	}
	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return descriptor.thermalCoolLimit;
	}
	@Override
	public double getThermalDestructionMax() {
		// TODO Auto-generated method stub
		return 2;
	}
	@Override
	public double getThermalDestructionStart() {
		// TODO Auto-generated method stub
		return 1;
	}
	@Override
	public double getThermalDestructionPerOverflow() {
		// TODO Auto-generated method stub
		return 0.2;
	}
	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return 0.1;
	}

}
