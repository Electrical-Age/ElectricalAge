package mods.eln.entity;

import java.util.ArrayList;
import java.util.Random;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableElement;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ITimeRemoverObserver;
import mods.eln.sim.TimeRemover;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class ReplicatoCableAI extends EntityAIBase implements ITimeRemoverObserver{

	EntityLiving entity;
	public ReplicatoCableAI(EntityLiving entity) {
		this.entity = entity;
		Eln.instance.lowVoltageCableDescriptor.applyTo(load, false);
	}
	
	public Coordonate cableCoordonate = null;
	Random rand = new Random();
	int lookingPerUpdate = 10;	

	ElectricalLoad load = new ElectricalLoad(),cableLoad;
	ElectricalConnection connection;
	TimeRemover timeRemover = new TimeRemover(this);
	
	@Override
	public boolean shouldExecute() {
		// TODO Auto-generated method stub
		return true;
	}

	
	@Override
	public void updateTask() {
		if(cableCoordonate == null){
			System.out.println("LookingForCableAi");
			ArrayList<NodeBase> nodes = NodeManager.instance.getNodes();
			if(nodes.size() == 0) return;
			for(int idx = 0;idx < lookingPerUpdate;idx++){
				NodeBase node = nodes.get(rand.nextInt(nodes.size()));
				double distance = node.coordonate.distanceTo(entity);
				if(distance > 30) continue;
				if(node instanceof SixNode == false) continue;
				SixNode sixNode = (SixNode)node;
				for(SixNodeElement e : sixNode.sideElementList){
					if(e == null) continue;
					if(e instanceof ElectricalCableElement == false) continue;
					ElectricalCableElement cable = (ElectricalCableElement)e;
					if(this.entity.getNavigator().tryMoveToXYZ(node.coordonate.x,node.coordonate.y,node.coordonate.z, 1)){
						cableCoordonate = node.coordonate;
						System.out.println("LookingForCableAi done");
						return;
					}else{
						System.out.println("LookingForCableAi NO PATH");
					}
				}
			}
			return;
		}

		ElectricalCableElement cable;
		if((cable = getCable()) == null){
			cableCoordonate = null;
			return;
		}
		cableLoad = cable.electricalLoad;
		
		double distance = cableCoordonate.distanceTo(entity);
		if(distance > 4){
			this.entity.getNavigator().tryMoveToXYZ(cableCoordonate.x,cableCoordonate.y,cableCoordonate.z, 1);
		}else{
			//System.out.println("replicator on cable !");
			load.setRp(Math.pow(cable.descriptor.electricalNominalVoltage, 2)/(cable.descriptor.electricalNominalPower/10));
			timeRemover.setTimeout(0.16);
		}
		
	}
	
	ElectricalCableElement getCable(){
		if(cableCoordonate == null) return null;
		NodeBase node = NodeManager.instance.getNodeFromCoordonate(cableCoordonate);
		if(node == null) return null;
		if(node instanceof SixNode){
			SixNode sixNode = (SixNode)node;
			for(SixNodeElement e : sixNode.sideElementList){
				if(e == null) continue;
				if(e instanceof ElectricalCableElement){
					ElectricalCableElement cable = (ElectricalCableElement)e;
					return cable;
				}
			}
		}
		return null;
	}
	
	@Override
	public void startExecuting() {
		// TODO Auto-generated method stub
		System.out.println("START REPLICATOOOOOR");

		//System.out.println(this.entity.getNavigator().tryMoveToXYZ(-2470,56,-50, 1));
	}


	@Override
	public void timeRemoverRemove() {
		Eln.simulator.removeElectricalLoad(load);
		Eln.simulator.removeElectricalConnection(connection);
	}


	@Override
	public void timeRemoverAdd() {
		Eln.simulator.addElectricalLoad(load);
		Eln.simulator.addElectricalConnection(connection = new ElectricalConnection(load, cableLoad));
	}

}
