package mods.eln.transparentnode.turret;

import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.transparent.TransparentNodeDescriptor;

public class TurretDescriptor extends TransparentNodeDescriptor {
	class Properties {
		public float actionAngle;
		public float detectionDistance;
		public float aimDistance;
		public float impulseEnergy;
		public float gunMinElevation;
		public float gunMaxElevation;
		public float turretSeekAnimationSpeed;
		public float turretAimAnimationSpeed;
		public float gunArmAnimationSpeed;
		public float gunDisarmAnimationSpeed;
		public float gunAimAnimationSpeed;
		public double minimalVoltage;
        public double maximalVoltage;
		public double basePower;
		public double aimingPower;
        public double chargeCurrent;
		
		public Properties() {
			actionAngle = 70;
			detectionDistance = 12;
			aimDistance = 15;
			impulseEnergy = 2000;
			gunMinElevation = -40;
			gunMaxElevation = 70;
			turretSeekAnimationSpeed = 40;
			turretAimAnimationSpeed = 70;
			gunArmAnimationSpeed = 3;
			gunDisarmAnimationSpeed = 0.5f;
			gunAimAnimationSpeed = 100;
			minimalVoltage = 180;
            maximalVoltage = 3300;
			basePower = 50;
			aimingPower = 250;
            chargeCurrent = 2;
		}
	}
		
	private Obj3D obj;
	private Obj3DPart turret, holder, joint, leftGun, rightGun, sensor, fire;
	
	private Properties properties;
	
	public TurretDescriptor(String name, String modelName, String description) {
		super(name, TurretElement.class, TurretRender.class);
		
		obj = Eln.obj.getObj(modelName);
		turret = obj.getPart("Turret");
		holder = obj.getPart("Holder");
		joint = obj.getPart("Joint");
		leftGun = obj.getPart("LeftGun");
		rightGun = obj.getPart("RightGun");
		sensor = obj.getPart("Sensor");
		fire = obj.getPart("Fire");
		
		properties = new Properties();
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw(null);
	}
	
	public void draw(TurretRender render) {
		float turretAngle = render != null ? render.getTurretAngle() : 0;
		float gunPosition = render != null ? render.getGunPosition() : 0;
		float gunAngle = render != null ? -render.getGunElevation() : 0;
		boolean shooting = render != null ? render.isShooting() : false;
		boolean enabled = render != null ? render.isEnabled() : true;

        if (holder != null) holder.draw();
		if (joint != null) joint.draw();
		GL11.glPushMatrix();
			GL11.glRotatef(turretAngle, 0f, 1f, 0f);
			if (turret != null) turret.draw();
			if (sensor != null) {
				if (enabled) {
					if (render != null && render.filter != null )
                        render.filter.glColor(0.5f + 0.5f * gunPosition);
                    else
                        GL11.glColor3f(0.5f, 0.5f, 0.5f);
					UtilsClient.drawLight(sensor);
					GL11.glColor3f(1f, 1f, 1f);
				} else {
                    GL11.glColor3f(0.5f, 0.5f, 0.5f);
					sensor.draw();
				}
			}
			GL11.glRotatef(gunAngle, 0f, 0f, 1f);
			
			GL11.glColor4f(.6f, .8f, 1f, .6f);
			if (shooting && fire != null) UtilsClient.drawLight(fire);
			GL11.glColor4f(1f, 1f, 1f, 1f);
			
			GL11.glTranslatef(0f, 0f, gunPosition / 4f);
			if (leftGun != null) leftGun.draw();
			GL11.glTranslatef(0f, 0f, -gunPosition / 2f);
			if (rightGun != null) rightGun.draw();
		GL11.glPopMatrix();
	}
}
