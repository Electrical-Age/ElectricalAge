package mods.eln.sim.process.destruct;

import net.minecraft.entity.Entity;
import mods.eln.misc.Coordonate;
import mods.eln.node.SixNodeElement;
import mods.eln.node.TransparentNodeElement;

public class WorldExplosion implements IDestructable{

	public WorldExplosion(Coordonate c) {
		this.c = c;
	}
	
	public WorldExplosion(SixNodeElement e) {
		this.c = e.getCoordonate();
	}
	
	public WorldExplosion(TransparentNodeElement e) {
		this.c = e.coordonate();
		this.strength = strength;
	}
	
	
	public WorldExplosion cableExplosion(){
		strength = 1.5f;
		return this;
	}

	public WorldExplosion machineExplosion(){
		strength = 3;
		return this;
	}

	
	Coordonate c;
	float strength;
	
	@Override
	public void destructImpl() {
		c.world().createExplosion((Entity)null, c.x,c.y,c.z, strength, true);
	}

}
