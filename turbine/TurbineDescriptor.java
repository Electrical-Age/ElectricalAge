package mods.eln.turbine;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.TurbineThermalProcess;

public class TurbineDescriptor extends TransparentNodeDescriptor{

	public TurbineDescriptor(String name,String modelName,String description,
			FunctionTable TtoU,
			FunctionTable TtoP,
			FunctionTable PoutToPin,
			double nominalDeltaT, double nominalU,double nominalP,double nominalPowerLost,
			double electricalRs,double electricalRp,double electricalC,
			double thermalC,double DeltaTForInput,
			double powerOutPerDeltaU
			) 
	{
		super(name, TurbineElement.class, TurbineRender.class);
		double nominalEff =  Math.abs(1 - (0 + PhysicalConstant.Tref)/(nominalDeltaT + PhysicalConstant.Tref));
		this.TtoU = TtoU;
		this.TtoP = TtoP;
		this.PoutToPin = PoutToPin;
		this.nominalDeltaT = nominalDeltaT;
		this.nominalU = nominalU; 
		this.nominalP = nominalP;
		this.thermalC = thermalC;
		this.thermalRs = DeltaTForInput/(nominalP / nominalEff);
		this.thermalRp = nominalDeltaT/nominalPowerLost;
		this.electricalRs = electricalRs;
		this.electricalRp = electricalRp;
		this.electricalC = electricalC;
		this.powerOutPerDeltaU = powerOutPerDeltaU;
		
		obj = Eln.obj.getObj(modelName);
		if(obj != null)
		{
			main = obj.getPart("main");
		}
	}
	Obj3D obj;
	Obj3DPart main;
	
	public double powerOutPerDeltaU;
	public FunctionTable TtoU;
	public FunctionTable TtoP;
	public FunctionTable PoutToPin;
	public double nominalDeltaT,nominalU; 
	double nominalP;
	public double thermalC,thermalRs,thermalRp;
	
	double electricalRs,electricalRp,electricalC;
	/*
	public void applyTo(TurbineThermalProcess turbine)
	{
		turbine.TtoU = TtoU;
		turbine.nominalDeltaT = nominalDeltaT;
		turbine.nominalU = nominalU;
		turbine.PintoPout = PintoPout;
	}*/
	public void applyTo(ThermalLoad load)
	{
		load.C = thermalC;
		load.Rp = thermalRp;
		load.Rs = thermalRs;	
	}
	
	public void applyTo(ElectricalLoad load,boolean grounded)
	{
		
		if(grounded) load.setAll(electricalRs, electricalRs, electricalC);
		else load.setAll(electricalRs, electricalRp, electricalC);
	}
	
	void draw()
	{
		
		//GL11.glTranslatef(0f, 0.5f, 0f);
		//GL11.glScalef(1f, 2f, 1f);
		if(main != null) main.drawList();
	}

}
