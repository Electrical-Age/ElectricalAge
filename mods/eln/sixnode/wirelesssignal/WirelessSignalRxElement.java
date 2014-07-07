package mods.eln.sixnode.wirelesssignal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;

public class WirelessSignalRxElement extends SixNodeElement implements IWirelessSignalTx{

	NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");
	NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess",outputGate);
	
	public int generation = 1000;
	public String channel = "Default channel";
	
	WirelessSignalRxProcess slowProcess = new WirelessSignalRxProcess(this);
	
	WirelessSignalRxDescriptor descriptor;
	
	public WirelessSignalRxElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		
		this.descriptor = (WirelessSignalRxDescriptor) descriptor;
		electricalLoadList.add(outputGate);
		electricalComponentList.add(outputGateProcess);
		
		slowProcessList.add(slowProcess);
		
		front = LRDU.Down;


		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRegister(this);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		
		if(front == lrdu) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		
		return outputGate.plot("Output gate");
	}

	@Override
	public String thermoMeterString() {
		
		return null;
	}

	@Override
	public void initialize() {
		
		
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		if(Utils.isPlayerUsingWrench(entityPlayer))
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		return false;
	}
	boolean connection = false;
	
	void setConnection(boolean connection)
	{
		if(connection != this.connection) {
			this.connection = connection;
			needPublish();
		}

	}
	
	@Override
	public void destroy() {
		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRemove(this);
		super.destroy();
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		
		super.writeToNBT(nbt);
		nbt.setString("channel", channel);
		nbt.setInteger("generation", generation);
		nbt.setBoolean("connection", connection);
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRemove(this);
		
		super.readFromNBT(nbt);
		channel = nbt.getString("channel");
		generation = nbt.getInteger("generation");
		connection = nbt.getBoolean("connection");
		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRegister(this);
	}

	@Override
	public Coordonate getCoordonate() {
		
		return sixNode.coordonate;
	}

	@Override
	public int getRange() {
		
		return descriptor.range;
	}

	@Override
	public String getChannel() {
		
		return channel;
	}

	@Override
	public int getGeneration() {
		
		return generation;
	}

	@Override
	public double getValue() {
		
		return outputGateProcess.getOutputNormalized();
	}
	
	
	
	public static final byte setChannelId = 1;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		
		super.networkUnserialize(stream);
		
		try {
			switch(stream.readByte()){
			case setChannelId:
				if(this.descriptor.repeater)
					WirelessSignalTxElement.channelRemove(this);
				channel = stream.readUTF();
				needPublish();
				if(this.descriptor.repeater)
					WirelessSignalTxElement.channelRegister(this);
				break;
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
	public void networkSerialize(DataOutputStream stream) {
		
		super.networkSerialize(stream);
		try {
			stream.writeUTF(channel);
			stream.writeBoolean(connection);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}	

}
