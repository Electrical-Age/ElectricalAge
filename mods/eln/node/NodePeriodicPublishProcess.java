package mods.eln.node;

import mods.eln.sim.IProcess;

public class NodePeriodicPublishProcess implements IProcess{
	Node node;
	public NodePeriodicPublishProcess(Node node,double base,double random) {
		this.node = node;
		this.base = base;
		this.random = random;
	}
	double counter = 0,random,base;
	@Override
	public void process(double time) {
		counter -= time;
		if(counter <= 0.0)
		{
			counter += base + Math.random()*random;
			node.setNeedPublish(true);
		}
	}

}
