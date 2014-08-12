package mods.eln.transparentnode.turret;

import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.SlewLimiter;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;

public class TurretMechanicsSimulation implements IProcess {
	private SlewLimiter turretAngle;
	private SlewLimiter gunPosition;
	private SlewLimiter gunElevation;
	
	private TurretDescriptor descriptor;
	
	public TurretMechanicsSimulation(TurretDescriptor descriptor) {
		this.descriptor = descriptor;
		turretAngle = new SlewLimiter(descriptor.getProperties().turretAimAnimationSpeed);
		gunPosition = new SlewLimiter(descriptor.getProperties().gunArmAnimationSpeed, descriptor.getProperties().gunDisarmAnimationSpeed);
		gunElevation = new SlewLimiter(descriptor.getProperties().gunAimAnimationSpeed);
	}

	public float getTurretAngle() {
		return turretAngle.getPosition();
	}
	
	public float getTurretTargetAngle() {
		return turretAngle.getTarget();
	}
	
	public boolean setTurretAngle(float angle) {
		angle = Utils.limit(angle, -descriptor.getProperties().actionAngle, descriptor.getProperties().actionAngle);
		boolean changed = angle != turretAngle.getTarget();
		turretAngle.setTarget(angle);
		return changed;
	}
	
	public float getGunPosition() {
		return gunPosition.getPosition();
	}
	
	public float getGunTargetPosition() {
		return gunPosition.getTarget();
	}
	
	public boolean setGunPosition(float position) {
		position = Utils.limit(position, 0, 1);
		boolean changed = position != gunPosition.getTarget();
		gunPosition.setTarget(position);
		return changed;
	}
	
	public float getGunElevation() {
		return gunElevation.getPosition();
	}
	
	public float getGunTargetElevation() {
		return gunElevation.getTarget();
	}
	
	public boolean setGunElevation(float elevation) {
		elevation = Utils.limit(elevation, descriptor.getProperties().gunMinElevation, descriptor.getProperties().gunMaxElevation);
		boolean changed = elevation != gunElevation.getTarget();
		gunElevation.setTarget(elevation);
		return changed;
	}

	public boolean inSeekMode() {
		return turretAngle.getPositiveSlewRate() == descriptor.getProperties().turretSeekAnimationSpeed;
	}
	
	public void setSeekMode(boolean seekModeEnabled) {
		if (seekModeEnabled)
			turretAngle.setSlewRate(descriptor.getProperties().turretSeekAnimationSpeed);
		else
			turretAngle.setSlewRate(descriptor.getProperties().turretAimAnimationSpeed);
	}
	
	@Override
	public void process(double time) {
		turretAngle.step((float)time);
		if (gunElevation.getPosition() == 0) gunPosition.step((float)time);
		if (gunPosition.getPosition() == 1) gunElevation.step((float)time);
	}
}
