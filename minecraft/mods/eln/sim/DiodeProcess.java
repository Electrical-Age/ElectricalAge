package mods.eln.sim;

import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;



public class DiodeProcess implements IProcess {
	ElectricalLoad anodeLoad, catodeLoad;
	public  IFunction IfU;
	
//	public double Imax;
	public DiodeProcess(ElectricalLoad anodeLoad,ElectricalLoad catodeLoad )
	{
		this.anodeLoad = anodeLoad;
		this.catodeLoad = catodeLoad;
	}

	
	
	
	@Override
	public void process(double time) {
		double I = IfU.getValue(anodeLoad.Uc - catodeLoad.Uc);
		ElectricalLoad.moveCurrent(I, anodeLoad, catodeLoad);
	}
	
	public double getDissipatedPower()
	{
		double U = anodeLoad.Uc - catodeLoad.Uc;
		double I = IfU.getValue(U);
		return Math.abs(U*I);
	}

}


/*
package eln.sim;

import eln.misc.FunctionTable;



public class DiodeProcess implements IProcess {
	ElectricalLoad anodeLoad, catodeLoad;
	//public  FunctionTable udFonction;
	//public double UdNominal,IdNominal;
	public double UdDrop;
	public DiodeProcess(ElectricalLoad anodeLoad,ElectricalLoad catodeLoad )
	{
		this.anodeLoad = anodeLoad;
		this.catodeLoad = catodeLoad;
	//	this.udFonction = udFonction;
	}

	@Override
	public void process(double time) {
		double deltaV = anodeLoad.Uc - catodeLoad.Uc;
		if(UdDrop < deltaV)
		{
			double anodeC = anodeLoad.getC(),catodeC = catodeLoad.getC();
			deltaV-=UdDrop;
			double Qtot = anodeLoad.Uc*anodeC  + catodeLoad.Uc*catodeC;
			double Qanode = (UdDrop + Qtot/catodeC)*anodeC*catodeC/(catodeC+anodeC);
			double I = (anodeLoad.Uc*anodeC - Qanode)/time;
			ElectricalLoad.moveCurrent(I, anodeLoad, catodeLoad);
			
		}
	}
	

}
*/