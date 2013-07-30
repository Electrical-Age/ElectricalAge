package mods.eln.electricalsource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;

import org.bouncycastle.crypto.modes.SICBlockCipher;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
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
import mods.eln.sim.ElectricalSourceRefGroundProcess;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class ElectricalSourceElement extends SixNodeElement{

	public ElectricalSourceElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(electricalLoad);

		electricalProcessList.add(groundProcess);
	}

	NodeElectricalLoad electricalLoad = new NodeElectricalLoad("electricalLoad");

	ElectricalSourceRefGroundProcess groundProcess = new ElectricalSourceRefGroundProcess(electricalLoad, 0);
	
	public static final int setVoltageId = 1;
	
	int color = 0;
	int colorCare = 0;


	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		byte b = nbt.getByte(str + "color");
		color = b & 0xF;
		colorCare = (b >> 4) & 1;
		
		groundProcess.Uc = nbt.getDouble(str + "voltage");
	}
	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "color",(byte) (color + (colorCare << 4)));
		
		nbt.setDouble(str + "voltage",groundProcess.Uc);
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
		return NodeBase.maskElectricalAll + (color << NodeBase.maskColorShift) +(colorCare << NodeBase.maskColorCareShift);
	}


	
	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotVolt("U",electricalLoad.Uc) + Utils.plotAmpere("I",electricalLoad.getCurrent());
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
			stream.writeByte( (color<<4));
	    	stream.writeFloat((float) groundProcess.Uc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		electricalLoad.setC(10000);
		electricalLoad.setRs(0.00001);

    	
          	   	

	}



	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		if(Eln.playerManager.get(entityPlayer).getInteractEnable())
		{
			colorCare = colorCare ^ 1;
			entityPlayer.addChatMessage("Wire color care " + colorCare);
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
						entityPlayer.addChatMessage("Brush is empty");
					}
				}
			}
		}
		return false;
	}

	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte())
			{
			case setVoltageId:
				groundProcess.Uc = stream.readFloat();
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
