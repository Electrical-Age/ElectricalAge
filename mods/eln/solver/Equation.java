package mods.eln.solver;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListResourceBundle;
import java.util.Scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.misc.FunctionTable;
import mods.eln.sim.IProcess;

public class Equation implements IValue,INBTTReady{
	
	LinkedList<String> stringList = new LinkedList<String>();
	
	ArrayList<INBTTReady> nbtList = new ArrayList<INBTTReady>();
	
	static HashMap<Integer,ArrayList<IOperatorMapper>> staticOperatorList;
	HashMap<Integer,ArrayList<IOperatorMapper>> operatorList;

	static String staticSeparatorList; 
	String separatorList; 
	 
	static{
		staticSeparatorList = "+-*&|/^,()<>";
		staticOperatorList = new HashMap<Integer,ArrayList<IOperatorMapper>>();

		int priority = 0;
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperFunc("sin",1,Sin.class));
			list.add(new OperatorMapperFunc("cos",1,Cos.class));
			list.add(new OperatorMapperFunc("abs",1,Abs.class));
			list.add(new OperatorMapperFunc("ramp",1,Ramp.class));
			list.add(new OperatorMapperFunc("integrate",2,Integrator.class));
			list.add(new OperatorMapperFunc("integrate",3,IntegratorMinMax.class));
			list.add(new OperatorMapperFunc("derivate",1,Derivator.class));
			list.add(new OperatorMapperFunc("batteryCharge",1,BatteryCharge.class));
			list.add(new OperatorMapperFunc("rs",2,Rs.class));
			list.add(new OperatorMapperBracket());
			staticOperatorList.put(priority++, list);		
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperAB("^",Pow.class));
			staticOperatorList.put(priority++, list);		
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperA("-",Inv.class));
			list.add(new OperatorMapperAB("*",Mul.class));
			list.add(new OperatorMapperAB("/",Div.class));
			staticOperatorList.put(priority++, list);
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperAB("+",Add.class));
			list.add(new OperatorMapperAB("-",Sub.class));
			staticOperatorList.put(priority++, list);
		}
		{
			ArrayList<IOperatorMapper> list = new ArrayList<IOperatorMapper>();
			list.add(new OperatorMapperAB(">",Bigger.class));
			list.add(new OperatorMapperAB("<",Smaller.class));
			list.add(new OperatorMapperAB("&",And.class));
			list.add(new OperatorMapperAB("|",Or.class));
			staticOperatorList.put(priority++, list);
		}
		
	}
	
	public Equation() {
		operatorList = new HashMap<Integer, ArrayList<IOperatorMapper>>();
		separatorList = "";
		symboleList = new ArrayList<ISymbole>();
		int iterationLimit = 100;
	}
	

	public void setUpDefaultOperatorAndMapper(){
		operatorList.putAll(staticOperatorList);
		separatorList += staticSeparatorList;
	}
	
	public void addMapper(int priority,IOperatorMapper mapper){
		ArrayList<IOperatorMapper> list = operatorList.get(priority);
		if(list == null){
			list = new ArrayList<IOperatorMapper>();
			operatorList.put(priority, list);
		}
		list.add(mapper);
	}
	
	public void setIterationLimit(int iterationLimit){
		this.iterationLimit = iterationLimit;
	}
	
	public void addSymbole(ArrayList<ISymbole> symboleList){
		this.symboleList.addAll(symboleList);
	}
	
	int iterationLimit;
	ArrayList<ISymbole> symboleList;
	
	public void preProcess(String exp){
		int idx;
		exp = exp.replace(" ", "");


		stringList.clear();
		LinkedList<Object> list = new LinkedList<Object>();
		String stack = "";
		idx = 0;
		while(idx != exp.length()){
			if(separatorList.contains(exp.subSequence(idx, idx+1))){
				if(stack != ""){
					list.add(stack);
					stringList.add(stack);
					stack = "";
				}
				list.add(exp.substring(idx, idx+1));
				
			}
			else{
				stack += exp.charAt(idx);
			}

			idx++;
		}
		if(stack != ""){
			list.add(stack);
			stringList.add(stack);
			stack = "";
		}

		int depthMax = getDepthMax(list);
		int depth;
		// Double str
		{
			idx = 0;
			Iterator<Object> i = list.iterator();
			while(i.hasNext()){
				Object o = i.next();	
				if(o instanceof String){
					String str = (String) o;
					boolean find = false;
					if(find == false)
					for(ISymbole s : symboleList){
						if(s.getName().equals(str)){
							list.set(idx, s);
							find = true;
						}
					}
					if(find == false)
					try{	
						double value = Double.parseDouble(str);
						list.set(idx, new Constant(value));
						find = true;
					}catch(NumberFormatException e){
						
					} 
					if(find == false){
						if(str.equals("PI") || str.equals("pi")){
							list.set(idx, new Constant(Math.PI));
							find = true;
						}
					}
				
				}
				idx++;
			}
		}
		
		int priority = -1;
		
		while(list.size() > 1 && iterationLimit != 0){
			iterationLimit--;
			IValue a,b;
			idx = 0;
			depth = 0;
			Iterator<Object> i = list.iterator();
			priority++;
			while(i.hasNext()){
				Object o = i.next();	
				if(o instanceof String){
					String str = (String) o;

					
					if(operatorList.containsKey(priority)){
						int depthDelta = depth - depthMax;
						boolean resetPriority = false;
						for (IOperatorMapper mapper : operatorList.get(priority)) {
							IOperator operator;
							if((operator = mapper.newOperator(str,depthDelta, list, idx)) != null){
								if(operator instanceof IProcess)
									processList.add((IProcess) operator);
								if(operator instanceof INBTTReady)
									nbtList.add((INBTTReady) operator);
								operatorCount += operator.getRedstoneCost();
								resetPriority = true;
								break;
							}
						}	
						if(resetPriority){
							depthMax = getDepthMax(list);
							priority = -1;
							break;							
						}
					}

					if(str.equals("(")) 
						depth++;
					if(str.equals(")")) 
						depth--;
				}
				
				idx++;
				
			}
		}
		
		
		if(list.size() == 1){
			if(list.get(0) instanceof IValue){
				root = (IValue)list.get(0);
			}
			else
				root = null;
		}	
	}
	



	int getDepthMax(LinkedList<Object> list)
	{
		int depth,depthMax;
		{
			depthMax = 0;
			depth = 0;
			Iterator<Object> i = list.iterator();
			while(i.hasNext()){
				Object o = i.next();	
				if(o instanceof String){
					String str = (String) o;
					if(str.equals("(")) depth++;
					if(str.equals(")")) depth--;
					depthMax = Math.max(depthMax, depth);
				}
			}
		}		
		return depthMax;
	}
	
	
	
	IValue root;
	
	public double getValue() {
		if(root == null)
			return 0.0;
		return root.getValue();
	}	
	public double getValue(double deltaT) {
		if(root == null)
			return 0.0;
		for(IProcess p : processList){
			p.process(deltaT);
		}
		return root.getValue();
	}	
	
	

	public boolean isValid()
	{
		return root != null;
	}


	
	public static class Bigger extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() > b.getValue() ? 1.0 : 0.0;
		}

		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	public static class Smaller extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() < b.getValue() ? 1.0 : 0.0;
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	public static class And extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() > 0.5 && b.getValue() > 0.5 ? 1.0 : 0.0;
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	
	public static class Or extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() > 0.5 || b.getValue() > 0.5 ? 1.0 : 0.0;
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	
	public static class Add extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() + b.getValue();
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}

	public static class Sub extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() - b.getValue();
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	
	public static class Mul extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() * b.getValue();
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	
	public static class Div extends OperatorAB{
		@Override
		public double getValue() {
			return a.getValue() / b.getValue();
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	public static class Pow extends OperatorAB{
		@Override
		public double getValue() {
			return Math.pow(a.getValue() , b.getValue());
		}
		@Override
		public int getRedstoneCost() {
			return 1;
		}
	}
	
	public static class Inv implements IOperator{
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
	public static class Bracket implements IOperator{
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
	
	public static class Abs implements IOperator{
		IValue a;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
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
	
	public static class Sin implements IOperator{
		IValue a;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
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
	public static class Cos implements IOperator{
		IValue a;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
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

	public static class Ramp implements IOperator,INBTTReady,IProcess{
		public double counter = 0.0;

		public IValue periode;
		@Override
		public double getValue() {
			return counter;
		}
		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			counter = nbt.getDouble(str + "counter");
		}
		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			nbt.setDouble(str + "counter", counter);
		}
		@Override
		public void process(double time) {
			double p = periode.getValue();
			counter += time / p;
			if(counter >= 1.0) counter -= 1.0;
			if(counter >= 1.0) counter = 0;
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
	
	public static class Integrator implements IOperator,INBTTReady,IProcess{
		public double counter = 0.0;
		public IValue probe,reset;
		@Override
		public double getValue() {
			return counter;
		}
		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			counter = nbt.getDouble(str + "counter");
		}
		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			nbt.setDouble(str + "counter", counter);
		}
		@Override
		public void process(double time) {
			counter += time*probe.getValue();
			if(reset.getValue() > 0.5) counter = 0;
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
	
	public static class IntegratorMinMax implements IOperator,INBTTReady,IProcess{
		public double counter = 0.0;

		public IValue probe,min,max;
		@Override
		public double getValue() {
			return counter;
		}
		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			counter = nbt.getDouble(str + "counter");
		}
		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			nbt.setDouble(str + "counter", counter);
		}
		@Override
		public void process(double time) {
			counter += time*probe.getValue();
			if(counter < min.getValue()) counter = min.getValue();
			if(counter > max.getValue()) counter = max.getValue();
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
	
	public static class Derivator implements IOperator,INBTTReady,IProcess{
		public double old = 0.0,value = 0.0;
		public IValue probe;
		@Override
		public double getValue() {
			return value;
		}
		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			old = nbt.getDouble(str + "old");
			value = nbt.getDouble(str + "value");
		}
		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			nbt.setDouble(str + "old", old);
			nbt.setDouble(str + "value", value);
		}
		@Override
		public void process(double time) {
			double next = probe.getValue();
			value = (next-old)/time;
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
	
	public static class Rs implements IOperator,INBTTReady{
		public boolean state = false;

		public IValue set,reset;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			if(set.getValue() > 0.6) state = true;
			if(reset.getValue() > 0.6) state = false;
			return state ? 1.0 : 0.0;
		}
		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			state = nbt.getBoolean(str + "state");
		}
		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setBoolean(str + "state",state);
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
	

	
	public static class BatteryCharge implements IOperator{
		public BatteryCharge() {
			FunctionTable  uFq= Eln.instance.batteryVoltageFunctionTable;
			double q = 0,dq = 0.01;
			eMax = 0;
			q = 0;
			while(q <= 1.0){
				eMax += uFq.getValue(q)*dq;			
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
			FunctionTable  uFq= Eln.instance.batteryVoltageFunctionTable;
			double probeU = probe.getValue();
			if(probeU > 1.5) return 1;
			double q = 0,dq = 0.01;
			double e = 0;
			double u;
			
			while((u = uFq.getValue(q)) < probeU){
				e += u*dq;			
				q += dq;
			}

			return e/eMax;
		}
	}
	
	
	ArrayList<IProcess> processList = new ArrayList<IProcess>();
	
	public boolean isSymboleUsed(ISymbole iSymbole) {
		if(isValid() == false) return false;
		return stringList.contains(iSymbole.getName());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		if(isValid() == false) return;
		int idx = 0;
		for(INBTTReady o : nbtList){
			o.readFromNBT(nbt, str + idx);
			idx++;
		}		
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		if(isValid() == false) return;
		int idx = 0;
		for(INBTTReady o : nbtList){
			o.writeToNBT(nbt, str + idx);
			idx++;
		}
	}

	int operatorCount; //Juste a counter for fun
	
	public int getOperatorCount() {
		// TODO Auto-generated method stub
		return operatorCount;
	}
}
