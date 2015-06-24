package mods.eln.sim.mna.primitives;

public class Conductance extends Unit {
    static final String _unit = "S";
    
    public Conductance() {
    	super();
    }
    
    public Conductance(double value) {
    	super(value);
    }
    
    public Conductance add(final Conductance other) {
    	return new Conductance(_value + other._value);
    }
    
    public Conductance substract(final Conductance other) {
    	return new Conductance(_value - other._value);
    }
    
    public Current multiply(final Voltage voltage) {
    	return new Current(_value * voltage._value);
    }
}
