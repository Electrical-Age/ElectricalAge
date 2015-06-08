package mods.eln.sim.mna.primitives;

public class Power extends Unit {
    static final String _unit = "W";
    
    public Power() {
    	super();
    }
    
    public Power(double value) {
    	super(value);
    }
    
    public Power add(final Power other) {
    	return new Power(_value + other._value);
    }
    
    public Power substract(final Power other) {
    	return new Power(_value - other._value);
    }
    
    public Voltage divide(final Current current) {
    	return new Voltage(_value / current._value);
    }
    
    public Current divide(final Voltage voltage) {
    	return new Current(_value / voltage._value);
    }
}
