package mods.eln.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ITimeRemoverObserver;
import mods.eln.sim.TimeRemover;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableElement;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.DamageSource;

public class ReplicatoCableAI extends EntityAIBase implements ITimeRemoverObserver {

	ReplicatorEntity entity;

	public Coordonate cableCoordonate = null;
	Random rand = new Random();
	int lookingPerUpdate = 20;

	ElectricalLoad load = new ElectricalLoad(),cableLoad;
	Resistor resistorLoad = new Resistor(load,null);
	ElectricalConnection connection;
	TimeRemover timeRemover = new TimeRemover(this);

	double moveTimeOut;
	double moveTimeOutReset = 20;
	double resetTimeout;
	double resetTimeoutReset = 120;

	PreSimCheck preSimCheck;

	public ReplicatoCableAI(ReplicatorEntity entity) {
		load.setAsPrivate();
		this.entity = entity;
		Eln.instance.highVoltageCableDescriptor.applyTo(load);
		load.setRs(load.getRs()*10);
		this.setMutexBits(1);
	}

	@Override
	public boolean shouldExecute() {
		//Utils.println("LookingForCableAi");
		List<NodeBase> nodes = NodeManager.instance.getNodes();
		if(nodes.isEmpty()) return false;
		for(int idx = 0; idx < lookingPerUpdate; idx++) {
			NodeBase node = nodes.get(rand.nextInt(nodes.size()));
			double distance = node.coordonate.distanceTo(entity);

			if(distance > 15) continue;

			if(node instanceof SixNode == false) continue;

			SixNode sixNode = (SixNode)node;

			for(SixNodeElement e : sixNode.sideElementList) {
				if(e == null) continue;

				if(e instanceof ElectricalCableElement == false) continue;

				ElectricalCableElement cable = (ElectricalCableElement)e;

				if(isElectricalCableInterresting(cable) == false) continue;


				PathEntity path = entity.getNavigator().getPathToXYZ(node.coordonate.x, node.coordonate.y, node.coordonate.z);

				if(path == null/* || path.isFinished() == false*/) continue;

				entity.getNavigator().setPath(path, 1);
				cableCoordonate = node.coordonate;
				//Utils.println("LookingForCableAi done");
				moveTimeOut = moveTimeOutReset;
				resistorLoad.highImpedance();
				resetTimeout = resetTimeoutReset*(0.8 + Math.random()*0.4);
				return true;
			}
		}
		//ADD isElectricalCableInterresting !
		return false;
	}
	
	@Override
	public boolean continueExecuting() {
		//Utils.println("Continue");
		return cableCoordonate != null;
	}

	@Override
	public void updateTask() {
		//Utils.println("update");
		moveTimeOut -= 0.05;
		resetTimeout -= 0.05;
		ElectricalCableElement cable;

		if((cable = getCable()) == null) {
			cableCoordonate = null;
			return;
		}

		cableLoad = cable.electricalLoad;
		double distance = cableCoordonate.distanceTo(entity);

		if(distance > 2 && (entity.getNavigator().getPath() == null ||entity.getNavigator().getPath().isFinished())) {
			this.entity.getNavigator().tryMoveToXYZ(cableCoordonate.x, cableCoordonate.y, cableCoordonate.z, 1);
		}
		if(distance < 2) {
			//Utils.println("replicator on cable !");
			double u = cable.electricalLoad.getU();
			double nextRp = Math.pow(u / Eln.LVU, -0.3) * u * u / (50);
			if(resistorLoad.getR() < 0.8*nextRp) {
				entity.attackEntityFrom(DamageSource.magic, 5);
			} else {
				entity.eatElectricity(resistorLoad.getP() * 0.05);
			}

			resistorLoad.setR(nextRp);

			timeRemover.setTimeout(0.16);
			moveTimeOut = moveTimeOutReset;
		} else {
			resistorLoad.highImpedance();
		}

		if(moveTimeOut < 0 || resetTimeout < 0) {
			cableCoordonate = null;
		}
	}
	
	boolean isElectricalCableInterresting(ElectricalCableElement c) {
		if(c.descriptor.signalWire || c.electricalLoad.getU() < 30) {
			return false;
		}
		return true;
	}
	
	ElectricalCableElement getCable() {
		if(cableCoordonate == null) return null;

		NodeBase node = NodeManager.instance.getNodeFromCoordonate(cableCoordonate);

		if(node == null) return null;

		if(node instanceof SixNode) {
			SixNode sixNode = (SixNode)node;
			for(SixNodeElement e : sixNode.sideElementList) {
				if(e == null) continue;

				if(e instanceof ElectricalCableElement) {
					ElectricalCableElement cable = (ElectricalCableElement)e;
					if(isElectricalCableInterresting(cable)) return cable;
				}
			}
		}
		return null;
	}
	
	@Override
	public void startExecuting() {
		//Utils.println("START REPLICATOOOOOR");

		//Utils.println(this.entity.getNavigator().tryMoveToXYZ(-2470, 56, -50, 1));
	}

	@Override
	public void timeRemoverRemove() {
		Eln.simulator.removeElectricalLoad(load);
		Eln.simulator.removeElectricalComponent(connection);
		Eln.simulator.removeElectricalComponent(resistorLoad);
		Eln.simulator.removeSlowPreProcess(preSimCheck);
		connection = null;
	}

	@Override
	public void timeRemoverAdd() {
		Eln.simulator.addElectricalLoad(load);
		Eln.simulator.addElectricalComponent(connection = new ElectricalConnection(load, cableLoad));
		Eln.simulator.addElectricalComponent(resistorLoad);
		Eln.simulator.addSlowPreProcess(preSimCheck = new PreSimCheck());
	}
	
	class PreSimCheck implements IProcess {
		@Override
		public void process(double time) {
			if(timeRemover.isArmed() == false) return;
			if(Eln.simulator.isRegistred(cableLoad) == false) {
				timeRemover.shot();
			}
		}
	}
}
