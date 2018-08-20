package mods.eln.sim;

import mods.eln.Eln;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;

public class ThermalLoadInitializer {

    public double warmLimit, coolLimit;
    double heatingTao;
    double conductionTao;

    double Rs, Rp, C;

    public ThermalLoadInitializer(double warmLimit, double coolLimit, double heatingTao, double conductionTao) {
        this.conductionTao = conductionTao;
        this.coolLimit = coolLimit;
        this.heatingTao = heatingTao;
        this.warmLimit = warmLimit;
    }

/*	public ThermalLoadInitializer (
            double warmLimit,double coolLimit,
			double heatingTao,double conductionTao,
			double P) {
		this.conductionTao = conductionTao;
		this.coolLimit = coolLimit;
		this.heatingTao = heatingTao;
		this.warmLimit = warmLimit;
		setMaximalPower(P);
	}*/

    public void setMaximalPower(double P) {
        C = P * heatingTao / (warmLimit);
        Rp = warmLimit / P;
        Rs = conductionTao / C / 2;

        Eln.simulator.checkThermalLoad(Rs, Rp, C);
    }

    public void applyTo(ThermalLoad load) {
        load.set(Rs, Rp, C);
    }

    public void applyTo(ThermalLoadWatchDog doggy) {
        doggy.set(this);
    }

    public ThermalLoadInitializer copy() {
        ThermalLoadInitializer thermalLoad = new ThermalLoadInitializer(warmLimit, coolLimit, heatingTao, conductionTao);
        thermalLoad.Rp = Rp;
        thermalLoad.Rs = Rs;
        thermalLoad.C = C;
        return thermalLoad;
    }
}
