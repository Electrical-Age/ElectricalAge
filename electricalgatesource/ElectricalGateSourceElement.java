package mods.eln.electricalgatesource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;

import org.bouncycastle.crypto.modes.SICBlockCipher;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.item.LampDescriptor;
import mods.eln.lampsocket.LampSocketContainer;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.IVoltageDestructorDescriptor;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeElectricalResistor;
import mods.eln.node.NodeElectricalSourceWithCurrentLimitationProcess;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.NodeVoltageWatchdogProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.IVoltageWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class ElectricalGateSourceElement extends SixNodeElement {

	public ElectricalGateSourceElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		front = LRDU.Left;
		
    	electricalLoadList.add(outputGate);
    	electricalProcessList.add(outputGateProcess);

    	this.descriptor = (ElectricalGateSourceDescriptor) descriptor;
	}


	public ElectricalGateSourceDescriptor descriptor;
	public NodeElectricalLoad outputGate = new NodeElectricalLoad("outputGate");
	
	public NodeElectricalGateOutputProcess outputGateProcess = new NodeElectricalGateOutputProcess("outputGateProcess",outputGate);


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
		if(front == lrdu) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub4
		if(front == lrdu) return Node.maskElectricalOutputGate;

		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotUIP(outputGate.Uc, outputGate.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return "";
	}



	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeByte( (front.toInt()<<4));
			stream.writeFloat((float) outputGateProcess.U);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	@Override
	public void initialize() {
		
		Eln.instance.signalCableDescriptor.applyTo(outputGate, false);
    	computeElectricalLoad();
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
	}

	public void computeElectricalLoad()
	{

	}
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if(Eln.playerManager.get(entityPlayer).getInteractEnable())
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		else if(Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{ 
    		return false;
    	}
    	if(Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{ 
    		return false;
    	}
    	if(Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{
    		return false;
    	}    
    	else
		{
			//setSwitchState(true);
			//return true;
		}
		//front = LRDU.fromInt((front.toInt()+1)&3);
    	return false;

	}

	public static final byte setVoltagerId = 1;


	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte())
			{
			case setVoltagerId:
				outputGateProcess.U = stream.readFloat();
				needPublish();
				break;

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	

	
	
	
}
