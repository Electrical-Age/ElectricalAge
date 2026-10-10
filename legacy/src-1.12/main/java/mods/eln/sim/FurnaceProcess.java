package mods.eln.sim;

public class FurnaceProcess implements IProcess {

    public ThermalLoad load;
    public double combustibleEnergy = 0;
    public double nominalCombustibleEnergy = 1;
    public double nominalPower = 1;
    private double gain = 1.0;
    private double gainMin = 0.0;

    public FurnaceProcess(ThermalLoad load) {
        this.load = load;
    }

    @Override
    public void process(double time) {
        double energyConsumed = getP() * time;
        combustibleEnergy -= energyConsumed;
        load.PcTemp += energyConsumed / time;
    }

    public void setGain(double gain) {
        if (gain < gainMin) gain = gainMin;
        if (gain > 1.0) gain = 1.0;
        this.gain = gain;
    }

    public void setGainMin(double gainMin) {
        this.gainMin = gainMin;
        setGain(getGain());
    }

    public double getGain() {
        return gain;
    }

    public double getP() {
        return combustibleEnergy / nominalCombustibleEnergy * nominalPower * gain;
    }
}
