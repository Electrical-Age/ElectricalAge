package mods.eln.sim;

public class RegulatorFurnaceProcess extends RegulatorProcess{
	FurnaceProcess furnace;
	public RegulatorFurnaceProcess(String name,FurnaceProcess furnace) {
		super(name);
		this.furnace = furnace;
	}
	@Override
	protected double getHit() {
		// TODO Auto-generated method stub
		return furnace.load.Tc;
	}
	@Override
	protected void setCmd(double cmd) {
		furnace.setGain(cmd);		
	}

}
