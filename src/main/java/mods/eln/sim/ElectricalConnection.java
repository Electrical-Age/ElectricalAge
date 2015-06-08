package mods.eln.sim;

import mods.eln.sim.mna.component.InterSystem;
import mods.eln.sim.mna.primitives.Resistance;

public class ElectricalConnection extends InterSystem {

	ElectricalLoad L1, L2;

	public ElectricalConnection(ElectricalLoad L1, ElectricalLoad L2) {
		this.L1 = L1;
		this.L2 = L2;
	}

	public void notifyRsChange() {
		double R = ((ElectricalLoad) aPin).getRs() + ((ElectricalLoad) bPin).getRs();
		setR(new Resistance(R));
	}

	@Override
	public void onAddToRootSystem() {
		this.connectTo(L1, L2);
	/*	((ElectricalLoad) aPin).electricalConnections.add(this);
		((ElectricalLoad) bPin).electricalConnections.add(this);*/
		notifyRsChange();
	}
	
	@Override
	public void onRemovefromRootSystem() {
		this.breakConnection();
	/*	((ElectricalLoad) aPin).electricalConnections.remove(this);
		((ElectricalLoad) bPin).electricalConnections.remove(this);*/
	}
}
