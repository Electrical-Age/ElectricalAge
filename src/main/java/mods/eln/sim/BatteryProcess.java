package mods.eln.sim;

import mods.eln.misc.FunctionTable;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;

public class BatteryProcess implements IProcess {

    VoltageState positiveLoad, negativeLoad;
    public FunctionTable voltageFunction;
    public double Q = 0, QNominal = 0;
    public double uNominal = 0;
    public double life = 1.0;
    //public double efficiency = 1.0;

    public VoltageSource voltageSource;

    public boolean isRechargeable = true;
    public double IMax = 20;

    public BatteryProcess(VoltageState positiveLoad, VoltageState negativeLoad, FunctionTable voltageFunction, double IMax, VoltageSource voltageSource) {
        this.positiveLoad = positiveLoad;
        this.negativeLoad = negativeLoad;
        this.voltageFunction = voltageFunction;

        this.IMax = IMax;
        this.voltageSource = voltageSource;
    }

    @Override
    public void process(double time) {
//		Utils.print("*");
        Q -= voltageSource.getCurrent() * time;

        double voltage = computeVoltage();

        voltageSource.setU(voltage);

		/*double Cequ = (positiveLoad.getC() * negativeLoad.getC()) / (positiveLoad.getC() + negativeLoad.getC());
		double QNeeded = (voltage - (positiveLoad.Uc - negativeLoad.Uc)) * Cequ;
		//Q -= QNeeded > 0 ? QNeeded : QNeeded * efficiency;
		if (isRechargeable == true || QNeeded > 0) {
			double I = QNeeded / time;
			if (I > IMax) I = IMax;
			if (I < -IMax) I = -IMax;
			ElectricalLoad.moveCurrent(I, negativeLoad, positiveLoad);
			Q -= I * time;
			dischargeCurrentMesure = I;
		} else {
			dischargeCurrentMesure = 0;
		}*/
    }

    double computeVoltage() {
        double voltage = voltageFunction.getValue(Q / (QNominal * life));
        return Math.max(0, voltage * uNominal);
    }

    public double getQRatio() {
        return Q / QNominal;
    }

    public void changeLife(double newLife) {
        if (newLife < life) {
            this.Q *= newLife / life;
        }
        life = newLife;
    }

    public double getCharge() {
        return Q / (QNominal * life);
    }

    public void setCharge(double charge) {
        Q = QNominal * life * charge;
    }

    public double getEnergy() {
        int stepNbr = 50;
        double chargeStep = getCharge() / stepNbr;
        double chargeIntegrator = 0;
        double energy = 0;
        double QperStep = QNominal * life * chargeStep;

        for (int step = 0; step < stepNbr; step++) {
            double voltage = voltageFunction.getValue(chargeIntegrator) * uNominal;
            energy += voltage * QperStep;
            chargeIntegrator += chargeStep;
        }

        return energy;
		
		/*double probeU = (positiveLoad.Uc - negativeLoad.Uc);
		double q = 0, dq = 0.00001;
		double e = 0;
		double u;
		while ((u = voltageFunction.getValue(q) * uNominal) < probeU) {
			e += u * dq * QNominal;
			q += dq;
			
			//if (e > 1) break;
		}
		return e * life;		*/
    }

    public double getEnergyMax() {
        int stepNbr = 50;
        double chargeStep = 1.0 / stepNbr;
        double chargeIntegrator = 0;
        double energy = 0;
        double QperStep = QNominal * life * 1.0 / stepNbr;

        for (int step = 0; step < stepNbr; step++) {
            double voltage = voltageFunction.getValue(chargeIntegrator) * uNominal;
            energy += voltage * QperStep;
            chargeIntegrator += chargeStep;
        }

        return energy;
    }

    public double getU() {
        return computeVoltage();
    }

    public double getDischargeCurrent() {
        return voltageSource.getI();
    }
}
