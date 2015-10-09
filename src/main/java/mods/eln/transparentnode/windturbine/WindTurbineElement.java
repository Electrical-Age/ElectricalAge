package mods.eln.transparentnode.windturbine;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.*;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

public class WindTurbineElement extends TransparentNodeElement{

	NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
	//NodeElectricalLoad negativeLoad = new NodeElectricalLoad("negativeLoad");
	
	

	PowerSource powerSource = new PowerSource("powerSource",positiveLoad);
	
	WindTurbineSlowProcess slowProcess = new WindTurbineSlowProcess("slowProcess",this);
	
	WindTurbineDescriptor descriptor;
	
	Direction cableFront = Direction.ZP;
	
	public WindTurbineElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		

		
		this.descriptor = (WindTurbineDescriptor) descriptor;
		
		electricalLoadList.add(positiveLoad);
		//electricalLoadList.add(negativeLoad);
		
		electricalComponentList.add(powerSource);
		slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 4, 4));
		slowProcessList.add(slowProcess);
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == cableFront.left()) return positiveLoad;
		//if(side == cableFront.right() && ! grounded) return negativeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		
		if(lrdu != LRDU.Down) return 0;
		if(side == cableFront.left()) return NodeBase.maskElectricalPower;
		if(side == cableFront.right() && ! grounded) return node.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		
		return null;
	}

	@Override
	public String thermoMeterString(Direction side) {
		
		return null;
	}

	@Override
	public void initialize() {
		setPhysicalValue();
		powerSource.setImax(descriptor.nominalPower*5/descriptor.maxVoltage);
		connect();
	}


	private void setPhysicalValue() {
		descriptor.cable.applyTo(positiveLoad);
	//	descriptor.cable.applyTo(negativeLoad,grounded);
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		
		if(Utils.isPlayerUsingWrench(entityPlayer)){
			cableFront = cableFront.right();
			reconnect();
		}
		return false;
	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0 , 64, this);
	
	@Override
	public IInventory getInventory() {
		
		return inventory;
	}
	@Override
	public boolean hasGui() {
		
		return false;
	}
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		
		return new WindTurbineContainer(this.node, player, inventory);
	}


	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		
		super.networkSerialize(stream);
		try {
			stream.writeFloat((float) slowProcess.getWind());
			stream.writeFloat((float) (powerSource.getP()/descriptor.nominalPower));
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		
		super.writeToNBT(nbt);
		cableFront.writeToNBT(nbt, "cableFront");
		Utils.println(cableFront);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		
		super.readFromNBT(nbt);
		cableFront = Direction.readFromNBT(nbt, "cableFront");
		Utils.println(cableFront);
	}
	
	 

}
