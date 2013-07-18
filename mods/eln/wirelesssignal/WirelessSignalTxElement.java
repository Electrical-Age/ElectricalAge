package mods.eln.wirelesssignal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class WirelessSignalTxElement extends SixNodeElement implements IWirelessSignalTx{


	

	public static HashMap<Integer, ArrayList<IWirelessSignalTx>> channelMap = new HashMap<Integer, ArrayList<IWirelessSignalTx>>(); 
	
	NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate");

	WirelessSignalTxDescriptor descriptor;
	
	
	
	public int channel = 0;
	
	public WirelessSignalTxElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(inputGate);

		front = LRDU.Down;
		this.descriptor = (WirelessSignalTxDescriptor) descriptor;
		channelRegister(this);
	}

	static void channelRegister(IWirelessSignalTx tx)
	{
		int channel = tx.getChannel();
		ArrayList<IWirelessSignalTx> list = channelMap.get(channel);
		if(list == null) 
			channelMap.put(channel,list =  new ArrayList<IWirelessSignalTx>());
		list.add(tx);
	}
	
	static void channelRemove(IWirelessSignalTx tx)
	{
		int channel = tx.getChannel();
		ArrayList<IWirelessSignalTx> list = channelMap.get(channel);
		if(list == null) return;
		list.remove(tx);
		if(list.size() == 0)
			channelMap.remove(channel);
	}
	
	
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return inputGate.plot("Input gate");
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
		// TODO Auto-generated method stub
		channelRemove(this);
		super.destroy();
	}
	
	
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setInteger(str + "channel", channel);
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		channelRemove(this);
		
		super.readFromNBT(nbt, str);
		channel = nbt.getInteger(str + "channel");
		
		channelRegister(this);
		
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
		return 0;
	}

	@Override
	public double getValue() {
		// TODO Auto-generated method stub
		return inputGate.getNormalized();
	}


	
	public static final byte setChannelId = 1;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		
		super.networkUnserialize(stream);
		
		try {
			switch(stream.readByte()){
			case setChannelId:
				channelRemove(this);
				channel = stream.readInt();
				needPublish();
				channelRegister(this);
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
