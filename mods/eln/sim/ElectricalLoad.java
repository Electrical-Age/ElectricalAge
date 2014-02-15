package mods.eln.sim;

import java.util.ArrayList;

import mods.eln.node.NodeBlockEntity;
import net.minecraft.nbt.NBTTagCompound;


public class ElectricalLoad{
	
	public double Uc,Ic,IcTemp,Irs,IrsTemp,IrsPow2,IrsPow2Temp,invC,invRp;
	public double Isp;
	private double Rp,Rs,C;
	private boolean simplifyAuthorized= false;
	
	ArrayList<ElectricalConnection> electricalConnections = new ArrayList<ElectricalConnection>(4);
	
	public void setSimplifyAuthorized(boolean value){
		simplifyAuthorized = value;
	}
	
	public boolean getSimplifyAuthorized(){
		return simplifyAuthorized;
	}
	
	public double getRp() {
		return Rp;
	}
	public double getRs() {
		return Rs;
	}
	public double getC() {
		return C;
	}
	
	
	
	
	/*
	public double getC() {
		return C;
	}
	public double getInvC() {
		return invC;
	}
	public double getRp() {
		return Rp;
	}
	public double getIc() {
		return Ic;
	}
	public double getIcTemp() {
		return IcTemp;
	}
	public double getInvRp() {
		return invRp;
	}
	public double getIrs() {
		return Irs;
	}
	public double getIrsPow2() {
		return IrsPow2;
	}
	public double getIrsPow2Temp() {
		return IrsPow2Temp;
	}
	public double getIrsTemp() {
		return IrsTemp;
	}
	public double getRs() {
		return Rs;
	}
	public double getUc() {
		return Uc;
	}*/
	
	public void setC(double c) {
		C = c;
		invC = 1/c;
	}
	public void setRs(double rs) {
		Rs = rs;
	}
	public void setRp(double rp) {
		Rp = rp;
		invRp = 1/rp;
	}
	
	public ElectricalLoad()
	{
		highImpedance();
		setC(1);
    	Uc = 0;
    	IcTemp = 0;
    	Irs = 0;
    	IrsTemp = 0;
    	Ic = 0;
    	IrsPow2 = 0;
    	IrsPow2Temp = 0;
	}
	public ElectricalLoad(double Uc,double Rp,double Rs,double C)
	{
		this.Uc = Uc;
		setRp(Rp);
		setRs(Rs);
		setC(C);
		IcTemp = 0;
		Irs = 0;
		IrsTemp = 0;
		Ic = 0;
    	IrsPow2 = 0;
    	IrsPow2Temp = 0;
	}
	
	
	
	public void setMinimalC(Simulator simulator)
	{
		setC(3/(Rs * simulator.electricalHz));
	}
	public void highImpedance()
	{
		setRs(1000000000.0);
	//	setC(0.01);
		setRp(1000000000.0);
	}
	public void groundedEnable()
	{
		setRp(Rs * 2);
	}
	public void groundedDisable()
	{
		setRp(1000000000.0);
	}
	
	public void grounded(boolean enable)
	{
		if(enable) groundedEnable();
		else       groundedDisable();
	}
	
	public double getCurrent()
	{
		return (Irs + Math.abs(Ic) + Math.abs(Uc)/getRp() + Isp)/2;
	} 	
	
	
	
	static public void moveCurrent(double i,ElectricalLoad from,ElectricalLoad to)
	{
    	double absi,iPow2;
    	  	
    	absi = Math.abs(i);
    	//iPow2 = i*i;
    	
    	to.IcTemp += i;
    	from.IcTemp -= i;
    	
    	to.Isp += absi;
    	from.Isp += absi;
    	/*
    	to.IrsTemp += absi;
    	from.IrsTemp += absi;
    	
    	to.IrsPow2Temp += iPow2;
    	from.IrsPow2Temp += iPow2;	*/
	}
	
	public void moveCurrentTo(double i)
	{	  	
    	IcTemp += i;
    	Isp += Math.abs(i);
    	/*IrsTemp += Math.abs(i);
    	IrsPow2Temp += i*i;
		*/
	}
	
	public double getRpPower()
	{
		return Uc*Uc/Rp;
	}
	public void setAll(double Rs,double Rp,double C)
	{
		setRs(Rs);
		setRp(Rp);
		setC(C);
	}
	public static final ElectricalLoad groundLoad = new ElectricalLoad(0,1000000000.0,1000000000.0,1000000000.0);
	
	
	public void weakPullDown(double tao)
	{
		setRp(tao/getC());
	}
	
	
	private boolean tag = false;
	
	public boolean isTaged() {
		return tag;
	}

	public void resetTag() {
		tag = false;
	}
	public void setTag(){
		tag = true;
	}

	private boolean tag2 = false;
	
	public boolean isTaged2() {
		return tag2;
	}

	public void resetTag2() {
		tag2 = false;
	}
	public void setTag2(){
		tag2 = true;
	}

	public double energyStored() {
		// TODO Auto-generated method stub
		return Uc*Uc*C/2;
	}
}