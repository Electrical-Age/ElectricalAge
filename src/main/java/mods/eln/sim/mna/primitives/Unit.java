package mods.eln.sim.mna.primitives;

public abstract class Unit {
	final double _value;
    static final String _unit = "s";
    
    public Unit() {
    	_value = 0;
    }
    
    public Unit(double value) {
    	_value = value;
    }
    
    public boolean isNaN() {
    	return Double.isNaN(_value);
    }
    
	public double getValue() {
		return _value;
	}
}
