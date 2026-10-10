package mods.eln.sim;

public class ThermalConnection {

    public ThermalLoad L1;
    public ThermalLoad L2;

    public ThermalConnection(ThermalLoad L1, ThermalLoad L2) {
        this.L1 = L1;
        this.L2 = L2;
    }
}
