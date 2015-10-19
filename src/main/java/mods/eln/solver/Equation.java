package mods.eln.solver;

import mods.eln.Eln;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class Equation implements IValue, INBTTReady {

	LinkedList<String> stringList = new LinkedList<String>();

	ArrayList<INBTTReady> nbtList = new ArrayList<INBTTReady>();

	static final HashMap<Integer, ArrayList<IOperatorMapper>> staticOperatorList;
	HashMap<Integer, ArrayList<IOperatorMapper>> operatorList;

	static final String staticSeparatorList;
	String separatorList;

    int iterationLimit;
    ArrayList<ISymbole> symboleList;

    IValue root;

    ArrayList<IProcess> processList = new ArrayList<IProcess>();

    int operatorCount; // Juste a counter for fun

	static {
		staticSeparatorList = "+-*&|/^,()<>=";
		staticOperatorList = new HashMap<Integer, ArrayList<IOperatorMapper>>();

		int priority = 0;
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperFunc("min", 2, Min.class));
			list.add(new OperatorMapperFunc("max", 2, Max.class));
			list.add(new OperatorMapperFunc("sin", 1, Sin.class));
			list.add(new OperatorMapperFunc("cos", 1, Cos.class));
			list.add(new OperatorMapperFunc("abs", 1, Abs.class));
			list.add(new OperatorMapperFunc("ramp", 1, Ramp.class));
			list.add(new OperatorMapperFunc("integrate", 2, Integrator.class));
			list.add(new OperatorMapperFunc("integrate", 3, IntegratorMinMax.class));
			list.add(new OperatorMapperFunc("derivate", 1, Derivator.class));
			list.add(new OperatorMapperFunc("pow", 2, Pow.class));
			list.add(new OperatorMapperFunc("pid", 5, Pid.class));
			list.add(new OperatorMapperFunc("pid", 7, PidMinMax.class));
			list.add(new OperatorMapperFunc("batteryCharge", 1, BatteryCharge.class));
			list.add(new OperatorMapperFunc("rs", 2, Rs.class));
			list.add(new OperatorMapperFunc("rc", 2, RC.class));
			list.add(new OperatorMapperFunc("if", 3, If.class));
			list.add(new OperatorMapperBracket());
			staticOperatorList.put(priority++, list);
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			staticOperatorList.put(priority++, list);
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperA("-", Inv.class));
			list.add(new OperatorMapperAB("*", Mul.class));
			list.add(new OperatorMapperAB("/", Div.class));
			staticOperatorList.put(priority++, list);
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperAB("+", Add.class));
			list.add(new OperatorMapperAB("-", Sub.class));
			staticOperatorList.put(priority++, list);
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperAB(">", Bigger.class));
			list.add(new OperatorMapperAB("<", Smaller.class));
			staticOperatorList.put(priority++, list);
		}		
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperAB("=", Eguals.class));
			list.add(new OperatorMapperAB("^", NotEguals.class));
			list.add(new OperatorMapperAB("&", And.class));
			list.add(new OperatorMapperAB("|", Or.class));
			staticOperatorList.put(priority++, list);
		}
	}

	public Equation() {
		operatorList = new HashMap<Integer, ArrayList<IOperatorMapper>>();
		separatorList = "";
		symboleList = new ArrayList<ISymbole>();
		int iterationLimit = 100;
	}

	public void setUpDefaultOperatorAndMapper() {
		operatorList.putAll(staticOperatorList);
		separatorList += staticSeparatorList;
	}

	public void addMapper(int priority, IOperatorMapper mapper) {
		ArrayList<IOperatorMapper> list = operatorList.get(priority);
		if (list == null) {
			list = new ArrayList<IOperatorMapper>();
			operatorList.put(priority, list);
		}
		list.add(mapper);
	}

	public void setIterationLimit(int iterationLimit) {
		this.iterationLimit = iterationLimit;
	}

	public void addSymbole(ArrayList<ISymbole> symboleList) {
		this.symboleList.addAll(symboleList);
	}
    
	public void preProcess(String exp) {
		int idx;
		exp = exp.replace(" ", "");

		stringList.clear();
		LinkedList<Object> list = new LinkedList<Object>();
		String stack = "";
		idx = 0;
		while (idx != exp.length()) {
			if (separatorList.contains(exp.subSequence(idx, idx + 1))) {
				if (stack != "") {
					list.add(stack);
					stringList.add(stack);
					stack = "";
				}
				list.add(exp.substring(idx, idx + 1));

			} else {
				stack += exp.charAt(idx);
			}

			idx++;
		}
		if (stack != "") {
			list.add(stack);
			stringList.add(stack);
		}

		int depthMax = getDepthMax(list);
		int depth;
		// Double str
		{
			idx = 0;
			Iterator<Object> i = list.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				if (o instanceof String) {
					String str = (String) o;
					boolean find = false;
					if (!find)
						for (ISymbole s : symboleList) {
							if (s.getName().equals(str)) {
								list.set(idx, s);
								find = true;
							}
						}
					if (!find)
						try {
							double value = Double.parseDouble(str);
							list.set(idx, new Constant(value));
							find = true;
						} catch (NumberFormatException e) {
						}
					if (!find) {
						if (str.equals("PI") || str.equals("pi")) {
							list.set(idx, new Constant(Math.PI));
						}
					}
				}
				idx++;
			}
		}

		int priority = -1;

		while (list.size() > 1 && iterationLimit != 0) {
			iterationLimit--;
			IValue a, b;
			idx = 0;
			depth = 0;
			Iterator<Object> i = list.iterator();
			priority++;
			while (i.hasNext()) {
				Object o = i.next();
				if (o instanceof String) {
					String str = (String) o;

					if (operatorList.containsKey(priority)) {
						int depthDelta = depth - depthMax;
						boolean resetPriority = false;
						for (IOperatorMapper mapper : operatorList.get(priority)) {
							IOperator operator;
							if ((operator = mapper.newOperator(str, depthDelta, list, idx)) != null) {
								if (operator instanceof IProcess)
									processList.add((IProcess) operator);
								if (operator instanceof INBTTReady)
									nbtList.add((INBTTReady) operator);
								operatorCount += operator.getRedstoneCost();
								resetPriority = true;
								break;
							}
						}
						if (resetPriority) {
							depthMax = getDepthMax(list);
							priority = -1;
							break;
						}
					}

					if (str.equals("("))
						depth++;
					if (str.equals(")"))
						depth--;
				}

				idx++;
			}
		}

		if (list.size() == 1) {
			if (list.get(0) instanceof IValue) {
				root = (IValue) list.get(0);
			} else
				root = null;
		}
	}

	int getDepthMax(LinkedList<Object> list) {
		int depth, depthMax;
		{
			depthMax = 0;
			depth = 0;
			Iterator<Object> i = list.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				if (o instanceof String) {
					String str = (String) o;
					if (str.equals("(")) depth++;
					if (str.equals(")")) depth--;
					depthMax = Math.max(depthMax, depth);
				}
			}
		}
		return depthMax;
	}
    
	public double getValue() {
		if (root == null)
			return 0.0;
		return root.getValue();
	}

	public double getValue(double deltaT) {
		if (root == null)
			return 0.0;
		for (IProcess p : processList) {
			p.process(deltaT);
		}
		return root.getValue();
	}

	public boolean isValid() {
		return root != null;
	}

	public static class Eguals extends OperatorAB {
		@Override
		public double getValue() {
			return (a.getValue() > 0.5) == (b.getValue() > 0.5) ? 1.0 : 0.0;
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class NotEguals extends OperatorAB {
		@Override
		public double getValue() {
			return (a.getValue() > 0.5) != (b.getValue() > 0.5) ? 1.0 : 0.0;
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Bigger extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() > b.getValue() ? 1.0 : 0.0;
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Smaller extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() < b.getValue() ? 1.0 : 0.0;
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class And extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() > 0.5 && b.getValue() > 0.5 ? 1.0 : 0.0;
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Or extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() > 0.5 || b.getValue() > 0.5 ? 1.0 : 0.0;
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Add extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() + b.getValue();
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Sub extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() - b.getValue();
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Mul extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() * b.getValue();
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Div extends OperatorAB {
		@Override
		public double getValue() {
			return a.getValue() / b.getValue();
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}


	public static class Inv implements IOperator {
		IValue a;

		@Override
		public double getValue() {
			return -a.getValue();
		}

		@Override
		public void setOperator(IValue[] values) {
			this.a = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Bracket implements IOperator {
		IValue a;

		@Override
		public double getValue() {
			return a.getValue();
		}

		@Override
		public void setOperator(IValue[] values) {
			this.a = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 0;
		}
	}

	public static class Abs implements IOperator {
		IValue a;

		@Override
		public double getValue() {
			return Math.abs(a.getValue());
		}

		@Override
		public void setOperator(IValue[] values) {
			this.a = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Sin implements IOperator {
		IValue a;

		@Override
		public double getValue() {
			return Math.sin(a.getValue());
		}

		@Override
		public void setOperator(IValue[] values) {
			this.a = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 2;
		}
	}

	public static class Cos implements IOperator {
		IValue a;

		@Override
		public double getValue() {
			return Math.cos(a.getValue());
		}

		@Override
		public void setOperator(IValue[] values) {
			this.a = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 2;
		}
	}


	public static class Pow implements IOperator {
		IValue a,b;

		@Override
		public double getValue() {
			return Math.pow(a.getValue(),b.getValue());
		}

		@Override
		public void setOperator(IValue[] values) {
			this.a = values[0];
			this.b = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 2;
		}
	}

	public static class Ramp implements IOperator, INBTTReady, IProcess {
		public double counter = 0.0;

		public IValue periode;

		@Override
		public double getValue() {
			return counter;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			counter = nbt.getDouble(str + "counter");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {

			nbt.setDouble(str + "counter", counter);
		}

		@Override
		public void process(double time) {
			double p = periode.getValue();
			counter += time / p;
			if (counter >= 1.0) counter -= 1.0;
			if (counter >= 1.0) counter = 0;
		}

		@Override
		public void setOperator(IValue[] values) {
			this.periode = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 3;
		}
	}

	public static class Integrator implements IOperator, INBTTReady, IProcess {
		public double counter = 0.0;
		public IValue probe, reset;

		@Override
		public double getValue() {
			return counter;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			counter = nbt.getDouble(str + "counter");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setDouble(str + "counter", counter);
		}

		@Override
		public void process(double time) {
			counter += time * probe.getValue();
			if (reset.getValue() > 0.5) counter = 0;
		}

		@Override
		public void setOperator(IValue[] values) {
			this.probe = values[0];
			this.reset = values[1];
		}

		@Override
		public int getRedstoneCost() {
			return 4;
		}
	}

	public static class IntegratorMinMax implements IOperator, INBTTReady, IProcess {
		public double counter = 0.0;

		public IValue probe, min, max;

		@Override
		public double getValue() {
			return counter;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			counter = nbt.getDouble(str + "counter");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setDouble(str + "counter", counter);
		}

		@Override
		public void process(double time) {
			counter += time * probe.getValue();
			if (counter < min.getValue()) counter = min.getValue();
			if (counter > max.getValue()) counter = max.getValue();
		}

		@Override
		public void setOperator(IValue[] values) {
			this.probe = values[0];
			this.min = values[1];
			this.max = values[2];
		}

		@Override
		public int getRedstoneCost() {
			return 4;
		}
	}

	public static class Derivator implements IOperator, INBTTReady, IProcess {
		public double old = 0.0, value = 0.0;
		public IValue probe;

		@Override
		public double getValue() {
			return value;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			old = nbt.getDouble(str + "old");
			value = nbt.getDouble(str + "value");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setDouble(str + "old", old);
			nbt.setDouble(str + "value", value);
		}

		@Override
		public void process(double time) {
			double next = probe.getValue();
			value = (next - old) / time;
			old = next;
		}

		@Override
		public void setOperator(IValue[] values) {
			this.probe = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 3;
		}
	}

	public static class Pid implements IOperator, INBTTReady, IProcess {
		public double iStack = 0.0, oldError = 0, dValue = 0;

		public IValue target, hit, p, i, d;

		@Override
		public double getValue() {
            double value = oldError * p.getValue() + iStack + dValue * d.getValue();
			return value;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			iStack = nbt.getDouble(str + "iStack");
			oldError = nbt.getDouble(str + "oldError");
			dValue = nbt.getDouble(str + "dValue");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setDouble(str + "iStack", iStack);
			nbt.setDouble(str + "oldError", oldError);
			nbt.setDouble(str + "dValue", dValue);
		}

		@Override
		public void process(double time) {
			double error = target.getValue() - hit.getValue();
			iStack += error * time * i.getValue();
			dValue = (error - oldError) / time;

            if(iStack > 1) iStack = 1;
            if(iStack < -1) iStack = -1;
			oldError = error;
		}

		@Override
		public void setOperator(IValue[] values) {
			this.target = values[0];
			this.hit = values[1];
			this.p = values[2];
			this.i = values[3];
			this.d = values[4];
		}

		@Override
		public int getRedstoneCost() {
			return 12;
		}
	}

	public static class PidMinMax extends Pid{
		public IValue min,max;
		@Override
		public double getValue() {
			return Math.max(min.getValue(),Math.min(max.getValue(),super.getValue()));
		}

		@Override
		public void setOperator(IValue[] values) {
			super.setOperator(values);
			min = values[5];
			max = values[6];
		}

		@Override
		public int getRedstoneCost() {
			return super.getRedstoneCost() + 2;
		}
	}
    
	public static class Min implements IOperator {
		public IValue a, b;

		@Override
		public double getValue() {
			return Math.min(a.getValue(), b.getValue());
		}
        
		@Override
		public void setOperator(IValue[] values) {
			this.a = values[1];
			this.b = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 2;
		}
	}
	
	public static class Max implements IOperator {
		public IValue a, b;

		@Override
		public double getValue() {
			return Math.max(a.getValue(), b.getValue());
		}
        
		@Override
		public void setOperator(IValue[] values) {
			this.a = values[1];
			this.b = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 2;
		}
	}
	
	public static class Rs implements IOperator, INBTTReady {
		public boolean state = false;

		public IValue set, reset;

		@Override
		public double getValue() {

			if (set.getValue() > 0.6) state = true;
			if (reset.getValue() > 0.6) state = false;
			return state ? 1.0 : 0.0;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			state = nbt.getBoolean(str + "state");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setBoolean(str + "state", state);
		}

		@Override
		public void setOperator(IValue[] values) {
			this.set = values[1];
			this.reset = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 3;
		}
	}

	public static class RC implements IOperator, INBTTReady, IProcess {
		public double state;

		public IValue tao, input;

		@Override
		public double getValue() {
			return state;
		}

		@Override
		public void process(double time) {
			double tao = Math.max(time, this.tao.getValue());
			state += (input.getValue() - state) / tao * time;
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			state = nbt.getDouble(str + "state");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setDouble(str + "state", state);
		}

		@Override
		public void setOperator(IValue[] values) {
			this.input = values[1];
			this.tao = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 3;
		}

	}
    
	public static class If implements IOperator {
		public double state;

		public IValue condition,thenValue,elseValue;

		@Override
		public double getValue() {
			return condition.getValue() > 0.5 ? thenValue.getValue() : elseValue.getValue();
		}
        
		@Override
		public void setOperator(IValue[] values) {
			this.condition = values[0];
			this.thenValue = values[1];
			this.elseValue = values[2];
		}

		@Override
		public int getRedstoneCost() {
			return 2;
		}

	}

	public static class BatteryCharge implements IOperator {
		public BatteryCharge() {
			FunctionTable uFq = Eln.instance.batteryVoltageFunctionTable;
			double q, dq = 0.01;
			eMax = 0;
			q = 0;
			while (q <= 1.0) {
				eMax += uFq.getValue(q) * dq;
				q += dq;
			}
		}

		double eMax;
		public IValue probe;

		@Override
		public void setOperator(IValue[] values) {
			this.probe = values[0];
		}

		@Override
		public int getRedstoneCost() {
			return 8;
		}

		@Override
		public double getValue() {
			FunctionTable uFq = Eln.instance.batteryVoltageFunctionTable;
			double probeU = probe.getValue();
			if (probeU > 1.5) return 1;
			double q = 0, dq = 0.01;
			double e = 0;
			double u;

			while ((u = uFq.getValue(q)) < probeU) {
				e += u * dq;
				q += dq;
			}

			return e / eMax;
		}
	}
    
	public boolean isSymboleUsed(ISymbole iSymbole) {
		if (!isValid()) return false;
		return stringList.contains(iSymbole.getName());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		if (!isValid()) return;
		int idx = 0;
		for (INBTTReady o : nbtList) {
			o.readFromNBT(nbt, str + idx);
			idx++;
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		if (!isValid()) return;
		int idx = 0;
		for (INBTTReady o : nbtList) {
			o.writeToNBT(nbt, str + idx);
			idx++;
		}
	}
    
	public int getOperatorCount() {
		return operatorCount;
	}
}
