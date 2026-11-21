package mods.eln.sim;

public class RegulatorFurnaceProcess extends RegulatorProcess {

    FurnaceProcess furnace;

    public RegulatorFurnaceProcess(String name, FurnaceProcess furnace) {
        super(name);
        this.furnace = furnace;
    }

    @Override
    protected double getHit() {
        return furnace.load.Tc;
    }

    /** Its bed, still spreading, burns less than the air lets it. */
    @Override
    protected boolean limitedUp() {
        return furnace.fire.bed < furnace.fire.air - 0.02;
    }

    @Override
    protected void setCmd(double cmd) {
        furnace.setGain(cmd);
    }
}
