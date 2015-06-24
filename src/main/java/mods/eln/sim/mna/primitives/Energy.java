package mods.eln.sim.mna.primitives;

public class Energy extends Unit {
    static final String _unit = "V";
    
    public Energy() {
    	super();
    }
    
    public Energy(double value) {
    	super(value);
    }
    
    public Energy(final Voltage voltage, final Capacitance capacity) {  	
    	super(voltage._value * voltage._value * capacity._value / 2);
    }
}
