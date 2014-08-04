package mods.eln.transparentnode.turret;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

public class TurretRender extends TransparentNodeElementRender {

	public TurretDescriptor descriptor;
	public TurretMechanicsSimulation simulation;

	public TurretRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (TurretDescriptor)descriptor;
		simulation = new TurretMechanicsSimulation(this.descriptor);
	}

	public float getTurretAngle() {
		return simulation.getTurretAngle();
	}
	
	public float getGunPosition() {
		return simulation.getGunPosition();
	}
	
	public float getGunElevation() {
		return simulation.getGunElevation();
	}
	
	@Override
	public void draw() {
		descriptor.draw(this);
	}
	
	@Override
	public void refresh(float deltaT) {
		super.refresh(deltaT);
		simulation.process(deltaT);
	}

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			simulation.setTurretAngle(stream.readFloat());
			simulation.setGunPosition(stream.readFloat());
			simulation.setGunElevation(stream.readFloat());
			simulation.setSeekMode(stream.readBoolean());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean cameraDrawOptimisation() {
		return true;
	}
}
