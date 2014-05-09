package mods.eln.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import com.google.common.collect.Multiset.Entry;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableElement;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeElement;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class LookingForCableAi extends EntityAIBase{

	EntityLiving entity;
	public LookingForCableAi(EntityLiving entity) {
		this.entity = entity;
	}
	
	public Coordonate cableCoordonate = null;
	@Override
	public boolean shouldExecute() {
		// TODO Auto-generated method stub
		return cableCoordonate == null;
	}
	
	Random rand = new Random();
	int lookingPerUpdate = 10;
	@Override
	public void updateTask() {
		System.out.println("LookingForCableAi");
		ArrayList<NodeBase> nodes = NodeManager.instance.getNodes();
		if(nodes.size() == 0) return;
		for(int idx = 0;idx < lookingPerUpdate;idx++){
			NodeBase node = nodes.get(rand.nextInt(nodes.size()));
			if(node instanceof SixNode){
				SixNode sixNode = (SixNode)node;
				for(SixNodeElement e : sixNode.sideElementList){
					if(e == null) continue;
					if(e instanceof ElectricalCableElement){
						ElectricalCableElement cable = (ElectricalCableElement)e;
						cableCoordonate = node.coordonate;
						System.out.println("LookingForCableAi done");
						return;
					}
				}
			}
		}
	}
	
	@Override
	public void startExecuting() {
		System.out.println("LookingForCableAi start");
	}

}
