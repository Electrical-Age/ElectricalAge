package mods.eln.wirelesssignal;

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
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class WirelessSignalRxElement extends SixNodeElement implements IWirelessSignalTx{

	NodeElectricalGateOutput outputGate = new NodeElectricalGateOutput("outputGate");
	NodeElectricalGateOutputProcess outputGateProcess = new NodeElectricalGateOutputProcess("outputGateProcess",outputGate);
	
	public int channel = 0,generation = 1000;
	
	WirelessSignalRxProcess slowProcess = new WirelessSignalRxProcess(this);
	
	WirelessSignalRxDescriptor descriptor;
	
	public WirelessSignalRxElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		
		this.descriptor = (WirelessSignalRxDescriptor) descriptor;
		electricalLoadList.add(outputGate);
		electricalProcessList.add(outputGateProcess);
		
		slowProcessList.add(slowProcess);
		
		front = LRDU.Down;


		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRegister(this);
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
		if(front == lrdu) return Node.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return outputGate.plot("Output gate");
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
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
	public void destroy() {
		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRemove(this);
		super.destroy();
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setInteger(str + "channel", channel);
		nbt.setInteger(str + "generation", generation);
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRemove(this);
		
		super.readFromNBT(nbt, str);
		channel = nbt.getInteger(str + "channel");
		generation = nbt.getInteger(str + "generation");
		if(this.descriptor.repeater)
			WirelessSignalTxElement.channelRegister(this);
	}

	@Override
	public Coordonate getCoordonate() {
		// TODO Auto-generated method stub
		return sixNode.coordonate;
	}

	@Override
	public int getRange() {
		// TODO Auto-generated method stub
		return descriptor.range;
	}

	@Override
	public int getChannel() {
		// TODO Auto-generated method stub
		return channel;
	}

	@Override
	public int getGeneration() {
		// TODO Auto-generated method stub
		return generation;
	}

	@Override
	public double getValue() {
		// TODO Auto-generated method stub
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
				channel = stream.readInt();
				needPublish();
				if(this.descriptor.repeater)
					WirelessSignalTxElement.channelRegister(this);
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
	
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeInt(channel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	

}
