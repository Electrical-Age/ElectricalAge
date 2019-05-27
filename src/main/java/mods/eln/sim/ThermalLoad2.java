package mods.eln.sim;

public class ThermalLoad2 {

    // environment temperature (deg C)
    double tempEnv;
    // mass (kG)
    double mass;
    // specific heat (kJ/kG)
    double specificHeat;
    // energy (kJ)
    double energy;
    boolean isSlow;

    /**
     * ThermalLoad2
     *
     * Specific Heat for various materials:
     * Copper: 0.385
     * Iron: 0.450
     * Aluminum: 0.902
     *
     * @param tempEnv Environmental (Biome) Temperature (deg C)
     * @param mass Thermal Mass (kG)
     * @param specificHeat Specific Heat (kJ/kG)
     */
    public ThermalLoad2(double tempEnv, double mass, double specificHeat) {
        this.tempEnv = tempEnv;
        this.mass = mass;
        this.specificHeat = specificHeat;
        this.energy = 0;
        this.isSlow = true;
    }

    public void movePower(double power) {
        energy += power / 0.05;
    }

    public void movePower(double power, ThermalLoad2 other) {
        this.energy -= power / 0.05;
        other.energy += power / 0.05;
    }

    public void movePower(double energy, double time, ThermalLoad2 other) {
        this.energy -= energy / (time * 20);
        other.energy += energy / (time * 20);
    }

    public void updateTempEnv(double tempEnv) {
        this.tempEnv = tempEnv;
    }

    public void setEnergyFromTemp(double temp) {
        this.energy = specificHeat / energy;
    }

    public double getT() {
        return energy / (specificHeat * mass);
    }

    public double getPower() {
        return 0;
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
