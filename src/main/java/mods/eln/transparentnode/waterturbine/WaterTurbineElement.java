package mods.eln.transparentnode.waterturbine;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
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

public class WaterTurbineElement extends TransparentNodeElement{

	NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");

	PowerSource powerSource = new PowerSource("powerSource",positiveLoad);
	
	WaterTurbineSlowProcess slowProcess = new WaterTurbineSlowProcess(this);
	
	WaterTurbineDescriptor descriptor;
	

	
	public WaterTurbineElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		

		
		this.descriptor = (WaterTurbineDescriptor) descriptor;
		
		electricalLoadList.add(positiveLoad);
		
		electricalComponentList.add(powerSource);
		slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 2, 2));
		slowProcessList.add(slowProcess);
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == front) return positiveLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		
		if(lrdu != LRDU.Down) return 0;
		if(side == front) return NodeBase.maskElectricalPower;
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

	Coordonate waterCoord;
		
	@Override
	public void initialize() {

		setPhysicalValue();
		waterCoord = descriptor.getWaterCoordonate(node.coordonate.world());
		waterCoord.applyTransformation(front, node.coordonate);
		powerSource.setUmax(descriptor.maxVoltage);
		powerSource.setImax(descriptor.nominalPower*5/descriptor.maxVoltage);
		connect();
	}


	private void setPhysicalValue() {
		descriptor.cable.applyTo(positiveLoad);
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
		
		return new WaterTurbineContainer(this.node, player, inventory);
	}


	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		
		super.networkSerialize(stream);
		try {
			stream.writeFloat((float) (powerSource.getP()/descriptor.nominalPower));
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		
		return false;
	}


	
	
	 

}
