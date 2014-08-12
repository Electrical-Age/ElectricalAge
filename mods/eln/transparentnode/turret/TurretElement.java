package mods.eln.transparentnode.turret;

import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class TurretElement extends TransparentNodeElement {
	
	private TurretDescriptor descriptor;
	
	private TurretMechanicsSimulation simulation;
	
	public TurretElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (TurretDescriptor)descriptor;
		slowProcessList.add(new TurretSlowProcess(this));
		simulation = new TurretMechanicsSimulation((TurretDescriptor)descriptor);
		slowProcessList.add(simulation);
	}
	
	public TurretDescriptor getDescriptor() {
		return descriptor;
	}

	public float getTurretAngle() {
		return simulation.getTurretAngle();
	}
	
	public void setTurretAngle(float angle) {
		if (simulation.setTurretAngle(angle)) needPublish();
	}
	
	public float getGunPosition() {
		return simulation.getGunPosition();
	}
	
	public void setGunPosition(float position) {
		if (simulation.setGunPosition(position)) needPublish();
	}
	
	public float getGunElevation() {
		return simulation.getGunPosition();
	}
	
	public void setGunElevation(float elevation) {
		if (simulation.setGunElevation(elevation)) needPublish();
	}
	
	public void setSeekMode(boolean seekModeEnabled) {
		if (seekModeEnabled != simulation.inSeekMode()) needPublish();
		simulation.setSeekMode(seekModeEnabled);
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
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
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		return false;
	}
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
    	try {
    		stream.writeFloat(simulation.getTurretTargetAngle());
    		stream.writeFloat(simulation.getGunTargetPosition());
    		stream.writeFloat(simulation.getGunTargetElevation());
    		stream.writeBoolean(simulation.inSeekMode());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
