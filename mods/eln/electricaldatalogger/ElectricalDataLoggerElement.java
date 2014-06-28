package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.text.MaskFormatter;




import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.IVoltageDestructorDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;

import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalDataLoggerElement extends SixNodeElement {

	public static final int logsSizeMax = 256;

	public ElectricalDataLoggerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

    	this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;
		
    	inputGate = new NodeElectricalGateInput("inputGate",false);
		
		electricalLoadList.add(inputGate);
    	slowProcessList.add(slowProcess);
    	sampleStackReset();
	}

	public int sampleStack, sampleStackNbr;
	
	public static boolean canBePlacedOnSide(Direction side,SixNodeDescriptor descriptor) {
		if(((ElectricalDataLoggerDescriptor)descriptor).onFloor && side == Direction.YN) return true;
		return false;
	}
	
	NodeElectricalGateInput inputGate;
	ElectricalDataLoggerProcess slowProcess = new ElectricalDataLoggerProcess(this);
	public ElectricalDataLoggerDescriptor descriptor;
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

	public SixNodeElementInventory getInventory() {
		return inventory;
	}

	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}
	
	public double timeToNextSample = 0;

	public DataLogs logs = new DataLogs(logsSizeMax);
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);

		logs.readFromNBT(nbt, "logs");
		pause = nbt.getBoolean("pause");
		timeToNextSample = nbt.getDouble("timeToNextSample");
		sampleStack = nbt.getInteger("sampleStack");
		sampleStackNbr = nbt.getInteger("sampleStackNbr");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte)((front.toInt() << 0)));
		nbt.setDouble("timeToNextSample", timeToNextSample);
		nbt.setBoolean("pause", pause);
		
		logs.writeToNBT(nbt, "logs");
		nbt.setInteger("sampleStack", sampleStack);
		nbt.setInteger("sampleStackNbr", sampleStackNbr);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {	
		if(front.inverse() == lrdu) return inputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front.inverse() == lrdu) return NodeBase.maskElectricalInputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return inputGate.plot("In: ");
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	static final byte publishId = 1, dataId = 2;
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte(logs.unitType);
			stream.writeBoolean(pause);
			stream.writeFloat((float) logs.samplingPeriod);
			stream.writeFloat((float) logs.maxValue);
			stream.writeFloat((float) logs.minValue);
		} catch (IOException e) {
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

	public void computeElectricalLoad() {
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if(Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
    	return false;
	}
	
	public static final byte resetId = 1;
	public static final byte setSamplingPeriodeId = 2, setMaxValue = 3;
	public static final byte setUnitId = 4;
	public static final byte newClientId = 5;
	public static final byte printId = 6;
	public static final byte tooglePauseId = 7;
	public static final byte setMinValue = 8;
	
	public static final byte toClientLogsClear = 1;
	public static final byte toClientLogsAdd = 2;

	boolean printToDo;
	boolean pause = false;
	
	@Override
	public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
		super.networkUnserialize(stream);
		byte header;
		try {
			switch(header = stream.readByte()) {
			case setSamplingPeriodeId:
				logs.reset();
				sampleStackReset();
				logs.samplingPeriod = stream.readFloat();
				timeToNextSample = 0.1;
				needPublish();
				break;
			case setMaxValue:
				logs.maxValue = stream.readFloat();
				needPublish();
				break;
			case setMinValue:
				logs.minValue = stream.readFloat();
				needPublish();
				break;
			case setUnitId:
				//sampleStackReset();
				//logs.reset();
				logs.unitType = stream.readByte();
				needPublish();
				break;
			case resetId:
				sampleStackReset();
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
			
			if(header == resetId || header == newClientId || header == setSamplingPeriodeId) {
		    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
		        DataOutputStream packet = new DataOutputStream(bos);   	
		        
				preparePacketForClient(packet);
				
				packet.writeByte(toClientLogsClear);
				int size = logs.size();
				for(int idx = size - 1; idx >= 0; idx--) {
					packet.writeByte(logs.read(idx));
				}
				if(header == newClientId)
					sendPacketToClient(bos, player);
				else
					sendPacketToAllClient(bos);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new ElectricalDataLoggerContainer(player, inventory);
	}

	public void sampleStackReset() {
		sampleStack = 0;
		sampleStackNbr = 0;
	}
}
