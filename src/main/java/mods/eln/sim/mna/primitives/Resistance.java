package mods.eln.sim.mna.primitives;

public class Resistance extends Unit {
    static final String _unit = "ohm";
    
    public Resistance() {
    	super();
    }
    
    public Resistance(double value) {
    	super(value);
    }
    
    public Resistance substract(final Resistance other) {
    	return new Resistance(_value - other._value);
    }
    
    public Voltage multiply(final Current current) {
    	return new Voltage(_value * current._value);
    }
    
    public Resistance multiply(double value) {
    	return new Resistance(_value * value);
    }
    
    public double multiply(final Conductance conductance) {
    	return _value * conductance._value;
    }
    
    public Conductance invert() {
    	return new Conductance(1 / _value);
    }

	public double getValue() {
		return _value;
	}

	public Resistance add(final Resistance other) {
		return new Resistance(_value + other._value);
	}

	public double divide(Resistance other) {
		return _value / other._value;
	}

	public Resistance opposite() {
		return new Resistance(-_value);
	}
}
