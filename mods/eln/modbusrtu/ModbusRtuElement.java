package mods.eln.modbusrtu;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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

public class ModbusRtuElement extends TransparentNodeElement implements ProcessImage{
	
	
	public NodeElectricalGateInputOutput[] ioGate = new NodeElectricalGateInputOutput[4];
	public NodeElectricalGateOutputProcess[] ioGateProcess = new NodeElectricalGateOutputProcess[4];

	ModbusRtuDescriptor descriptor;
	
	
	int station = -1;
	
	String name = "";
	
	public ModbusRtuElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);

		for(int idx = 0;idx < 4;idx++){
			ioGate[idx] = new NodeElectricalGateInputOutput("ioGate"+idx);
			ioGateProcess[idx] = new NodeElectricalGateOutputProcess("ioGateProcess"+idx,ioGate[idx]);
			
			electricalLoadList.add(ioGate[idx]);
			electricalProcessList.add(ioGateProcess[idx]);
			
			ioGateProcess[idx].setHighImpedance(true);
		}

	   	this.descriptor = (ModbusRtuDescriptor) descriptor;
	   	
	}
	


	
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down || side.isY()) return null;
		return ioGate[side.getHorizontalIndex()];
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {

		if(lrdu == lrdu.Down && side.isNotY())
		{
			return NodeBase.maskElectricalGate;	
		}
		return 0;
	}


	
	@Override
	public String multiMeterString(Direction side) {
		return null;//Utils.plotUIP(powerLoad.Uc, powerLoad.getCurrent());

	}
	
	@Override
	public String thermoMeterString(Direction side) {
		return  null;
	}

	
	@Override
	public void initialize() {

		addToServer();
		connect();
    			
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

	
	@Override
	public byte networkUnserialize(DataInputStream stream, Player player) {
		// TODO Auto-generated method stub
		try {
			switch(super.networkUnserialize(stream, player)){
			case setStation:
				setStation(stream.readInt());
				break;
			case setName:
				setName(stream.readUTF());	
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return unserializeNulldId;
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
	public String[] getMethodNames() {
		// TODO Auto-generated method stub
		return new String[]{"writeDirection","readDirection","writeOutput","readOutput","readInput"};
	}


	
	
	
	
	

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context,
			int method, Object[] arguments) throws Exception {
		int id = -1;
		if(arguments.length < 1) return null;
		if(arguments[0].equals("XN")) id = 0;
		if(arguments[0].equals("XP")) id = 1;
		if(arguments[0].equals("ZN")) id = 2;
		if(arguments[0].equals("ZP")) id = 3;
		if(id == -1) return null;
		
		switch (method) {
		case 0:
			if(arguments.length < 2) return null;
			ioGateProcess[id].setHighImpedance((Boolean) arguments[1]);
			break;
		case 1:
			return new Object[]{ioGateProcess[id].isHighImpedance()};
		case 2:
			if(arguments.length < 2) return null;
			ioGateProcess[id].setOutputNormalized((Double) arguments[1]);
			break;
		case 3:
			return new Object[]{ioGateProcess[id].getOutputNormalized()};
		case 4:
			return new Object[]{ioGate[id].getInputNormalized()};
		default:
			break;
		}
		return null;
	}



	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setInteger(str + "station", station);
		nbt.setString(str + "name", name);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		station = nbt.getInteger(str + "station");
		name = nbt.getString(str + "name");
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





	@Override
	public boolean getCoil(int id) throws IllegalDataAddressException {
		if(id < 4){
			return ioGateProcess[id].isHighImpedance();
		}else if(id < 8){
			return ioGateProcess[id-4].getOutputNormalized() > 0.5;
		}else if(id < 12){
			return ioGate[id-8].isInputHigh();
		}
		return false;
	}





	@Override
	public byte getExceptionStatus() {
		// TODO Auto-generated method stub
		return 0;
	}





	@Override
	public short getHoldingRegister(int id)
			throws IllegalDataAddressException {
		if(id < 4){
			return (short) (ioGateProcess[id].isHighImpedance() ? 1 : 0);
		}else if(id < 8){
			return (short)(ioGateProcess[id-4].getOutputNormalized()*10000);
		}else if(id < 12){
			return (short) (ioGate[id-8].getInputNormalized()*10000);
		}
		return 0;
	}





	@Override
	public boolean getInput(int id) throws IllegalDataAddressException {
		// TODO Auto-generated method stub
		return getCoil(id);
	}





	@Override
	public short getInputRegister(int id) throws IllegalDataAddressException {
		// TODO Auto-generated method stub
		return getHoldingRegister(id);
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
		if(id < 4){
			ioGateProcess[id].setHighImpedance(value);
		}else if(id < 8){
			ioGateProcess[id-4].state(value);
		}		
	}





	@Override
	public void setHoldingRegister(int id, short value) {
		if(id < 4){
			ioGateProcess[id].setHighImpedance(value != 0);
		}else if(id < 8){
			ioGateProcess[id-4].setOutputNormalized(value/10000.0);
			System.out.println(value/10000.0);
		}
	}



	@Override
	public void setInput(int id, boolean value) {
		setCoil(id, value);
	}





	@Override
	public void setInputRegister(int id, short value) {
		setHoldingRegister(id, value);
	}





	@Override
	public void writeCoil(int id, boolean value)
			throws IllegalDataAddressException {
		setCoil(id, value);
		
	}





	@Override
	public void writeHoldingRegister(int id, short value)
			throws IllegalDataAddressException {
		setHoldingRegister(id, value);
	}

}
