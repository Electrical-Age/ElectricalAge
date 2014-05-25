package mods.eln.wirelesssignal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class WirelessSignalTxElement extends SixNodeElement implements IWirelessSignalTx{


	

	public static HashMap<String, ArrayList<IWirelessSignalTx>> channelMap = new HashMap<String, ArrayList<IWirelessSignalTx>>(); 
	
	NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate",false);

	WirelessSignalTxDescriptor descriptor;
	

	public static IWirelessSignalTx getBestTx(String channel,Coordonate rxCoordonate){
		IWirelessSignalTx bestTx = null;
		float bestPower = -1f;
		int bestGeneration = 1000;
		Coordonate rxC = rxCoordonate;
	
		ArrayList<IWirelessSignalTx> txList = WirelessSignalTxElement.channelMap.get(channel);
		if(txList != null) {
			int x = rxC.x;
			int y = rxC.y;
			int z = rxC.z;
			for(IWirelessSignalTx tx : txList){
				Coordonate txC = tx.getCoordonate();
				double distance = txC.trueDistanceTo(rxC);

				if(txC.dimention == rxC.dimention && distance <= tx.getRange() && tx.getGeneration() < 100){
					float power = 0f;
					if(Double.isNaN(distance = getVirtualDistance(distance, txC, rxC))) continue;
					power = (float) (tx.getRange() - distance);
							

					if(power < -0.1) continue;
					
						
					if(tx.getGeneration() < bestGeneration || (tx.getGeneration() == bestGeneration && power > bestPower)){
						bestPower = power;
						bestTx = tx;
						bestGeneration = tx.getGeneration();
					}
				}
			}
		}
		
		return bestTx;		
	}

	
	static public double getVirtualDistance(double distance,Coordonate txC,Coordonate rxC)
	{
		double virtualDistance = distance;
		if(distance > 2){
			double vx,vy,vz;
			double dx,dy,dz;
			vx = rxC.x + 0.5;
			vy = rxC.y + 0.5;
			vz = rxC.z + 0.5;
			
			dx = (txC.x - rxC.x)/distance;
			dy = (txC.y - rxC.y)/distance;
			dz = (txC.z - rxC.z)/distance;
			Coordonate c = new Coordonate();
			c.setDimention(rxC.dimention);
			
			for(int idx = 0;idx < distance - 1;idx++){
				vx += dx;
				vy += dy;
				vz += dz;
				c.x = (int) vx;
				c.y = (int) vy;
				c.z = (int) vz;
				//if(c.getBlockExist() == false) return Double.NaN;
				Block b = c.getBlock();
				if(b != null && b.isOpaqueCube()){
					virtualDistance += 2.0;
				}
				
			}
		}
		return virtualDistance;
	}
	
	
	public String channel = "Default channel";
	
	public WirelessSignalTxElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(inputGate);

		front = LRDU.Down;
		this.descriptor = (WirelessSignalTxDescriptor) descriptor;
		channelRegister(this);
	}

	public static void channelRegister(IWirelessSignalTx tx)
	{
		String channel = tx.getChannel();
		ArrayList<IWirelessSignalTx> list = channelMap.get(channel);
		if(list == null) 
			channelMap.put(channel,list =  new ArrayList<IWirelessSignalTx>());
		list.add(tx);
	}
	
	public static void channelRemove(IWirelessSignalTx tx)
	{
		String channel = tx.getChannel();
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
		if(front == lrdu) return NodeBase.maskElectricalInputGate;
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
		nbt.setString(str + "channel", channel);
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		channelRemove(this);
		
		super.readFromNBT(nbt, str);
		channel = nbt.getString(str + "channel");
		
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
	public String getChannel() {
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
				channel = stream.readUTF();
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
			stream.writeUTF(channel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
