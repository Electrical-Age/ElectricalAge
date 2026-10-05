package mods.eln.sim;

public class ThermalLoad {

    /**
     * Current temperature as a difference to ambient, in K (= degrees C above PhysicalConstant.Tamb, 20 C).
     * The thermal step relaxes it to 0 through Rp; GUIs that show absolute temperatures add Tamb.
     */
    public double Tc;
    /** Thermal resistance to ambient, K/W. */
    public double Rp;
    /**
     * Thermal resistance (K/W) of this load's half of a ThermalConnection (a connection conducts dT/(Rs1+Rs2)).
     */
    public double Rs;
    /** Heat capacity, J/K. */
    public double C;
    /**
     * Current thermal power, in watts, of this load.
     * This will be negative if it's cooling down.
     */
    public double Pc;
    /**
     * Current resistive loss, in watts.
     */
    public double Prs;
    public double Psp;

    /**
     * Absolute heat transfer in this simulator tick.
     */
    public double PrsTemp = 0;
    /**
     * Heat power transferred during this simulator tick.
     */
    public double PspTemp = 0;
    /**
     * Relative heat transfer during this simulator tick.
     */
    public double PcTemp;

    boolean isSlow;

    public ThermalLoad() {
        setHighImpedance();
        Tc = 0;
        PcTemp = 0;
        Pc = 0;
        Prs = 0;
        Psp = 0;
    }

    public ThermalLoad(double Tc, double Rp, double Rs, double C) {
        this.Tc = Tc;
        this.Rp = Rp;
        this.Rs = Rs;
        this.C = C;
        PcTemp = 0;
    }

    public void setRsByTao(double tao) {
        Rs = tao / C;
    }

    public void setHighImpedance() {
        Rs = 1000000000.0;
        C = 1;
        Rp = 1000000000.0;
    }

    /**
     * Ambient as an infinite heat reservoir (other end of e.g. the electrical furnace's ThermalResistor): Tc stays 0
     * whatever flows in. 1.7.10 had Rp = 0 and C = 0, so Tc/Rp (getPower, or the thermal step if it was ever put on
     * a load list) was NaN.
     */
    public static final ThermalLoad externalLoad = new ThermalLoad(0, Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY);

    public void setRp(double Rp) {
        this.Rp = Rp;
    }

    public double getPower() {
        return (Prs + Math.abs(Pc) + Tc / Rp + Psp) / 2;
    }

    public void set(double Rs, double Rp, double C) {
        this.Rp = Rp;
        this.Rs = Rs;
        this.C = C;
    }

    public static void moveEnergy(double energy, double time, ThermalLoad from, ThermalLoad to) {
        double I = energy / time;
        double absI = Math.abs(I);
        from.PcTemp -= I;
        to.PcTemp += I;
        from.PspTemp += absI;
        to.PspTemp += absI;
    }

    public static void movePower(double power, ThermalLoad from, ThermalLoad to) {
        double absI = Math.abs(power);
        from.PcTemp -= power;
        to.PcTemp += power;
        from.PspTemp += absI;
        to.PspTemp += absI;
    }

    public void movePowerTo(double power) {
        double absI = Math.abs(power);
        PcTemp += power;
        PspTemp += absI;
    }

    public double getT() {
        return Tc;
    }

    public boolean isSlow() {
        return isSlow;
    }

    public void setAsSlow() {
        isSlow = true;
    }

    public void setAsFast() {
        isSlow = false;
    }
}
