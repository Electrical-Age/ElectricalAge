package mods.eln.sim;

import mods.eln.sim.mna.component.Bipole;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Line;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageStateLineReady;

public class ElectricalLoad extends VoltageStateLineReady {

    public static final State groundLoad = null;

    private double Rs = MnaConst.highImpedance;

    public ElectricalLoad() {
    }

    //public VoltageState state = new VoltageState();

    public void setRs(double Rs) {
        if (this.Rs != Rs) {
            this.Rs = Rs;
            for (Component c : getConnectedComponents()) {
                if (c instanceof ElectricalConnection) {
                    ((ElectricalConnection) c).notifyRsChange();
                }
            }
        }
    }

    public double getRs() {
        return Rs;
    }

    public void highImpedance() {
        setRs(MnaConst.highImpedance);
    }
//	ArrayList<ElectricalConnection> electricalConnections = new ArrayList<ElectricalConnection>(4);

    public double getI() {
        double i = 0;
        for (Component c : getConnectedComponents()) {
            if (c instanceof Bipole && (!(c instanceof Line)))
                i += Math.abs(((Bipole) c).getCurrent());
        }
        return i * 0.5;
    }

    public double getCurrent() {
        return getI();
    }
}
