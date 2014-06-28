package mods.eln.sim.mna;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import mods.eln.misc.Profiler;
import mods.eln.misc.Utils;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Delay;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.misc.ISubSystemProcessFlush;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RRQRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

public class SubSystem {
	public ArrayList<Component> component = new ArrayList<Component>();
	public ArrayList<State> states = new ArrayList<State>();
	public LinkedList<IDestructor> breakDestructor = new LinkedList<IDestructor>();

	ArrayList<ISubSystemProcessI> processI = new ArrayList<ISubSystemProcessI>();
	State[] statesTab;

	RootSystem root;
	
	public SubSystem(RootSystem root,double dt) {
		this.dt = dt;
		this.root = root;
	}

	double dt;
	boolean matrixValid = false;

	public void invalidate() {
		matrixValid = false;
	}

	int stateCount;
	RealMatrix A;
	//RealMatrix I;
	boolean singularMatrix;

	double[][] AInvdata;
	double[] Idata;

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
		c.quitSubSystem();
		invalidate();
	}

	public void removeState(State s) {
		states.remove(s);
		s.quitSubSystem();
		invalidate();
	}
	
	/*public void removeAll() {
		for(Component c : component) {
			c.disconnectFromSubSystem();
		}
		for(State s : states) {
			s.disconnectFromSubSystem();
		}	
		invalidate();
	}*/
	
	public void removeProcess(ISubSystemProcessI p) {
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

	public void addProcess(ISubSystemProcessI p) {
		processI.add(p);
	}
	
	//double[][] getDataRef()

	public void generateMatrix() {
		stateCount = states.size();

		A = MatrixUtils.createRealMatrix(stateCount, stateCount);
		//Adata = ((Array2DRowRealMatrix) A).getDataRef();
		// X = MatrixUtils.createRealMatrix(stateCount, 1); Xdata =
		// ((Array2DRowRealMatrix)X).getDataRef();
		//I = MatrixUtils.createRealMatrix(stateCount, 1);
		//Idata = ((Array2DRowRealMatrix) I).getDataRef();
		Idata = new double[stateCount];
		XtempData = new double[stateCount];
		{
			int idx = 0;
			for(State s : states) {
				s.setId(idx++);
			}
		}

		for(Component c : component) {
			c.applyTo(this);
		}
		
		Profiler p = new Profiler();
		p.add("Inversse");
		
		try {
			//FieldLUDecomposition QRDecomposition  LUDecomposition RRQRDecomposition
			RealMatrix Ainv = new QRDecomposition(A).getSolver().getInverse();
			AInvdata = Ainv.getData();
			singularMatrix = false;
		} catch (Exception e) {
			singularMatrix = true;
		}
		
		p.stop();
		Utils.println(p);

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
		Idata[s.getId()] =  v;
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
		Profiler profiler = new Profiler();
	//	profiler.add("generateMatrix");
		if(matrixValid == false) {
			generateMatrix();
		}

		if(singularMatrix == false){
			//profiler.add("generateMatrix");
			for(int y = 0; y < stateCount; y++) {
				Idata[y] = 0;
			}
			//profiler.add("generateMatrix");
			for(ISubSystemProcessI p : processI) {
				p.simProcessI(this);
			}
		//	profiler.add("generateMatrix");
			
			for(int idx2 = 0;idx2 < stateCount;idx2++){
				double stack = 0;
				for(int idx = 0;idx < stateCount;idx++){
					stack += AInvdata[idx2][idx]*Idata[idx];
				}
				XtempData[idx2] = stack;
			}
			//Xtemp = Ainv.multiply(I);
			
		}
		profiler.stop();
		//Utils.println(profiler);
	}

	
	public double solve(State pin) {
		//Profiler profiler = new Profiler();
		if(matrixValid == false) {
			generateMatrix();
		}

		if(singularMatrix == false){
			for(int y = 0; y < stateCount; y++) {
				Idata[y] = 0;
			}
			for(ISubSystemProcessI p : processI) {
				p.simProcessI(this);
			}

			int idx2 = pin.getId();
			double stack = 0;
			for(int idx = 0;idx < stateCount;idx++){
				stack += AInvdata[idx2][idx]*Idata[idx];
			}
			return stack;
			
		}
		return 0;
	}
	
	
	//RealMatrix Xtemp;
	double[] XtempData;
	public void stepFlush() {
		if(singularMatrix == false){
			for(int idx = 0; idx < stateCount; idx++) {
				//statesTab[idx].state = Xtemp.getEntry(idx, 0);
				statesTab[idx].state = XtempData[idx];

			}
		}else{
			for(int idx = 0; idx < stateCount; idx++) {
				statesTab[idx].state = 0;
			}			
		}
		
		for(ISubSystemProcessFlush p : processF){
			p.simProcessFlush();
		}
	}

	public static void main(String[] args) {

//		SubSystem s = new SubSystem(null,0.1);
//		VoltageState n1, n2;
//		VoltageSource u1;
//		Resistor r1, r2;
//
//		s.addState(n1 = new VoltageState());
//		s.addState(n2 = new
//				VoltageState());
//
//		//s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));
//
//		s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
//		s.addComponent((r2 = new Resistor()).setR(20).connectTo(n2, null));
//
//		s.step();
//		s.step();

				SubSystem s = new SubSystem(null,0.1);
				VoltageState n1, n2, n3, n4, n5;
				VoltageSource u1;
				Resistor r1, r2, r3;
				Delay d1, d2;
		
				s.addState(n1 = new VoltageState());
				s.addState(n2 = new VoltageState());
				s.addState(n3 = new VoltageState());
			//	s.addState(n4 = new VoltageState());
			//	s.addState(n5 = new VoltageState());
		
				s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));
		
				s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
				s.addComponent((d1 = new Delay()).set(1).connectTo(n2, n3));
				s.addComponent((r2 = new Resistor()).setR(10).connectTo(n3, null));
				//s.addComponent((d2 = new Delay()).set(10).connectTo(n4, n5));
				//s.addComponent((r2 = new Resistor()).setR(10).connectTo(n5, null));

		for(int idx = 0; idx < 100; idx++) {
			s.step();
		}

		System.out.println("END");
		
		s.step();
		s.step();
		s.step();
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

	
	
	public void breakSystem(){
		while (breakDestructor.isEmpty() == false) {
			breakDestructor.pop().destruct(false);
		}
		
		for(Component c : component) {
			c.quitSubSystem();
		}
		for(State s : states) {
			s.quitSubSystem();
		}	
		
		if(root != null){
			for(Component c : component) {
				c.returnToRootSystem(root);
			}
			for(State s : states) {
				s.returnToRootSystem(root);
			}	
		}
		root.systems.remove(this);
		
		
		
		invalidate();		
	}
	
	
	ArrayList<ISubSystemProcessFlush> processF = new ArrayList<ISubSystemProcessFlush>();

	public void addProcess(ISubSystemProcessFlush p) {
		processF.add(p);
	}
	public void removeProcess(ISubSystemProcessFlush p) {
		processF.remove(p);
	}




}
