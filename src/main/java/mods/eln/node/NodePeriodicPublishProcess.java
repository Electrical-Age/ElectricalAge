package mods.eln.node;

import mods.eln.sim.IProcess;

public class NodePeriodicPublishProcess implements IProcess {
    NodeBase node;
    private double counter = 0, random, base;

    public NodePeriodicPublishProcess(NodeBase node, double base, double random) {
        this.node = node;
        this.base = base;
        this.random = random;
    }

    @Override
    public void process(double time) {
        counter -= time;
        if (counter <= 0.0) {
            counter += base + Math.random() * random;
            node.setNeedPublish(true);
        }
    }

    public void reconfigure(double base, double random) {
        this.base = base;
        this.random = random;
        counter = 0;
    }
}
