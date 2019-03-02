package mods.eln.entity;

import mods.eln.Eln;
import mods.eln.init.Cable;
import mods.eln.misc.Coordinate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.*;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableElement;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.DamageSource;

import java.util.List;
import java.util.Random;

public class ReplicatorCableAI extends EntityAIBase implements ITimeRemoverObserver {

    ReplicatorEntity entity;

    private Coordinate cableCoordinate = null;
    private Random rand = new Random();
    private int lookingPerUpdate = 20;

    private ElectricalLoad load = new ElectricalLoad(), cableLoad;
    private Resistor resistorLoad = new Resistor(load, null);
    private ElectricalConnection connection;
    private TimeRemover timeRemover = new TimeRemover(this);

    private double moveTimeOut;
    private double moveTimeOutReset = 20;
    private double resetTimeout;
    private double resetTimeoutReset = 120;

    PreSimCheck preSimCheck;

    ReplicatorCableAI(ReplicatorEntity entity) {
        load.setAsPrivate();
        this.entity = entity;
        Cable.Companion.getHighVoltage().descriptor.applyTo(load);
        load.setRs(load.getRs() * 10);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        //Utils.println("LookingForCableAi");
        List<NodeBase> nodes = NodeManager.instance.getNodes();
        if (nodes.isEmpty()) return false;
        for (int idx = 0; idx < lookingPerUpdate; idx++) {
            NodeBase node = nodes.get(rand.nextInt(nodes.size()));
            double distance = node.coordinate.distanceTo(entity);

            if (distance > 15) continue;

            if (!(node instanceof SixNode)) continue;

            SixNode sixNode = (SixNode) node;

            for (SixNodeElement e : sixNode.sideElementList) {
                if (e == null) continue;

                if (!(e instanceof ElectricalCableElement)) continue;

                ElectricalCableElement cable = (ElectricalCableElement) e;

                if (!isElectricalCableInterresting(cable)) continue;


                Path path = entity.getNavigator().getPathToXYZ(node.coordinate.pos.getX(), node.coordinate.pos.getY(), node.coordinate.pos.getZ());

                if (path == null/* || path.isFinished() == false*/) continue;

                entity.getNavigator().setPath(path, 1);
                cableCoordinate = node.coordinate;
                //Utils.println("LookingForCableAi done");
                moveTimeOut = moveTimeOutReset;
                resistorLoad.highImpedance();
                resetTimeout = resetTimeoutReset * (0.8 + Math.random() * 0.4);
                return true;
            }
        }
        //ADD isElectricalCableInterresting !
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return cableCoordinate != null;
    }

    @Override
    public void updateTask() {
        //Utils.println("update");
        moveTimeOut -= 0.05;
        resetTimeout -= 0.05;
        ElectricalCableElement cable;

        if ((cable = getCable()) == null) {
            cableCoordinate = null;
            return;
        }

        cableLoad = cable.electricalLoad;
        double distance = cableCoordinate.distanceTo(entity);

        if (distance > 2 && (entity.getNavigator().getPath() == null || entity.getNavigator().getPath().isFinished())) {
            this.entity.getNavigator().tryMoveToXYZ(cableCoordinate.pos.getX(), cableCoordinate.pos.getY(), cableCoordinate.pos.getZ(), 1);
        }
        if (distance < 2) {
            //Utils.println("replicator on cable !");
            double u = cable.electricalLoad.getU();
            double nextRp = Math.pow(u / Cable.LVU, -0.3) * u * u / (50);
            if (resistorLoad.getR() < 0.8 * nextRp) {
                entity.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 5);
            } else {
                entity.eatElectricity(resistorLoad.getP() * 0.05);
            }

            resistorLoad.setR(nextRp);

            timeRemover.setTimeout(0.16);
            moveTimeOut = moveTimeOutReset;
        } else {
            resistorLoad.highImpedance();
        }

        if (moveTimeOut < 0 || resetTimeout < 0) {
            cableCoordinate = null;
        }
    }

    boolean isElectricalCableInterresting(ElectricalCableElement c) {
        if (c.descriptor.signalWire || c.electricalLoad.getU() < 30) {
            return false;
        }
        return true;
    }

    ElectricalCableElement getCable() {
        if (cableCoordinate == null) return null;

        NodeBase node = NodeManager.instance.getNodeFromCoordinate(cableCoordinate);

        if (node == null) return null;

        if (node instanceof SixNode) {
            SixNode sixNode = (SixNode) node;
            for (SixNodeElement e : sixNode.sideElementList) {
                if (e == null) continue;

                if (e instanceof ElectricalCableElement) {
                    ElectricalCableElement cable = (ElectricalCableElement) e;
                    if (isElectricalCableInterresting(cable)) return cable;
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
            if (!timeRemover.isArmed()) return;
            if (!Eln.simulator.isRegistred(cableLoad)) {
                timeRemover.shot();
            }
        }
    }
}
