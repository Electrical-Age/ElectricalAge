package mods.eln.sim;

import mods.eln.node.NodeBlockEntity;
import net.minecraft.nbt.NBTTagCompound;



public class ThermalLoad{
	
	public double Tc,Rp,Rs,C,PcTemp,Pc,Prs,Psp,PrsTemp = 0,PspTemp = 0;

	public ThermalLoad()
	{
		setHighImpedance();
    	Tc = 0;
    	PcTemp = 0;
    	Pc = 0;
    	Prs = 0;
    	Psp = 0;
	}
	public ThermalLoad(double Tc,double Rp,double Rs,double C)
	{
		this.Tc = Tc;
		this.Rp = Rp;
		this.Rs = Rs;
		this.C = C;
		PcTemp = 0;
	}
	
	public void setRsByTao(double tao)
	{
		Rs = tao/C;
	}
	
	public void setHighImpedance() {
		// TODO Auto-generated method stub
		Rs = 1000000000.0;
		C = 1;
		Rp = 1000000000.0;
	}
	
	public static final ThermalLoad externalLoad = new ThermalLoad(0, 0, 0, 0);

	public void setRp(double Rp) {
		// TODO Auto-generated method stub
		this.Rp = Rp;
	}
	
	
	public double getPower()
	{
		return (Prs + Math.abs(Pc) + Tc/Rp + Psp)/2;
	} 	
	
	public void set(double Rs,double Rp,double C)
	{
		this.Rp = Rp;
		this.Rs = Rs;
		this.C = C;	
	}
	
	public static void moveEnergy(double energy,double time,ThermalLoad from,ThermalLoad to)
	{
		double I = energy/time;
		double absI = Math.abs(I);
		from.PcTemp -= I;
		to.PcTemp += I;
		from.PspTemp += absI;
		to.PspTemp += absI;
	}
	public static void movePower(double power,ThermalLoad from,ThermalLoad to)
	{
		double absI = Math.abs(power);
		from.PcTemp -= power;
		to.PcTemp += power;
		from.PspTemp += absI;
		to.PspTemp += absI;
	}
	public void movePowerTo(double power)
	{
		double absI = Math.abs(power);
		PcTemp += power;
		PspTemp += absI;
	}
	public double getT() {
		// TODO Auto-generated method stub
		return Tc;
	}
}