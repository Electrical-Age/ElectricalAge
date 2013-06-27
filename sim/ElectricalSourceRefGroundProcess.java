package mods.eln.sim;

public class ElectricalSourceRefGroundProcess implements IProcess {
	public ElectricalLoad eLoad;
	public double Uc;
	public ElectricalSourceRefGroundProcess(ElectricalLoad eLoad,double Uc) 
	{
		this.eLoad = eLoad;
		this.Uc = Uc;
		// TODO Auto-generated constructor stub
	}
	public void setUc(double Uc)
	{
		this.Uc = Uc;
	}
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub

		eLoad.moveCurrentTo((Uc-eLoad.Uc)*eLoad.getC()/time);
	}
}
