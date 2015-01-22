package mods.eln.solver;

public class ConstSymbole implements ISymbole{
	private double value;
	private String name;

	public ConstSymbole(String name,double value) {
		this.value = value;
		this.name = name;
	}
	@Override
	public double getValue() {
		
		return value;
	}

	@Override
	public String getName() {
		
		return name;
	}

}
