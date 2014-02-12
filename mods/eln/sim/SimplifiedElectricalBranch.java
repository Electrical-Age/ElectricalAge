package mods.eln.sim;

import java.util.ArrayList;

public class SimplifiedElectricalBranch implements IProcess {

	ElectricalLoad[] lList;
	ElectricalLoad L1,L2;	
	double Rs/*,Rp,invRp2*/,serialConductance;
	double i;
	public SimplifiedElectricalBranch(ElectricalLoad[] lList,
			ElectricalLoad lA, ElectricalLoad lB) {
		this.lList = lList;
		this.L1 = lA;
		this.L2 = lB;
	}

	@Override
	public void process(double time) {
    	double i,absi,iPow2;

    	i = (L2.Uc - L1.Uc)*serialConductance;
    	absi = Math.abs(i);
    	iPow2 = i*i;
    	
    	L1.IcTemp += i;
    	L2.IcTemp -= i;
    	
    	L1.IrsTemp += absi;
    	L2.IrsTemp += absi;
    	
    	L1.IrsPow2Temp += iPow2;
    	L2.IrsPow2Temp += iPow2;
    	
    	
    	this.i = i;
	}

	public void stepA() {
    	double i,absi,iPow2;

    	i = (L2.Uc - L1.Uc)*serialConductance;
    	absi = Math.abs(i);
    	iPow2 = i*i;
    	
    	L1.IcTemp += i/* - L1.Uc*invRp2*/;
    	L2.IcTemp -= i/* + L2.Uc*invRp2*/;
	}
	
	public void stepB() {
    	double i,absi,iPow2;

    	i = (L2.Uc - L1.Uc)*serialConductance;
    	absi = Math.abs(i);
    	iPow2 = i*i;
    	
    	L1.IcTemp += i /*- L1.Uc*invRp2*/;
    	L2.IcTemp -= i /*+ L2.Uc*invRp2*/;
    	
    	L1.IrsTemp += absi;
    	L2.IrsTemp += absi;
    	
    	L1.IrsPow2Temp += iPow2;
    	L2.IrsPow2Temp += iPow2;
    	
    	
    	this.i = i;		
	}
	public void preStep(){
		Rs = L1.getRs() + L2.getRs();
		//Rp = 0;
		for (ElectricalLoad l : lList) {
			Rs += l.getRs()*2;
		//	Rp += 1.0/l.getRp();
		}	
	/*	invRp2 = Rp*2;
		Rp = 1/Rp;*/
		serialConductance = 1/ Rs;
	}
	
	public void postStep(){
		double u = L1.Uc;
		double absi2 = Math.abs(i)*2;
		double uDelta;
		u += i*L1.getRs();
		for (ElectricalLoad l : lList) {
			uDelta = i*l.getRs();
			u += uDelta;
			l.Uc = u;
	    	l.Irs = absi2;
	    	l.Ic = 0;
	    	l.IrsPow2 = i*i*2;
			u += uDelta;
			
		}
	}

	public double energyStored() {
		double e = 0;
		for (ElectricalLoad l : lList) {
			e += l.energyStored();
		}
		return e;
	}

	


}
