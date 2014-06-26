package mods.eln.sim.mna.component;

import java.util.ArrayList;

import mods.eln.sim.mna.SubSystem;

import org.apache.commons.math3.linear.RealMatrix;

public class Resistor extends Bipole {

	static class Line extends Resistor{
		public Line(Resistor root) {
			resistors.add(root);
			ofInterSystem = root.isInterSystem();
		}
		
		ArrayList<Resistor> resistors = new ArrayList<Resistor>();
		boolean ofInterSystem;
		boolean canAdd(Component c){
			return (c instanceof Resistor && c.isInterSystem() == ofInterSystem);
		}
	}
	
	Line line = null;
	
	double r, rInv;

	
	public double getRInv(){
		return rInv;
	}
	
	public double getR() {
		return r;
	}
	
	public Resistor setR(double r) {
		if(this.r != r){
			this.r = r;
			this.rInv = 1 / r;
			dirty();
		}
		return this;
	}

	boolean canBridge() {
		return false;
	}

	@Override
	public void applyTo(SubSystem s) {
		s.addToA(aPin, aPin, rInv);
		s.addToA(aPin, bPin, -rInv);
		s.addToA(bPin, bPin, rInv);
		s.addToA(bPin, aPin, -rInv);
	}
}
