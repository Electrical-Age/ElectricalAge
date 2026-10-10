package mods.eln.sim;

public class ThermalResistor implements IProcess {

    ThermalLoad a, b;

    protected double R, Rinv;
    //double P = 0;

    public ThermalResistor(ThermalLoad a, ThermalLoad b) {
        this.a = a;
        this.b = b;
        highImpedance();
    }

    @Override
    public void process(double time) {
        double P = (a.Tc - b.Tc) * Rinv;
        a.PcTemp -= P;
        b.PcTemp += P;
    }

    public double getP() {
        return (a.Tc - b.Tc) * Rinv;
    }

    public void setR(double r) {
        R = r;
        Rinv = 1 / r;
    }

    public double getR() {
        return R;
    }
    /*
	public double getU() {
		return P * R;
	}*/

    public void highImpedance() {
        setR(1000000000.0);
    }
}
