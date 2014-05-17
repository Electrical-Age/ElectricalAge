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
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.DamageSource;

public class ReplicatoCableAI extends EntityAIBase implements ITimeRemoverObserver{

	ReplicatorEntity entity;
	public ReplicatoCableAI(ReplicatorEntity entity) {
		this.entity = entity;
		Eln.instance.highVoltageCableDescriptor.applyTo(load, false);
		load.setRs(load.getRs()*10);
		 this.setMutexBits(1);
	}
	
	public Coordonate cableCoordonate = null;
	Random rand = new Random();
	int lookingPerUpdate = 20;	

	ElectricalLoad load = new ElectricalLoad(),cableLoad;
	ElectricalConnection connection;
	TimeRemover timeRemover = new TimeRemover(this);
	double moveTimeOut;
	double moveTimeOutReset = 20;
	double resetTimeout;
	double resetTimeoutReset = 120;
	@Override
	public boolean shouldExecute() {
		//System.out.println("LookingForCableAi");
		ArrayList<NodeBase> nodes = NodeManager.instance.getNodes();
		if(nodes.size() == 0) return false;
		for(int idx = 0;idx < lookingPerUpdate;idx++){
			NodeBase node = nodes.get(rand.nextInt(nodes.size()));
			double distance = node.coordonate.distanceTo(entity);
			if(distance > 15) continue;
			if(node instanceof SixNode == false) continue;
			SixNode sixNode = (SixNode)node;
			for(SixNodeElement e : sixNode.sideElementList){
				if(e == null) continue;
				if(e instanceof ElectricalCableElement == false) continue;
				ElectricalCableElement cable = (ElectricalCableElement)e;
				if(isElectricalCableInterresting(cable) == false) continue;
				PathEntity path = entity.getNavigator().getPathToXYZ(node.coordonate.x,node.coordonate.y,node.coordonate.z);
				if(path == null/* || path.isFinished() == false*/) continue;
				entity.getNavigator().setPath(path, 1);
				cableCoordonate = node.coordonate;
				//System.out.println("LookingForCableAi done");
				moveTimeOut = moveTimeOutReset;
				load.setRp(1000000000000.0);
				resetTimeout = resetTimeoutReset*(0.8 + Math.random()*0.4);
				return true;
			}
		}
		//ADD isElectricalCableInterresting !
		return false;
	}
	
	@Override
	public boolean continueExecuting() {
		// TODO Auto-generated method stub
		//System.out.println("Continue");
		return cableCoordonate != null;
	}


	@Override
	public void updateTask() {
		//System.out.println("update");
		moveTimeOut -= 0.05;
		resetTimeout -= 0.05;
		ElectricalCableElement cable;
		if((cable = getCable()) == null){
			cableCoordonate = null;
			return;
		}
		cableLoad = cable.electricalLoad;
		
		double distance = cableCoordonate.distanceTo(entity);
		if(distance > 2 && (entity.getNavigator().getPath() == null ||entity.getNavigator().getPath().isFinished())){
			this.entity.getNavigator().tryMoveToXYZ(cableCoordonate.x,cableCoordonate.y,cableCoordonate.z, 1);
		}
		if(distance < 2){
			//System.out.println("replicator on cable !");
			double u = cable.electricalLoad.Uc;
			double nextRp = Math.pow(u/Eln.LVU, -0.3)*u*u/(50);
			if(load.getRp() < 0.8*nextRp){
				entity.attackEntityFrom(DamageSource.magic, 5);
			}
			else{
				entity.eatElectricity(load.getRpPower()*0.05);
			}
			load.setRp(nextRp);

			timeRemover.setTimeout(0.16);
			moveTimeOut = moveTimeOutReset;

		}
		else{
			load.setRp(1000000000000.0);
		}
		
		
		if(moveTimeOut < 0 || resetTimeout < 0){
			cableCoordonate = null;
		}
		
	}
	
	boolean isElectricalCableInterresting(ElectricalCableElement c){
		if(c.descriptor.signalWire || c.electricalLoad.Uc < 30){
			return false;
		}
		return true;
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
					if(isElectricalCableInterresting(cable))
						return cable;
				}
			}
		}
		return null;
	}
	
	@Override
	public void startExecuting() {
		// TODO Auto-generated method stub
		//System.out.println("START REPLICATOOOOOR");

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
