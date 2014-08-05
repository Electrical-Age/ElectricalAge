package mods.eln.transparentnode.turret;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumSkyBlock;
import mods.eln.misc.Coordonate;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sound.SoundCommand;

public class TurretSlowProcess implements IProcess {
	
	public TurretSlowProcess(TurretElement element) {
		this.element = element;
	}

	private TurretElement element;

	private interface State {
		void enter();
		State process(double time);
		void leave();
	}
	
	private class SeekingState implements State {
		@Override
		public void enter() {
			element.setSeekMode(true);
			element.setTurretAngle(element.getDescriptor().getProperties().actionAngle);
		}

		@Override
		public State process(double time) {
			if (element.getTurretAngle() >= element.getDescriptor().getProperties().actionAngle)
				element.setTurretAngle(-element.getDescriptor().getProperties().actionAngle);
			else if (element.getTurretAngle() <= -element.getDescriptor().getProperties().actionAngle)
				element.setTurretAngle(element.getDescriptor().getProperties().actionAngle);
			
			Coordonate coord = element.coordonate();
			AxisAlignedBB bb = coord.getAxisAlignedBB((int)element.getDescriptor().getProperties().detectionDistance);
			List<EntityLivingBase> list = coord.world().getEntitiesWithinAABB(EntityLivingBase.class, bb);
			for (EntityLivingBase entity: list) {
				float dx = (float)(entity.posX - coord.x - 0.5);
				float dz = (float)(entity.posZ - coord.z - 0.5);
				float entityAngle = -(float)Math.toDegrees(Math.atan(dz / dx));
				
				if (Math.abs(entityAngle - element.getTurretAngle()) < 15) {
					ArrayList<Block> blockList = Utils.traceRay(coord.world(), coord.x + 0.5, coord.y + 0.5, coord.z + 0.5, 
																entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
					boolean visible = true;
					for (Block b: blockList)
						if (b.isOpaqueCube()) {
							visible = false;
							break;
						}
					
					if (visible && !coord.world().playerEntities.contains(entity)) {
							return new AimingState(entity);
					}
				}
			}
			
			return null;
		}

		@Override
		public void leave() {
			element.setSeekMode(false);
		}
	}
	
	private class AimingState implements State {
		public AimingState(EntityLivingBase target) {
			this.target = target;
		}

		private EntityLivingBase target;
		private float aimTime = 0;

		@Override
		public void enter() {
			element.setGunPosition(1);
		}

		@Override
		public State process(double time) {
			if (target.isDead) return new SeekingState();
			aimTime += time;
			
			Coordonate coord = element.coordonate();
			
			float dx = (float)(target.posX - coord.x - 0.5);
			float dy = (float)(target.posY + target.getEyeHeight() - coord.y - 0.75);
			float dz = (float)(target.posZ - coord.z - 0.5);
			float entityAngle = -(float)Math.toDegrees(Math.atan(dz / dx));
			float entityAngle2 = -(float)Math.toDegrees(Math.asin(dy / Math.sqrt(dx * dx + dz * dz)));
			
			if (Math.abs(entityAngle) > element.getDescriptor().getProperties().actionAngle) return new SeekingState();
			
			element.setTurretAngle(entityAngle);
			element.setGunElevation(-entityAngle2);
			
			if (Math.abs(target.posX - coord.x) > element.getDescriptor().getProperties().aimDistance || 
					Math.abs(target.posZ - coord.z) > element.getDescriptor().getProperties().aimDistance )
				return new SeekingState();
			
			ArrayList<Block> blockList = Utils.traceRay(coord.world(), coord.x + 0.5, coord.y + 0.5, coord.z + 0.5, 
					target.posX, target.posY + target.getEyeHeight(), target.posZ);
			for (Block b: blockList)
				if (b.isOpaqueCube()) 
					return new SeekingState();
			
			if (element.getGunPosition() == 1 && aimTime > 0.5f) return new ShootState(target);
			
			return this;
		}

		@Override
		public void leave() {
			element.setGunPosition(0);
			element.setGunElevation(0);
		}
	}
	
	class ShootState implements State {
		public ShootState(EntityLivingBase target) {
			this.target = target;
		}
		
		private EntityLivingBase target;
		
		@Override
		public void enter() {
			target.attackEntityFrom(DamageSource.generic, 5.0f);
			element.play(new SoundCommand("eln:LaserGun"));
		}

		@Override
		public State process(double time) {
			if (target.isDead)
				return new SeekingState();
			else
				return new AimingState(target);
		}

		@Override
		public void leave() {
		}
	}
	
	@Override
	public void process(double time) {
		if (state == null) {
			state = new SeekingState();
			state.enter();
		}
		
		State nextState = state.process(time);
		if (nextState != null && nextState != state) {
			state.leave();
			state = nextState;
			state.enter();
		}
	}	
	
	private State state = null;
}
