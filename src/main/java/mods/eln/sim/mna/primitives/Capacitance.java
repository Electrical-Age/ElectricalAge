package mods.eln.sim.mna.primitives;

public class Capacitance extends Unit {
    static final String _unit = "F";
    
    public Capacitance() {
    	super();
    }
    
    public Capacitance(double value) {
    	super(value);
    }
    
    public Capacitance add(final Capacitance other) {
    	return new Capacitance(_value + other._value);
    }
    
    public Capacitance substract(final Capacitance other) {
    	return new Capacitance(_value - other._value);
    }
    
    public Conductance divide(final Timedelta timedelta) {
    	return new Conductance(_value / timedelta._value);
    }
}
