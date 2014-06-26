package mods.eln.sim.mna;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Delay;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.misc.ISystemProcessI;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

public class SubSystem {
	ArrayList<Component> component = new ArrayList<Component>();
	public ArrayList<State> states = new ArrayList<State>();
	public LinkedList<IDestructor> breakDestructor = new LinkedList<IDestructor>();

	ArrayList<ISystemProcessI> processI = new ArrayList<ISystemProcessI>();
	State[] statesTab;

	public SubSystem(double dt) {
		this.dt = dt;
	}

	double dt;
	boolean matrixValid = false;

	public void invalidate() {
		matrixValid = false;
	}

	int stateCount;
	RealMatrix A, Ainv;
	RealMatrix I;
	boolean singularMatrix;

	//double[][] Adata, Idata, Xdata;

	public void addComponent(Component c) {
		component.add(c);
		c.addedTo(this);
		invalidate();
	}

	public void addState(State s) {
		states.add(s);
		s.addedTo(this);
		invalidate();
	}

	public void removeComponent(Component c) {
		component.remove(c);
		c.disconnectFromSubSystem();
		invalidate();
	}

	public void removeState(State s) {
		states.remove(s);
		s.disconnectFromSubSystem();
		invalidate();
	}

	public void removeProcess(ISystemProcessI p) {
		processI.remove(p);
		invalidate();
	}

	public void addComponent(Iterable<Component> i) {
		for(Component c : i) {
			addComponent(c);
		}
	}

	public void addState(Iterable<State> i) {
		for(State s : i) {
			addState(s);
		}
	}

	public void addProcess(ISystemProcessI p) {
		processI.add(p);
	}
	
	//double[][] getDataRef()

	public void generateMatrix() {
		stateCount = states.size();

		A = MatrixUtils.createRealMatrix(stateCount, stateCount);
		//Adata = ((Array2DRowRealMatrix) A).getDataRef();
		// X = MatrixUtils.createRealMatrix(stateCount, 1); Xdata =
		// ((Array2DRowRealMatrix)X).getDataRef();
		I = MatrixUtils.createRealMatrix(stateCount, 1);
		//Idata = ((Array2DRowRealMatrix) I).getDataRef();

		{
			int idx = 0;
			for(State s : states) {
				s.setId(idx++);
			}
		}

		for(Component c : component) {
			c.applyTo(this);
		}

		try {
			Ainv = new LUDecomposition(A).getSolver().getInverse();
			singularMatrix = false;
		} catch (Exception e) {
			singularMatrix = true;
		}
		

		statesTab = new State[stateCount];
		statesTab = states.toArray(statesTab);

		matrixValid = true;
	}

	public void addToA(State a, State b, double v) {
		if(a == null || b == null)
			return;
		A.addToEntry(a.getId(), b.getId(), v);
		//Adata[a.getId()][b.getId()] += v;
	}

	public void addToI(State s, double v) {
		I.addToEntry(s.getId(),0, v);
		//Idata[s.getId()][0] += v;
	}

	/*
	 * public void pushX(){
	 * 
	 * }
	 */
	/*
	 * public void popX(){
	 * 
	 * }
	 */

	public void step() {
		stepCalc();
		stepFlush();
	}

	public void stepCalc() {
		if(matrixValid == false) {
			generateMatrix();
		}

		if(singularMatrix == false){
			for(int y = 0; y < stateCount; y++) {
				I.setEntry(y,0, 0);
			}
			for(ISystemProcessI p : processI) {
				p.simProcessI(this);
			}
			Xtemp = Ainv.multiply(I);
		}
	}

	RealMatrix Xtemp;

	public void stepFlush() {
		if(singularMatrix == false){
			for(int idx = 0; idx < stateCount; idx++) {
				statesTab[idx].state = Xtemp.getEntry(idx, 0);
			}
		}else{
			for(int idx = 0; idx < stateCount; idx++) {
				statesTab[idx].state = 0;
			}			
		}
	}

	public static void main(String[] args) {

		SubSystem s = new SubSystem(0.1);
		VoltageState n1, n2;
		VoltageSource u1;
		Resistor r1, r2;

		s.addState(n1 = new VoltageState());
		s.addState(n2 = new
				VoltageState());

		//s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));

		s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
		s.addComponent((r2 = new Resistor()).setR(20).connectTo(n2, null));

		s.step();
		s.step();

		//		SubSystem s = new SubSystem(0.1);
		//		VoltageState n1, n2, n3, n4, n5;
		//		VoltageSource u1;
		//		Resistor r1, r2, r3;
		//		Delay d1, d2;
		//
		//		s.addState(n1 = new VoltageState());
		//		s.addState(n2 = new VoltageState());
		//		s.addState(n3 = new VoltageState());
		//		s.addState(n4 = new VoltageState());
		//		s.addState(n5 = new VoltageState());
		//
		//		s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));
		//
		//		s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
		//		s.addComponent((d1 = new Delay()).set(5).connectTo(n2, n3));
		//		s.addComponent((r2 = new Resistor()).setR(10).connectTo(n3, n4));
		//		s.addComponent((d2 = new Delay()).set(10).connectTo(n4, n5));
		//		s.addComponent((r2 = new Resistor()).setR(10).connectTo(n5, null));

		for(int idx = 0; idx < 100; idx++) {
			s.step();
		}

		System.out.println("END");
	}

	public boolean containe(State state) {
		// TODO Auto-generated method stub
		return states.contains(state);
	}

	public void setX(State s, double value) {
		s.state = value;
	}

	public double getX(State s) {
		return s.state;
	}

	public void breakSystem(RootSystem root) {
		while (breakDestructor.isEmpty() == false) {
			breakDestructor.pop().destruct();
		}

		for(Component c : component) {
			c.disconnectFromSubSystem();
		}
		for(State s : states) {
			s.disconnectFromSubSystem();
		}

		root.addComponents.addAll(component);
		root.addStates.addAll(states);
	}

}
