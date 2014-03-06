package mods.eln.windturbine;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.PlayerManager;
import mods.eln.ghost.GhostObserver;
import mods.eln.item.DynamoDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalPowerSource;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class WindTurbineElement extends TransparentNodeElement{

	NodeElectricalLoad positiveLoad = new NodeElectricalLoad("positiveLoad");
	NodeElectricalLoad negativeLoad = new NodeElectricalLoad("negativeLoad");
	
	ElectricalPowerSource powerSource = new ElectricalPowerSource(positiveLoad, negativeLoad);
	
	WindTurbineSlowProcess slowProcess = new WindTurbineSlowProcess("slowProcess",this);
	
	WindTurbineDescriptor descriptor;
	
	Direction cableFront = Direction.ZP;
	
	public WindTurbineElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		

		
		this.descriptor = (WindTurbineDescriptor) descriptor;
		
		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		
		electricalProcessList.add(powerSource);
		slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 4, 4));
		slowProcessList.add(slowProcess);
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == cableFront.left()) return positiveLoad;
		if(side == cableFront.right() && ! grounded) return negativeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(lrdu != LRDU.Down) return 0;
		if(side == cableFront.left()) return NodeBase.maskElectricalPower;
		if(side == cableFront.right() && ! grounded) return node.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
		setPhysicalValue();
		
		connect();
	}


	private void setPhysicalValue() {
		descriptor.cable.applyTo(positiveLoad,false);
		descriptor.cable.applyTo(negativeLoad,grounded);
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		
		if(Eln.playerManager.get(entityPlayer).getInteractEnable()){
			cableFront = cableFront.right();
			reconnect();
		}
		return false;
	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0 , 64, this);
	
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new WindTurbineContainer(this.node, player, inventory);
	}


	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeFloat((float) slowProcess.getWind());
			stream.writeFloat((float) (powerSource.getP()/descriptor.nominalPower));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		cableFront.writeToNBT(nbt, str + "cableFront");
		System.out.println(cableFront);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		cableFront = Direction.readFromNBT(nbt, str + "cableFront");
		System.out.println(cableFront);
	}
	
	 

}
