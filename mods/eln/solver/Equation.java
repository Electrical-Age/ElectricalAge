package mods.eln.solver;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListResourceBundle;
import java.util.Scanner;

import net.minecraft.nbt.NBTTagCompound;

import mods.eln.INBTTReady;
import mods.eln.sim.IProcess;

public class Equation implements IValue,INBTTReady{
	
	LinkedList<String> stringList = new LinkedList<String>();
	
	ArrayList<INBTTReady> nbtList = new ArrayList<INBTTReady>();
	
	public Equation(String exp,ArrayList<ISymbole> symboleList,int iterationLimit)
	{
		int idx;
		exp = exp.replace(" ", "");

		//String [] sList = exp.split("\\+|\\-|\\*|\\/|\\(|\\)");//+|\\-|\\*|\\/|\\(|\\)
		/*for(String str : sList){
			list.add(str);
		}  */

		stringList.clear();
		LinkedList<Object> list = new LinkedList<Object>();
		String stack = "";
		idx = 0;
		while(idx != exp.length()){
			switch(exp.charAt(idx)){
			case '+':
			case '-':
			case '*':
			case '&':
			case '|':
			case '/':
			case '^':
			case ',':
			case '(':
			case ')':
			case '<':
			case '>':
				if(stack != ""){
					list.add(stack);
					stringList.add(stack);
					stack = "";
				}
				list.add(exp.substring(idx, idx+1));
				
				break;
			default:
				stack += exp.charAt(idx);
				break;
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
						double value = NumberFormat.getInstance().parse(str).floatValue();
						list.set(idx, new Constant(value));
						find = true;
					}catch(NumberFormatException e){
						
					} catch (ParseException e) {
						// TODO Auto-generated catch block
					//	e.printStackTrace();
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
					if(idx-1 >= 0 && list.get(idx-1) instanceof IValue) a = (IValue)list.get(idx-1);
					else a = null;
					if(idx+1 <= list.size()-1 && list.get(idx+1) instanceof IValue) b = (IValue)list.get(idx+1);
					else b = null;
					
					String str = (String) o;
					if(str.equals("(")){
						if(idx > list.size() - 3) return;
						if(list.get(idx+1) instanceof IValue 
								&& list.get(idx+2) instanceof String && ((String)list.get(idx+2)).equals(")")){
							list.remove(idx+2);
							list.remove(idx+0);
							priority = -1;
							depthMax = getDepthMax(list);
							break;
						}
					}
					if(depth + 1 == depthMax){
						if(str.equals("ramp")){
							if(isFuncReady(1,list,idx)){
								operatorCount+=2;
								Ramp p;
								IValue c = (IValue) list.get(idx + 2);
								list.set(idx,p = new Ramp(c));
								removeFunc(1,list,idx);
								priority = -1;
								depthMax = getDepthMax(list);
								processList.add(p);
								nbtList.add(p);
								break;
							}
						}	
						if(str.equals("rs")){
							if(isFuncReady(2,list,idx)){
								operatorCount+=2;
								Rs rs;
								IValue c = (IValue) list.get(idx + 2);
								IValue d = (IValue) list.get(idx + 4);
								list.set(idx,rs = new Rs(c,d));
								removeFunc(2,list,idx);
								priority = -1;
								depthMax = getDepthMax(list);
								nbtList.add(rs);
								break;
							}
						}							
						if(str.equals("integrate")){
							if(isFuncReady(2,list,idx)){
								operatorCount+=2;
								Integrator integrator;
								IValue c = (IValue) list.get(idx + 2);
								IValue d = (IValue) list.get(idx + 4);
								list.set(idx,integrator = new Integrator(c,d));
								removeFunc(2,list,idx);
								priority = -1;
								depthMax = getDepthMax(list);
								processList.add(integrator);
								nbtList.add(integrator);
								break;
							}
						}
						if(str.equals("derivate")){
							if(isFuncReady(1,list,idx)){
								operatorCount+=2;
								Derivator derivator;
								IValue c = (IValue) list.get(idx + 2);
								list.set(idx,derivator = new Derivator(c));
								removeFunc(1,list,idx);
								priority = -1;
								depthMax = getDepthMax(list);
								processList.add(derivator);
								nbtList.add(derivator);
								break;
							}
						}
					}
					if(depth == depthMax){
						if(priority >= 0){
							if(b != null){
								if(str.equals("abs")){
									operatorCount++;
									list.set(idx, new Abs(b));
									list.remove(idx+1);
									priority = -1;
									break;
								}								
								if(str.equals("sin")){
									operatorCount++;
									list.set(idx, new Sin(b));
									list.remove(idx+1);
									priority = -1;
									break;
								}								
								if(str.equals("cos")){
									operatorCount++;
									list.set(idx, new Cos(b));
									list.remove(idx+1);
									priority = -1;
									break;
								}								
							}
							

						}
						if(priority >= 1){
							if(a != null && b != null){
								if(str.equals("^")){
									operatorCount++;
									list.set(idx-1, new Pow(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
							}	
						}
						if(priority >= 2){
							if(a == null && b != null){
								if(str.equals("-")){
									operatorCount++;
									list.set(idx, new Inv(b));
									list.remove(idx+1);
									priority = -1;
									break;
								}								
							}
							if(a != null && b != null){
								if(str.equals("*")){
									operatorCount++;
									list.set(idx-1, new Mul(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
								if(str.equals("/")){
									operatorCount++;
									list.set(idx-1, new Div(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
							}	
						}
						if(priority >= 3){
							if(a != null && b != null){
								if(str.equals("+")){		
									operatorCount++;
									list.set(idx-1, new Add(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}						
								if(str.equals("-")){
									operatorCount++;
									list.set(idx-1, new Sub(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
							}		
						}
						if(priority >= 4){
							if(a != null && b != null){
								if(str.equals(">")){		
									operatorCount++;
									list.set(idx-1, new Bigger(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}						
								if(str.equals("<")){
									operatorCount++;
									list.set(idx-1, new Smaller(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
								if(str.equals("&")){
									operatorCount++;
									list.set(idx-1, new And(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
								if(str.equals("|")){
									operatorCount++;
									list.set(idx-1, new Or(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
							}		
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

	private void removeFunc(int argCount, LinkedList<Object> list, int offset) {
		for(int idx = 0;idx< 2 + argCount*2-1;idx++){
			list.remove(offset + 1);
		}
		
	}

	private boolean isFuncReady(int argCount, LinkedList<Object> list, int offset) {
		int counter = 0;
		offset++;
		for(int end = offset + 2 + argCount*2-1;offset < end;offset++) {
			Object o = list.get(offset);
			String str = null;
			if(o instanceof String) str = (String) o;
			if(counter == 0){
				if(str.equals("(") == false) return false; 
			}
			else if(offset == end-1){
				if(str.equals(")") == false) return false; 
			}
			else if((counter % 2) == 1){
				if(o instanceof IValue == false) return false;
			}
			else{
				if(str.equals(",") == false) return false; 
			}
			counter++;
		}
		return true;
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

	public class Bigger implements IValue{
		public Bigger(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() > b.getValue() ? 1.0 : 0.0;
		}
	}
	public class Smaller implements IValue{
		public Smaller(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() < b.getValue() ? 1.0 : 0.0;
		}
	}
	public class And implements IValue{
		public And(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() > 0.5 && b.getValue() > 0.5 ? 1.0 : 0.0;
		}
	}
	
	public class Or implements IValue{
		public Or(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() > 0.5 || b.getValue() > 0.5 ? 1.0 : 0.0;
		}
	}
	
	public class Add implements IValue{
		public Add(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() + b.getValue();
		}
	}

	public class Sub implements IValue{
		public Sub(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() - b.getValue();
		}
	}
	
	public class Mul implements IValue{
		public Mul(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() * b.getValue();
		}
	}
	
	public class Div implements IValue{
		public Div(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return a.getValue() / b.getValue();
		}
	}
	public class Pow implements IValue{
		public Pow(IValue a,IValue b) {
			this.a = a;
			this.b = b;
		}
		IValue a,b;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return Math.pow(a.getValue() , b.getValue());
		}
	}
	public class Inv implements IValue{
		public Inv(IValue a) {
			this.a = a;
		}
		IValue a;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return -a.getValue();
		}
	}
	public class Abs implements IValue{
		public Abs(IValue a) {
			this.a = a;
		}
		IValue a;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return Math.abs(a.getValue());
		}
	}
	
	public class Sin implements IValue{
		public Sin(IValue a) {
			this.a = a;
		}
		IValue a;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return Math.sin(a.getValue());
		}
	}
	public class Cos implements IValue{
		public Cos(IValue a) {
			this.a = a;
		}
		IValue a;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return Math.cos(a.getValue());
		}
	}

	public class Ramp implements IValue,INBTTReady,IProcess{
		public double counter;
		public Ramp(IValue periode) {
			this.periode = periode;
			counter = 0.0;
		}
		public IValue periode;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub

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
	}
	
	public class Integrator implements IValue,INBTTReady,IProcess{
		public double counter;
		public Integrator(IValue probe,IValue reset) {
			this.probe = probe;
			this.reset = reset;
			counter = 0.0;
		}
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
	}
	
	public class Derivator implements IValue,INBTTReady,IProcess{
		public double old,value;
		public Derivator(IValue probe) {
			this.probe = probe;
		}
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
	}
	
	public class Rs implements IValue,INBTTReady{
		public boolean state;
		public Rs(IValue reset,IValue set) {
			this.set = set;
			this.reset = reset;
			state = false;
		}
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
