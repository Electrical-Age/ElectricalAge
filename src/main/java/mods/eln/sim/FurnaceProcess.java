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

    /** The fire's real burn level, lagging the gain (Fire.COAL). */
    public final Fire fire = new Fire(Fire.COAL);

    @Override
    public void process(double time) {
        // the fire can't burn harder than its bed has fuel for (Baughn: with nothing to burn it banks until it's out,
        // rather than climbing to the setting on embers and flaring to full power when fuel comes back)
        fire.step(Math.min(gain, fuelShare()), combustibleEnergy > 0, time);
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

    /** How full the fuel bed is (0..1). */
    public double fuelShare() {
        return nominalCombustibleEnergy <= 0 ? 0 : Math.max(0, Math.min(1, combustibleEnergy / nominalCombustibleEnergy));
    }

    public double getP() {
        return combustibleEnergy / nominalCombustibleEnergy * nominalPower * fire.level;
    }
}
