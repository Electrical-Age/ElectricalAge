package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.text.MaskFormatter;

import org.bouncycastle.crypto.modes.SICBlockCipher;

import cpw.mods.fml.common.network.Player;


import mods.eln.Eln;
import mods.eln.INBTTReady;
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
import mods.eln.node.NodeElectricalGateInput;
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


public class ElectricalDataLoggerElement extends SixNodeElement {

	public static final int logsSizeMax = 128;

	public ElectricalDataLoggerElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

    	this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;
		
    	inputGate = new NodeElectricalGateInput("inputGate");
		
		electricalLoadList.add(inputGate);
    	thermalProcessList.add(slowProcess);

	}

	NodeElectricalGateInput inputGate;
	ElectricalDataLoggerProcess slowProcess = new ElectricalDataLoggerProcess(this);
	public ElectricalDataLoggerDescriptor descriptor;
	
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(2,64,this);

	public SixNodeElementInventory getInventory() {
		return inventory;
	}

	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	
	public double samplingPeriod = 1;
	public double highValue = 60;
	public double timeToNextSample = 0;
	public byte unitType = voltageType;
	public DataLogs logs = new DataLogs(logsSizeMax);
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
        byte value = nbt.getByte(str + "front");
        front = LRDU.fromInt((value>>0) & 0x3);
        samplingPeriod = nbt.getDouble(str + "samplingPeriod");
		highValue = nbt.getDouble(str + "highValue");
		unitType = nbt.getByte(str + "unitType");
		logs.readFromNBT(nbt, str + "logs");
		pause = nbt.getBoolean(str + "pause");
		timeToNextSample = nbt.getDouble(str + "timeToNextSample");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "front",(byte) ((front.toInt()<<0)));
		nbt.setDouble(str + "samplingPeriod", samplingPeriod);
		nbt.setDouble(str + "highValue", highValue);
		nbt.setByte(str + "unitType", unitType);
		nbt.setDouble(str + "timeToNextSample", timeToNextSample);
		nbt.setBoolean(str + "pause", pause);
		
		logs.writeToNBT(nbt, str + "logs");
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {	
		if(front == lrdu) return inputGate;
		
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu) return Node.maskElectricalInputGate;
		
		return 0;
	}

	@Override
	public String multiMeterString() {
		return inputGate.plot("in");

	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return "";
	}


	static final byte publishId = 1,dataId = 2;
	
	static final byte voltageType = 0,currentType = 1,powerType = 2,celsiusType = 3;
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeByte(unitType);
			stream.writeBoolean(pause);
			stream.writeFloat((float) samplingPeriod);
			stream.writeFloat((float) highValue);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	@Override
	public void initialize() {
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

    	return false;

	}
	
	public static final byte resetId = 1;
	public static final byte setSamplingPeriodeId = 2,setHighValue = 3;
	public static final byte setUnitId = 4;
	public static final byte newClientId = 5;
	public static final byte printId = 6;
	public static final byte tooglePauseId = 7;

	
	public static final byte toClientLogsClear = 1;
	public static final byte toClientLogsAdd = 2;

	boolean printToDo;
	boolean pause = false;
	@Override
	public void networkUnserialize(DataInputStream stream,Player player) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		byte header;
		try {
			switch(header = stream.readByte())
			{

			case setSamplingPeriodeId:
				logs.reset();
				samplingPeriod = stream.readFloat();
				needPublish();
				break;
			case setHighValue:
				highValue = stream.readFloat();
				needPublish();
				break;
			case setUnitId:
				logs.reset();
				unitType = stream.readByte();
				needPublish();
				break;
			case resetId:
				logs.reset();
				break;
			case newClientId:

				break;
			case printId:
				printToDo = true;
				break;
			case tooglePauseId:
				pause = !pause;
				needPublish();
				break;
			
			}
			
			if(header == resetId || header == newClientId || header == setSamplingPeriodeId)
			{
		    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
		        DataOutputStream packet = new DataOutputStream(bos);   	
		        
				preparePacketForClient(packet);
				
				packet.writeByte(toClientLogsClear);
				int size = logs.size();
				for(int idx = size - 1;idx >= 0;idx--)
				{
					packet.writeByte(logs.read(idx));
				}
				if(header == newClientId)
					sendPacketToClient(bos, player);
				else
					sendPacketToAllClient(bos);
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
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalDataLoggerContainer(player, inventory);
	}
	
	
	
}
