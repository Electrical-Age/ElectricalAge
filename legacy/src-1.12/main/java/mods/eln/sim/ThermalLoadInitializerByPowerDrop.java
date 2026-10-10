package mods.eln.sim;

import mods.eln.Eln;

public class ThermalLoadInitializerByPowerDrop {

    public double warmLimit, coolLimit;
    double heatingTao;
    double TConductivityDrop;

    public double Rs;
    public double Rp;
    /**
     * Thermal capacitance.
     */
    public double C;

    /**
     * @param warmLimit Intended maximum temperature in celsius.
     * @param coolLimit Intended minimum temperature in celsius.
     * @param heatingTao
     * @param TConductivityDrop
     */
    public ThermalLoadInitializerByPowerDrop(double warmLimit, double coolLimit, double heatingTao, double TConductivityDrop) {
        this.TConductivityDrop = TConductivityDrop;
        this.coolLimit = coolLimit;
        this.heatingTao = heatingTao;
        this.warmLimit = warmLimit;
    }

    public void setMaximalPower(double P) {
        C = P * heatingTao / warmLimit;
        Rp = warmLimit / P;
        Rs = TConductivityDrop / P / 2;

        Eln.simulator.checkThermalLoad(Rs, Rp, C);
    }

    public void applyTo(ThermalLoad load) {
        load.set(Rs, Rp, C);
    }

    public ThermalLoadInitializerByPowerDrop copy() {
        ThermalLoadInitializerByPowerDrop thermalLoad = new ThermalLoadInitializerByPowerDrop(warmLimit, coolLimit, heatingTao, TConductivityDrop);
        thermalLoad.Rp = Rp;
        thermalLoad.Rs = Rs;
        thermalLoad.C = C;
        return thermalLoad;
    }
}
