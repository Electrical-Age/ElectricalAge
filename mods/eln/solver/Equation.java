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
						if(str.equals("periodic")){
							if(isFuncReady(2,list,idx)){
								operatorCount+=2;
								Periodic p;
								IValue c = (IValue) list.get(idx + 2);
								IValue d = (IValue) list.get(idx + 4);
								list.set(idx,p = new Periodic(c,d));
								removeFunc(2,list,idx);
								priority = -1;
								depthMax = getDepthMax(list);
								periodicList.add(p);
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
		for(Periodic p : periodicList){
			double perio = p.periode.getValue();
			p.counter += deltaT / perio;
			if(p.counter >= p.amplitude.getValue()) p.counter = 0.0;
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

	public class Periodic implements IValue,INBTTReady{
		public double counter;
		public Periodic(IValue periode,IValue amplitude) {
			this.periode = periode;
			this.amplitude = amplitude;
			counter = 0.0;
		}
		public IValue periode,amplitude;
		@Override
		public double getValue() {
			// TODO Auto-generated method stub

			return counter*amplitude.getValue();
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
	
	
	ArrayList<Periodic> periodicList = new ArrayList<Equation.Periodic>();
	
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

	int operatorCount;
	
	public int getOperatorCount() {
		// TODO Auto-generated method stub
		return operatorCount;
	}
}
