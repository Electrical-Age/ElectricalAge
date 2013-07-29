package mods.eln.electricalcable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.text.MaskFormatter;

import org.bouncycastle.crypto.modes.SICBlockCipher;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.IProcess;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;


public class ElectricalCableElement extends SixNodeElement implements IThermalDestructorDescriptor , ITemperatureWatchdogDescriptor {

	ElectricalCableDescriptor descriptor;
	
	public ElectricalCableElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (ElectricalCableDescriptor) descriptor;
		color = 0;
		colorCare = 1;
		
		thermalWatchdogProcess = new NodeThermalWatchdogProcess(sixNode,this,this,thermalLoad);
		
		electricalLoadList.add(electricalLoad);
		thermalLoadList.add(thermalLoad);
		thermalProcessList.add(ETProcess);
		slowProcessList.add(thermalWatchdogProcess);
		slowProcessList.add(electricalLoadDynamicProcess);
		

	}


	NodeElectricalLoad electricalLoad = new NodeElectricalLoad("electricalLoad");
	NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	ElectricalLoadHeatThermalLoadProcess ETProcess = new ElectricalLoadHeatThermalLoadProcess(electricalLoad,thermalLoad);

	NodeThermalWatchdogProcess thermalWatchdogProcess;
	ElectricalLoadDynamicProcess electricalLoadDynamicProcess = new ElectricalLoadDynamicProcess(electricalLoad,thermalLoad);
	
	int color;
	int colorCare;

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		byte b = nbt.getByte(str + "color");
		color = b & 0xF;
		colorCare = (b >> 4) & 1;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "color",(byte) (color + (colorCare << 4)));
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return electricalLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub
		return descriptor.getNodeMask() + Node.maskElectricalWire + (color << Node.maskColorShift) +(colorCare << Node.maskColorCareShift);
	}


	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotUIP(electricalLoad.Uc,electricalLoad.getCurrent());
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
			stream.writeByte( (color<<4));
	    	stream.writeShort((short) (electricalLoad.Uc*Node.networkSerializeUFactor));
	    	stream.writeShort((short) (electricalLoad.getCurrent()*Node.networkSerializeIFactor));
	    	stream.writeShort((short) (thermalLoad.Tc*Node.networkSerializeTFactor));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		descriptor.applyTo(electricalLoad, false);
		descriptor.applyTo(thermalLoad);
		descriptor.applyTo(electricalLoadDynamicProcess);
	}


	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
	/*	World w = sixNode.coordonate.world();
		boolean exist = w.blockExists(10000, 0, 0);
		int id = w.getBlockId(10000, 0, 0);*/
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		//int i;
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
		return 0.5;
	}

	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return 1/descriptor.thermalWarmLimit/0.05;
	}

/*
    public static void staticInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
    	ElectricalCableDescriptor cable = ElectricalCableDescriptor.getDescriptorFrom(itemStack);
    	list.add("UMax : " + cable.dielectricVoltage);
    }*/
    /*
    public static boolean staticInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
    	ElectricalCableDescriptor cable = ElectricalCableDescriptor.getDescriptorFrom(itemStack);
    	list.add("UMax : " + cable.dielectricVoltage);
    }*/
}
