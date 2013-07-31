package mods.eln.solver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListResourceBundle;
import java.util.Scanner;

public class Equation implements IValue{
	
	LinkedList<String> stringList = new LinkedList<String>();
	
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
			case '(':
			case ')':
				if(stack != ""){
					list.add(stack);
					stack = "";
				}
				list.add(exp.substring(idx, idx+1));
				stringList.add(exp.substring(idx, idx+1));
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
						double value = Double.parseDouble(str);
						list.set(idx, new Constant(value));
						find = true;
					}catch(NumberFormatException e){
						
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
					if(depth == depthMax){
						if(priority >= 0){
							if(b != null){
								if(str.equals("abs")){
									list.set(idx, new Abs(b));
									list.remove(idx+1);
									priority = -1;
									break;
								}								
							}
						}
						if(priority >= 1){
							if(a != null && b != null){
								if(str.equals("^")){
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
									list.set(idx, new Inv(b));
									list.remove(idx+1);
									priority = -1;
									break;
								}								
							}
							if(a != null && b != null){
								if(str.equals("*")){
									list.set(idx-1, new Mul(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}
								if(str.equals("/")){
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
									list.set(idx-1, new Add(a,b));
									list.remove(idx);list.remove(idx);
									priority = -1;
									break;
								}						
								if(str.equals("-")){
									list.set(idx-1, new Sub(a,b));
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
	
	public boolean isValid()
	{
		return root != null;
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
}
