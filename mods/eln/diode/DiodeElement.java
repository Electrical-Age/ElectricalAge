





package mods.eln.diode;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;

import org.bouncycastle.crypto.modes.SICBlockCipher;

import mods.eln.Eln;
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
import mods.eln.sim.DiodeHeatingThermalLoadProcess;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class DiodeElement extends SixNodeElement implements IThermalDestructorDescriptor , ITemperatureWatchdogDescriptor{

	public DiodeElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		front = LRDU.Left;
    	electricalLoadList.add(anodeLoad);
    	electricalLoadList.add(catodeLoad);
    	thermalLoadList.add(thermalLoad);
    //	thermalProcessList.add(positiveETProcess);
    //	thermalProcessList.add(negativeETProcess);
    	thermalProcessList.add(diodeHeatingProcess);
    	electricalProcessList.add(diodeProcess);
    	slowProcessList.add(thermalWatchdog);
    	this.descriptor = (DiodeDescriptor) descriptor;
	}


	public DiodeDescriptor descriptor;
	public NodeElectricalLoad anodeLoad = new NodeElectricalLoad("anodeLoad");
	public NodeElectricalLoad catodeLoad = new NodeElectricalLoad("catodeLoad");
	public NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
/*	public ElectricalLoadHeatThermalLoadProcess positiveETProcess = new ElectricalLoadHeatThermalLoadProcess(anodeLoad,thermalLoad);
	public ElectricalLoadHeatThermalLoadProcess negativeETProcess = new ElectricalLoadHeatThermalLoadProcess(catodeLoad,thermalLoad);
*/
	public DiodeProcess diodeProcess = new DiodeProcess(anodeLoad, catodeLoad);
	public DiodeHeatingThermalLoadProcess diodeHeatingProcess = new DiodeHeatingThermalLoadProcess(diodeProcess, thermalLoad);

	public NodeThermalWatchdogProcess thermalWatchdog = new NodeThermalWatchdogProcess(sixNode, this, this, thermalLoad);
	
	LRDU front;
	

	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
        byte value = nbt.getByte(str + "front");
        front = LRDU.fromInt((value>>0) & 0x3);
       
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "front",(byte) ((front.toInt()<<0)));
        
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(front == lrdu) return anodeLoad;
		if(front.inverse() == lrdu) return catodeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub4
		if(front == lrdu) return descriptor.cable.getNodeMask();
		if(front.inverse() == lrdu) return descriptor.cable.getNodeMask();

		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotVolt("U+", anodeLoad.Uc) + Utils.plotVolt("U-", catodeLoad.Uc) + Utils.plotAmpere("I", anodeLoad.getCurrent()) ;
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T",thermalLoad.Tc);
	}



	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeByte( (front.toInt()<<4));
	    	stream.writeShort((short) ((anodeLoad.Uc)*NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short) ((catodeLoad.Uc)*NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short) (anodeLoad.getCurrent()*NodeBase.networkSerializeIFactor));
	    	stream.writeShort((short) (thermalLoad.Tc*NodeBase.networkSerializeTFactor));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void initialize() {
		// TODO Auto-generated method stub
    

		descriptor.applyTo(catodeLoad);
		descriptor.applyTo(anodeLoad);
    	descriptor.applyTo(thermalLoad);    	
    	descriptor.applyTo(diodeProcess);
    	

	}


	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		if(currentItemStack != null)
		{
			Item item = currentItemStack.getItem();
			/*if(item== Eln.toolsSetItem)
			{
				colorCare = colorCare ^ 1;
				entityPlayer.addChatMessage("Wire color care " + colorCare);
				sixNode.reconnect();
			}
			if(item == Eln.brushItem)
			{
				if(currentItemStack.getItemDamage() < BrushItem.maximalUse)
				{
					color = currentItemStack.getItemDamage() & 0xF;
					
					currentItemStack.setItemDamage(currentItemStack.getItemDamage() + 16);
					
					sixNode.reconnect();
				}
				else
				{
					entityPlayer.addChatMessage("Brush is empty");
				}
			}*/

		}
		//front = LRDU.fromInt((front.toInt()+1)&3);
		if(Eln.playerManager.get(entityPlayer).getInteractEnable())
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		return false;
	}
	@Override
	public double getTmax() {
		// TODO Auto-generated method stub
		return descriptor.thermal.warmLimit;
	}
	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return descriptor.thermal.coolLimit;
	}
	@Override
	public double getThermalDestructionMax() {
		// TODO Auto-generated method stub
		return 1;
	}
	@Override
	public double getThermalDestructionStart() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public double getThermalDestructionPerOverflow() {
		// TODO Auto-generated method stub
		return 0.2;
	}
	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return 0.2;
	}


}
