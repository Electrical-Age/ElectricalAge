package mods.eln.transparentnode.solarpannel;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.PowerSourceBipole;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

public class SolarPannelElement extends TransparentNodeElement{

	SolarPannelDescriptor descriptor;
	NodeElectricalLoad positiveLoad = new NodeElectricalLoad("positiveLoad");
	NodeElectricalLoad negativeLoad = new NodeElectricalLoad("negativeLoad");
	VoltageSource positiveSrc = new VoltageSource("posSrc",positiveLoad, null);
	VoltageSource negativeSrc = new VoltageSource("negSrc",negativeLoad, null);
	
	//ElectricalCurrentSource currentSource;
	DiodeProcess diode;
	PowerSourceBipole powerSource;
	
	SolarPannelSlowProcess slowProcess = new SolarPannelSlowProcess(this);
	
	public double pannelAlpha = Math.PI/2;
	
	public SolarPannelElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (SolarPannelDescriptor) descriptor;

		grounded = false;
		
		/*if(this.descriptor.basicModel == false)
		{
			currentSource = new ElectricalCurrentSource(positiveLoad,negativeLoad);
			diode = new DiodeProcess(positiveLoad, negativeLoad);
			electricalProcessList.add(currentSource);
			electricalProcessList.add(diode);
		}
		else*/
		{
			powerSource = new PowerSourceBipole(positiveLoad, negativeLoad,positiveSrc,negativeSrc);
			
			
		}
		
		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		
		electricalComponentList.add(positiveSrc);
		electricalComponentList.add(negativeSrc);

		slowProcessList.add(slowProcess);
	}

	@Override
	public void connectJob() {
		Eln.simulator.mna.addProcess(powerSource);
		super.connectJob();
	}
	
	@Override
	public void disconnectJob() {	
		super.disconnectJob();
		Eln.simulator.mna.removeProcess(powerSource);
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(lrdu != LRDU.Down) return null;
		if(side == front.left()) return positiveLoad;
		if(side == front.right() && ! grounded) return negativeLoad;
		return null;	
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		/*if(lrdu != LRDU.Down) return null;
		if(side == front) return thermalLoad;
		if(side == front.getInverse() && ! grounded) return thermalLoad;*/
		return null;			
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(lrdu != LRDU.Down) return 0;
		if(side == front.left()) return node.maskElectricalPower;
		if(side == front.right() && ! grounded) return node.maskElectricalPower;
		return 0;		
	}

	@Override
	public String multiMeterString(Direction side) {
	//	if(side == front)return  Utils.plotVolt("U+", positiveLoad.Uc );
	//	if(side == front.back() && ! grounded)return  Utils.plotVolt("U-", negativeLoad.Uc );
		return  Utils.plotUIP(positiveLoad.getU()-negativeLoad.getU(), positiveLoad.getCurrent());
	}


	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		//return  Utils.plotCelsius("Tbat",thermalLoad.Tc);
		return "";
	}
	@Override
	public void initialize() {
		powerSource.setUmax(this.descriptor.electricalUmax);
		powerSource.setImax(this.descriptor.electricalPmax/this.descriptor.electricalUmax * 1.5);
		
		descriptor.applyTo(positiveLoad);
		descriptor.applyTo(negativeLoad);
		
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		powerSource.writeToNBT(nbt, "powerSource");
		nbt.setDouble("pannelAlpha", pannelAlpha);
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		powerSource.readFromNBT(nbt, "powerSource");
		pannelAlpha = nbt.getDouble("pannelAlpha");
	}



	public void networkSerialize(java.io.DataOutputStream stream)
	{
		super.networkSerialize(stream);
		try {	
			stream.writeBoolean(inventory.getStackInSlot(SolarPannelContainer.trackerSlotId) != null);
			stream.writeFloat((float) pannelAlpha);
			node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}	
	
	public static final byte unserializePannelAlpha = 0;
	public byte networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		byte packetType = super.networkUnserialize(stream);
		try {
			switch(packetType)
			{
			case unserializePannelAlpha:			
				pannelAlpha = stream.readFloat();
				needPublish();
				break;

			default:
				return packetType;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return unserializeNulldId;
	}
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1 , 64, this);
	
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		
		return descriptor.canRotate;
	}
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		
		return new SolarPannelContainer(node, player, inventory);
	}
	
}
