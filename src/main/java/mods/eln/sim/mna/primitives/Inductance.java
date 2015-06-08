package mods.eln.sim.mna.primitives;

public class Inductance extends Unit {
    static final String _unit = "H";
    
    public Inductance() {
    	super();
    }
    
    public Inductance(double value) {
    	super(value);
    }
    
    public Inductance substract(final Inductance other) {
    	return new Inductance(_value - other._value);
    }

	public Resistance divide(Timedelta dt) {
		return new Resistance(_value / dt._value);
	}
}
