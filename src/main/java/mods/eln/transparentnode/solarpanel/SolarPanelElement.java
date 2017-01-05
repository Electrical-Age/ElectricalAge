package mods.eln.transparentnode.solarpanel;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.IPublishable;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.PowerSourceBipole;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SolarPanelElement extends TransparentNodeElement{

	SolarPanelDescriptor descriptor;
	NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
	NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
	VoltageSource positiveSrc = new VoltageSource("posSrc",positiveLoad, null);
	VoltageSource negativeSrc = new VoltageSource("negSrc",negativeLoad, null);
	
	//ElectricalCurrentSource currentSource;
	DiodeProcess diode;
	PowerSourceBipole powerSource;
	
	SolarPannelSlowProcess slowProcess = new SolarPannelSlowProcess(this);
	
	public double pannelAlpha = Math.PI/2;
	
	public SolarPanelElement(TransparentNode transparentNode,
							 TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (SolarPanelDescriptor) descriptor;

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
		if(lrdu != LRDU.Down) return null;
		if(side == front.left()) return positiveLoad;
		if(side == front.right() && ! grounded) return negativeLoad;
		return null;	
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;			
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return 0;
		if(side == front.left()) return NodeBase.maskElectricalPower;
		if(side == front.right() && ! grounded) return NodeBase.maskElectricalPower;
		return 0;		
	}

	@Override
	public String multiMeterString(Direction side) {
		return  Utils.plotUIP(positiveLoad.getU()-negativeLoad.getU(), positiveLoad.getCurrent());
	}


	@Override
	public String thermoMeterString(Direction side) {
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
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return descriptor.canRotate && inventory.take(entityPlayer.getCurrentEquippedItem(), (IPublishable) this);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		powerSource.writeToNBT(nbt, "powerSource");
		nbt.setDouble("pannelAlpha", pannelAlpha);
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		powerSource.readFromNBT(nbt, "powerSource");
		pannelAlpha = nbt.getDouble("pannelAlpha");
	}



	public void networkSerialize(java.io.DataOutputStream stream)
	{
		super.networkSerialize(stream);
		try {	
			stream.writeBoolean(getInventory().getStackInSlot(SolarPanelContainer.trackerSlotId) != null);
			stream.writeFloat((float) pannelAlpha);
			node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
		} catch (IOException e) {
			
			e.printStackTrace();
		}	
	}	
	
	public static final byte unserializePannelAlpha = 0;
	public byte networkUnserialize(DataInputStream stream) {
		
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
			
			e.printStackTrace();
		}
		return unserializeNulldId;
	}

	private final AutoAcceptInventoryProxy inventory =
		(new AutoAcceptInventoryProxy(new TransparentNodeElementInventory(1 , 64, this)))
		.acceptIfEmpty(0, SolarTrackerDescriptor.class);

	@Override
	public IInventory getInventory() {
		return inventory.getInventory();
	}
	
	@Override
	public boolean hasGui() {
		return descriptor.canRotate;
	}
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new SolarPanelContainer(node, player, inventory.getInventory());
	}

	@Override
	public Map<String, String> getWaila(){
		Map<String, String> info = new HashMap<String, String>();
		info.put(I18N.tr("Sun angle"), Utils.plotValue(((slowProcess.getSolarAlpha()) * (180/Math.PI)) - 90, "\u00B0"));
		info.put(I18N.tr("Panel angle"), Utils.plotValue((pannelAlpha * (180/Math.PI)) - 90, "\u00B0"));
		info.put(I18N.tr("Producing energy"), (slowProcess.getSolarLight() != 0 ? "Yes" : "No"));
		if (Eln.wailaEasyMode) {
			info.put(I18N.tr("Produced power"), Utils.plotPower("", powerSource.getP()));
		}
		return info;
	}
	
}
