package mods.eln.transparentnode.electricalantennarx;

import java.io.DataOutputStream;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalAntennaRxElement extends TransparentNodeElement {

	ElectricalAntennaRxSlowProcess slowProcess = new ElectricalAntennaRxSlowProcess(this);
	
	NbtElectricalLoad powerOut = new NbtElectricalLoad("powerOut");
	NbtElectricalGateInput signalIn = new NbtElectricalGateInput("signalIn");

	PowerSource powerSrc = new PowerSource("powerSrc", powerOut);

    LRDU rot = LRDU.Up;
    Coordonate rxCoord = null;
    ElectricalAntennaRxDescriptor descriptor;
    
	public double getSignal() {
		return signalIn.getBornedU();
	}
	
	public void setPowerOut(double power) {
		powerSrc.setP(power);
	}
	
	public void rxDisconnect() {
		powerSrc.setP(0.0);
	}

	public ElectricalAntennaRxElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		slowProcessList.add(slowProcess);
	
		electricalLoadList.add(powerOut);
		electricalLoadList.add(signalIn);		
		electricalComponentList.add(powerSrc);
		
		this.descriptor = (ElectricalAntennaRxDescriptor) descriptor;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if (front.getInverse() != side.applyLRDU(lrdu)) return null;
		
		if (side == front.applyLRDU(rot.left())) return powerOut;
		if (side == front.applyLRDU(rot.right())) return signalIn;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if (front.getInverse() != side.applyLRDU(lrdu)) return 0;
		
		if (side == front.applyLRDU(rot.left())) return NodeBase.maskElectricalPower;
		if (side == front.applyLRDU(rot.right())) return NodeBase.maskElectricalInputGate;
		
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		return "";
	}

	@Override
	public String thermoMeterString(Direction side) {
		return "";
	}

	@Override
	public void initialize() {
		descriptor.cable.applyTo(powerOut);
		powerSrc.setUmax(descriptor.electricalMaximalVoltage * 2);
		powerSrc.setImax(descriptor.electricalMaximalVoltage * descriptor.electricalMaximalPower * 2);
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			rot = rot.getNextClockwise();
			node.reconnect();
			return true;	
		}
		return false;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		rot = LRDU.readFromNBT(nbt, "rot");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		rot.writeToNBT(nbt,"rot");
	}
	
	public boolean mustHaveFloor() {
		return false;
	}
	
	public boolean mustHaveCeiling() {
		return false;
	}
	
	public boolean mustHaveWall() {
		return false;
	}
	
	public boolean mustHaveWallFrontInverse() {
		return true;
	}
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		rot.serialize(stream);		
		node.lrduCubeMask.getTranslate(front.getInverse()).serialize(stream);
	}
}
