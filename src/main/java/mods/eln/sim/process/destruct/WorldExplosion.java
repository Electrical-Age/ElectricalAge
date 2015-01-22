package mods.eln.sim.process.destruct;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;

public class WorldExplosion implements IDestructable{
	Object origine;
	
	public WorldExplosion(Coordonate c) {
		this.c = c;
	}
	
	public WorldExplosion(SixNodeElement e) {
		this.c = e.getCoordonate();
		origine = e;
	}
	
	public WorldExplosion(TransparentNodeElement e) {
		this.c = e.coordonate();
		origine = e;
	}
	
	
	public WorldExplosion(EnergyConverterElnToOtherNode e) {
		this.c = e.coordonate;
		origine = e;
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

		//NodeManager.instance.removeNode(NodeManager.instance.getNodeFromCoordonate(c));
		
		if(Eln.instance.explosionEnable)
			c.world().createExplosion((Entity)null, c.x,c.y,c.z, strength, true);
		else
			c.world().setBlock(c.x, c.y, c.z, Blocks.air);
			
	}

}
