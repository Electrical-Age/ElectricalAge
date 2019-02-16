package mods.eln.sim;

public class ThermalLoad {

    /**
     * Current temperature, in celsius.
     */
    public double Tc;
    public double Rp;
    /**
     * Thermal resistance, analogous to ohms.
     */
    public double Rs;
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

    public static final ThermalLoad externalLoad = new ThermalLoad(0, 0, 0, 0);

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
