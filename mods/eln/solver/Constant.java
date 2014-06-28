package mods.eln.solver;

public class Constant implements IValue{

	private double value;

	Constant(double value)
	{
		this.value = value;
	}

	@Override
	public double getValue() {
		// TODO Auto-generated method stub
		return value;
	}
	
}
