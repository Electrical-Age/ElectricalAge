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

    /**
     * A lead-acid cell's EMF per unit of its nominal 2 V, by charge (Baughn 2026-11-22: a more accurate lead-acid curve,
     * float and absorption included). Resting it runs nearly straight from 1.95 V at 10 % to 2.12 V at 92 % (a 48 V
     * battery reads 48 V near 30 %); over that it takes charge only at a rising voltage (absorption), to 2.40 V at full;
     * held at the float voltage, 2.25 V, it settles near 97 % and takes almost nothing. Past full, charge no longer
     * stores: it gasses, as heat. Under 10 % it falls away, to nothing at empty.
     */
    public static final double[][] LEAD_ACID = {{0, 0}, {0.05, 0.875}, {0.1, 0.975}, {0.92, 1.06}, {0.96, 1.10},
        {0.975, 1.125}, {1.0, 1.20}, {1.2, 1.20}};
    /**
     * Per unit: the float voltage, and the absorption voltage, a full cell's EMF: over it charge only gasses (the
     * overvoltage chip stops charging there).
     */
    public static final double FLOAT = 1.125, ABSORPTION = 1.20;

    public BatteryProcess(VoltageState positiveLoad, VoltageState negativeLoad, FunctionTable voltageFunction, VoltageSource voltageSource) {
        this.positiveLoad = positiveLoad;
        this.negativeLoad = negativeLoad;
        this.voltageFunction = voltageFunction;
        this.voltageSource = voltageSource;
    }

    /** The voltage across its terminals (V). */
    public double terminalU() {
        return positiveLoad.getU() - negativeLoad.getU();
    }

    /** Where charging a battery that can't be recharged puts the energy (its thermal load), or null. */
    public mods.eln.sim.ThermalLoad chargeHeat;

    @Override
    public void process(double time) {
        double I = voltageSource.getCurrent();
        if (!isRechargeable && I < 0) {
            // a single-use battery being charged stores nothing: the energy goes into heat, until it burns (Baughn)
            if (chargeHeat != null) chargeHeat.moveEnergyTo(-I * voltageSource.getU() * time);
            I = 0;
        }
        if (isRechargeable && I < 0) {
            // charge past full doesn't store: the cell gasses, and the energy goes into heat (Baughn 2026-11-22)
            double dq = -I * time / QNominal, stored = Math.max(0, Math.min(dq, life - Q));
            if (dq > stored && chargeHeat != null) chargeHeat.moveEnergyTo((dq - stored) / dq * -I * voltageSource.getU() * time);
            Q += stored;
        } else {
            Q = Math.max(Q - I * time / QNominal, 0);
        }
        if (isRechargeable && Q > life) Q = life;   // (a battery saved overcharged loads full)

        double voltage = computeVoltage();

        voltageSource.setU(voltage);

    }

    double computeVoltage() {
        double voltage = voltageFunction.getValue(Q / life);
        return Math.max(0, voltage * uNominal);
    }

    public double getQRatio() {
        return Q;
    }

    public void changeLife(double newLife) {
        if (newLife < life) {
            Q *= newLife / life;
        }
        life = newLife;
    }

    public double getCharge() {
        return Q / life;
    }

    public void setCharge(double charge) {
        Q = life * charge;
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
