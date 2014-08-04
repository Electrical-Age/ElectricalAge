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

public class TurretSlowProcess implements IProcess {

	private TurretElement element;
	
	enum State {
		Seek,
		Aim,
		Attack
	};
	
	private State state = State.Seek;
	private EntityLivingBase target;
	
	public EntityLivingBase seek(double time) {
		if (element.getTurretAngle() >= element.getDescriptor().getProperties().actionAngle)
			element.setTurretAngle(-element.getDescriptor().getProperties().actionAngle);
		else if (element.getTurretAngle() <= -element.getDescriptor().getProperties().actionAngle)
			element.setTurretAngle(element.getDescriptor().getProperties().actionAngle);
		element.setGunElevation(0);
		element.setGunPosition(0);
		// Do not check each pass! Uses a lot of resources!!!
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
				
				if (visible) {
						return entity;
				}
			}
		}
		
		return null;
	}
	
	public boolean aim(double time) {
		if (target.isDead) return false;
		element.setSeekMode(false);
		Coordonate coord = element.coordonate();

		element.setGunPosition(1);
		
		float dx = (float)(target.posX - coord.x - 0.5);
		float dy = (float)(target.posY + target.getEyeHeight() - coord.y - 0.75);
		float dz = (float)(target.posZ - coord.z - 0.5);
		float entityAngle = -(float)Math.toDegrees(Math.atan(dz / dx));
		float entityAngle2 = -(float)Math.toDegrees(Math.asin(dy / Math.sqrt(dx * dx + dz * dz)));
		
		element.setTurretAngle(entityAngle);
		element.setGunElevation(-entityAngle2);
		
		/*if (Math.abs(target.posX - coord.x) > element.getDescriptor().getDetectionDistance() + 1 || 
				Math.abs(target.posZ - coord.z) > element.getDescriptor().getDetectionDistance() + 1 )
			return false;*/
		
		ArrayList<Block> blockList = Utils.traceRay(coord.world(), coord.x + 0.5, coord.y + 0.5, coord.z + 0.5, 
				target.posX, target.posY + target.getEyeHeight(), target.posZ);
		for (Block b: blockList)
			if (b.isOpaqueCube()) 
				return false;
		
		return true;
	}
	
	public TurretSlowProcess(TurretElement element) {
		this.element = element;
	}
	
	@Override
	public void process(double time) {
		switch (state) {
			case Seek:
				target = seek(time);
				if (target != null) state = State.Aim;
				break;
				
			case Aim:
				if (!aim(time)) {
					state = State.Seek;
					target = null;
				}
				break;
		}
	}

}
