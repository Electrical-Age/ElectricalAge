package mods.eln.modbusrtu;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.Descriptor;

import com.google.common.base.CaseFormat;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.exception.IllegalDataAddressException;
import com.serotonin.modbus4j.ip.tcp.TcpSlave;

import cpw.mods.fml.common.network.Player;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.lampsocket.LampSocketContainer;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateInputOutput;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalSourceRefGroundProcess;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.TransformerProcess;
import mods.eln.sim.VoltageWatchdogProcessForInventoryItemBlockDamageDualLoad;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingData;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;

public class ModbusRtuElement extends SixNodeElement implements ProcessImage{
	
	
	public NodeElectricalGateInputOutput[] ioGate = new NodeElectricalGateInputOutput[4];
	public NodeElectricalGateOutputProcess[] ioGateProcess = new NodeElectricalGateOutputProcess[4];


	HashMap<Integer,ServerWirelessTxStatus> wirelessTxStatusList = new HashMap<Integer,ServerWirelessTxStatus>();
	HashMap<Integer,ServerWirelessRxStatus> wirelessRxStatusList = new HashMap<Integer,ServerWirelessRxStatus>();
	
	ModbusRtuDescriptor descriptor;
	
	
	static final int ioStartOffset = 16;
	static final int ioRange = 8;
	int station = -1;
	
	String name = "";
	
	public ModbusRtuElement(SixNode sixNode,Direction side,SixNodeDescriptor descriptor) {
		super(sixNode,side,descriptor);

		for(int idx = 0;idx < 4;idx++){
			ioGate[idx] = new NodeElectricalGateInputOutput("ioGate"+idx);
			ioGateProcess[idx] = new NodeElectricalGateOutputProcess("ioGateProcess"+idx,ioGate[idx]);
			
			electricalLoadList.add(ioGate[idx]);
			electricalProcessList.add(ioGateProcess[idx]);
			
			ioGateProcess[idx].setHighImpedance(true);
			
			mapping.add(new modbusAnalogIoSlot(ioStartOffset + idx * ioRange, ioRange, ioGate[idx], ioGateProcess[idx]));
		}
		
		slowProcessList.add(new ModbusRtuSlowProcess());

	   	this.descriptor = (ModbusRtuDescriptor) descriptor;
	   	
	}
	


	
	
	class ModbusRtuSlowProcess implements IProcess{

		@Override
		public void process(double time) {
			for (ServerWirelessRxStatus rx : wirelessRxStatusList.values()) {
				if(rx.isConnected() != rx.connected){
					rx.connected = ! rx.connected;
					sendRx1Connected(rx);
				}
			}
		}
		
	}
	
	
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		return ioGate[lrdu.toInt()];
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		return NodeBase.maskElectricalGate;	
	}


	
	@Override
	public String multiMeterString() {
		return null;//Utils.plotUIP(powerLoad.Uc, powerLoad.getCurrent());

	}
	
	@Override
	public String thermoMeterString() {
		return  null;
	}

	
	@Override
	public void initialize() {

		addToServer();
		//connect();
    			
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
		removeFromServer();
		
		// Remove all TX signals.
		for(Integer key: wirelessTxStatusList.keySet()) {
			ServerWirelessTxStatus status = wirelessTxStatusList.get(key);
			status.delete();
			wirelessTxStatusList.remove(key);
		}
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		
		
		try {
			stream.writeInt(station);
			stream.writeUTF(name);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static final byte setStation = 1;
	static final byte setName = 2;
	static final byte serverTxAdd = 3;
	static final byte serverRxAdd = 4;
	static final byte serverTxConfig = 5;
	static final byte serverRxConfig = 6;
	static final byte serverTxDelete = 7;
	static final byte serverRxDelete = 8;
	static final byte serverAllSyncronise = 9;
	
	static final byte clientAllSyncronise = 1;
	static final byte clientTx1Syncronise = 2;
	static final byte clientRx1Syncronise = 3;
	static final byte clientTxDelete = 4;
	static final byte clientRxDelete = 5;
	static final byte clientRx1Connected = 6;
	
	
	void sendTx1Syncronise(WirelessTxStatus tx){
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);   	
        
		preparePacketForClient(packet);
		
		try {
			packet.writeByte(clientTx1Syncronise);
			
			tx.writeTo(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//TODO

		sendPacketToAllClient(bos);	
	}
	
	void sendRx1Syncronise(WirelessRxStatus rx){
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);   	
        
		preparePacketForClient(packet);
		
		try {
			packet.writeByte(clientRx1Syncronise);
			
			rx.writeTo(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//TODO

		sendPacketToAllClient(bos);	
	}
		
	void sendRx1Connected(WirelessRxStatus rx){
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);   	
        
		preparePacketForClient(packet);
		
		try {
			packet.writeByte(clientRx1Connected);
			packet.writeInt(rx.uuid);
			packet.writeBoolean(rx.connected);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//TODO

		sendPacketToAllClient(bos);	
	}
		
	@Override
	public void networkUnserialize(DataInputStream stream, Player player) {
		super.networkUnserialize(stream, player);
		try {
			switch(stream.readByte()){
			case setStation:
				setStation(stream.readInt());
				break;
			case setName:
				setName(stream.readUTF());	
				break;
			case serverTxAdd:
				{
					int idx = 0;
					String name = stream.readUTF();
					
					
					int uuid = 0;
					/*do{
						uuid = (int)(Math.random()*100000);
					}while(wirelessTxStatusList.containsKey(uuid));*/
					for (ServerWirelessTxStatus tx : wirelessTxStatusList.values()) {
						uuid = Math.max(uuid, tx.uuid);
					}
					uuid++;
					ServerWirelessTxStatus tx;
					wirelessTxStatusList.put(uuid,tx = new ServerWirelessTxStatus(name, -1, 0,sixNode.coordonate,uuid, this));					
				
					
					sendTx1Syncronise(tx);
				}
				break;
			case serverRxAdd:
				{
					int idx = 0;
					String name = stream.readUTF();

					int uuid = 0;
					/*do{
						uuid = (int)(Math.random()*100000);
					}while(wirelessTxStatusList.containsKey(uuid));*/
					for (ServerWirelessRxStatus rx : wirelessRxStatusList.values()) {
						uuid = Math.max(uuid, rx.uuid);
					}
					uuid++;
					
					ServerWirelessRxStatus rx;
					wirelessRxStatusList.put(uuid,rx = new ServerWirelessRxStatus(name, -1, false,uuid,this));
				
					
					sendRx1Syncronise(rx);
				}
				break;
			case serverTxConfig:
			{
				int uuid = stream.readInt();
				String name = stream.readUTF();
				int id = stream.readInt();

				WirelessTxStatus tx = wirelessTxStatusList.get(uuid);
				if(tx != null){
					tx.setName(name);
					tx.setId(id);
					sendTx1Syncronise(tx);

				}
				
								
			}
				break;
			case serverRxConfig:
				{
					int uuid = stream.readInt();
					String name = stream.readUTF();
					int id = stream.readInt();

					WirelessRxStatus rx = wirelessRxStatusList.get(uuid);
					if(rx != null){
						rx.setName(name);
						rx.setId(id);
						sendRx1Syncronise(rx);
					}
				}
				break;
			case serverTxDelete:
			{
				int uuid = stream.readInt();
				ServerWirelessTxStatus tx = wirelessTxStatusList.get(uuid);
				if(tx != null){
					tx.delete();
					wirelessTxStatusList.remove(tx.uuid);
					
			    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
			        DataOutputStream packet = new DataOutputStream(bos);   	
			        
					preparePacketForClient(packet);
					
					packet.writeByte(clientTxDelete);
					packet.writeInt(uuid);
					//TODO

					sendPacketToAllClient(bos);					
				}
			}
				break;
			case serverRxDelete:
				int uuid = stream.readInt();
				ServerWirelessRxStatus rx = wirelessRxStatusList.get(uuid);
				if(rx != null){
					rx.delete();
					wirelessRxStatusList.remove(uuid);
					
			    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
			        DataOutputStream packet = new DataOutputStream(bos);   	
			        
					preparePacketForClient(packet);
					
					packet.writeByte(clientRxDelete);
					packet.writeInt(uuid);
					//TODO

					sendPacketToAllClient(bos);					
				}
				break;
			case serverAllSyncronise:
				{
			    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
			        DataOutputStream packet = new DataOutputStream(bos);   	
			        
					preparePacketForClient(packet);
					
					packet.writeByte(clientAllSyncronise);

					packet.writeInt(wirelessTxStatusList.size());
					for (ServerWirelessTxStatus e : wirelessTxStatusList.values()) {
						e.writeTo(packet);
					}
					
					packet.writeInt(wirelessRxStatusList.size());
					for (WirelessRxStatus e : wirelessRxStatusList.values()) {
						e.writeTo(packet);
					}

					sendPacketToClient(bos, player);

				}
				break;
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	/*
	public String getType() {
		// TODO Auto-generated method stub
		return "Probe";
	}
*/


	private void setName(String name) {
		this.name = name;
		needPublish();
	}





	private void setStation(int port) {
		removeFromServer();
		this.station = port;
		addToServer();
		needPublish();
	}





	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setInteger(str + "station", station);
		nbt.setString(str + "name", name);
		
		int idx;
		
		nbt.setInteger(str + "txCnt", wirelessTxStatusList.size());	
		idx = 0;
		for (ServerWirelessTxStatus tx : wirelessTxStatusList.values()) {
			tx.writeToNBT(nbt,str + "tx" + idx);		
			idx++;
		}
		
		nbt.setInteger(str + "rxCnt", wirelessRxStatusList.size());	
		idx = 0;
		for (ServerWirelessRxStatus rx : wirelessRxStatusList.values()) {
			rx.writeToNBT(nbt,str + "rx" + idx);		
			idx++;
		}
		
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		station = nbt.getInteger(str + "station");
		name = nbt.getString(str + "name");
		
		int cnt;
		
		cnt = nbt.getInteger(str + "txCnt");
		for(int idx = 0;idx < cnt;idx++){
			ServerWirelessTxStatus tx = new ServerWirelessTxStatus(nbt,str + "tx" + idx,this);
			wirelessTxStatusList.put(tx.uuid, tx);
		}
		cnt = nbt.getInteger(str + "rxCnt");
		for(int idx = 0;idx < cnt;idx++){
			ServerWirelessRxStatus rx = new ServerWirelessRxStatus(nbt,str + "rx" + idx,this);
			wirelessRxStatusList.put(rx.uuid, rx);
		}
		
	}
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	boolean addedOnServer = false;
	void addToServer(){
		if(station != -1){
			addedOnServer = Eln.modbusServer.add(this);
		}
	}
	
	void removeFromServer(){
		if(addedOnServer)
			Eln.modbusServer.remove(this);
		addedOnServer = false;
	}


	ArrayList<IModbusSlot> mapping = new ArrayList<IModbusSlot>();
	ModbusNullSlot nullSlot = new ModbusNullSlot();
	
	IModbusSlot getModbusSlot(int id){
		for (IModbusSlot slot : mapping) {
			if(id >= slot.getOffset() && id < slot.getOffset() + slot.getSize()){
				return slot;
			}
		}
		return nullSlot;
	}
	

	@Override
	public boolean getCoil(int id) throws IllegalDataAddressException {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		return slot.getCoil(id);
	}





	@Override
	public byte getExceptionStatus() {
		// TODO Auto-generated method stub
		return 0;
	}





	@Override
	public short getHoldingRegister(int id)
			throws IllegalDataAddressException {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		return slot.getHoldingRegister(id);
	}





	@Override
	public boolean getInput(int id) throws IllegalDataAddressException {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		return slot.getInput(id);
	}





	@Override
	public short getInputRegister(int id) throws IllegalDataAddressException {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		return slot.getInputRegister(id);
	}





	@Override
	public byte[] getReportSlaveIdData() {
		// TODO Auto-generated method stub
		return null;
	}





	@Override
	public int getSlaveId() {
		// TODO Auto-generated method stub
		return station;
	}





	@Override
	public void setCoil(int id, boolean value) {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		slot.setCoil(id, value);
	}





	@Override
	public void setHoldingRegister(int id, short value) {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		slot.setHoldingRegister(id, value);
	}



	@Override
	public void setInput(int id, boolean value) {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		slot.setInput(id, value);
	}





	@Override
	public void setInputRegister(int id, short value) {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		slot.setInputRegister(id, value);
	}





	@Override
	public void writeCoil(int id, boolean value)
			throws IllegalDataAddressException {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		slot.writeCoil(id, value);
	}





	@Override
	public void writeHoldingRegister(int id, short value)
			throws IllegalDataAddressException {
		IModbusSlot slot = getModbusSlot(id); id -= slot.getOffset();
		slot.writeHoldingRegister(id, value);
	}

}
