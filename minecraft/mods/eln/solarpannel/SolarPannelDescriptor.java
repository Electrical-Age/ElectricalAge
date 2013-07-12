package mods.eln.solarpannel;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;

public class SolarPannelDescriptor extends TransparentNodeDescriptor{

	boolean basicModel;
	public SolarPannelDescriptor(
			String name,
			GhostGroup ghostGroup, int solarOffsetX,int solarOffsetY,int solarOffsetZ,
			//FunctionTable solarIfSBase,
			double electricalUmax,double electricalPmax,
			double electricalDropFactor,
			double alphaMin,double alphaMax
			
			) {
		super(name, SolarPannelElement.class,SolarPannelRender.class);
		this.ghostGroup = ghostGroup;

		electricalRs = 	electricalUmax*electricalUmax*electricalDropFactor
						/electricalPmax/2.0;
		this.electricalPmax = electricalPmax;
		this.solarOffsetX = solarOffsetX;
		this.solarOffsetY = solarOffsetY;
		this.solarOffsetZ = solarOffsetZ;
		this.alphaMax = alphaMax;
		this.alphaMin = alphaMin;
		basicModel = true;
		this.electricalUmax = electricalUmax;
	}
	double electricalUmax;
	double electricalPmax;
	public SolarPannelDescriptor(
			String name,
			GhostGroup ghostGroup, int solarOffsetX,int solarOffsetY,int solarOffsetZ,
			FunctionTable diodeIfUBase,
			FunctionTable solarIfSBase,
			double electricalUmax,double electricalImax,
			double electricalDropFactor,
			double alphaMin,double alphaMax
			
			) {
		super(name, SolarPannelElement.class,SolarPannelRender.class);
		this.ghostGroup = ghostGroup;
		this.diodeIfU = diodeIfUBase.duplicate(electricalUmax,electricalImax);
		electricalRs = 	electricalUmax*electricalImax*electricalDropFactor
						/electricalImax/electricalImax/2.0;
	//	this.efficiency = efficiency;
		this.solarIfS = solarIfSBase.duplicate(1.0,electricalImax);
		this.solarOffsetX = solarOffsetX;
		this.solarOffsetY = solarOffsetY;
		this.solarOffsetZ = solarOffsetZ;
		this.alphaMax = alphaMax;
		this.alphaMin = alphaMin;
		basicModel = false;
	}
	int solarOffsetX, solarOffsetY, solarOffsetZ;
	double alphaMin, alphaMax;
	//double efficiency;
	double electricalRs;
	IFunction diodeIfU;
	FunctionTable solarIfS;
	private GhostGroup ghostGroup;
	
	public GhostGroup getGhostGroup() {
		return ghostGroup;
	}

	
	public void applyTo(ElectricalLoad load,boolean grounded)
	{
		load.setRs(electricalRs);
		load.setMinimalC(Eln.simulator);
		load.grounded(grounded);
	}
	
	public void applyTo(DiodeProcess diode)
	{
		diode.IfU = diodeIfU;
	}
	
	public double alphaTrunk(double alpha)
	{
		if(alpha > alphaMax) return alphaMax;
		if(alpha < alphaMin) return alphaMin;
		return alpha;
	}
}
