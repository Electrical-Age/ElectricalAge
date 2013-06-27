package mods.eln.diode;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;

import com.google.common.base.Function;


public class DiodeDescriptor extends SixNodeDescriptor{

	public DiodeDescriptor(
			String name,
			IFunction IfU,
			double Imax,
			ThermalLoadInitializer thermal,
			ElectricalCableDescriptor cable
			) {
		super(name, DiodeElement.class,DiodeRender.class);
		this.IfU = IfU;
		
		
		double Umax = 0;
		while(IfU.getValue(Umax) < Imax) Umax += 0.01;
		double Pmax = Umax * IfU.getValue(Umax);
		this.cable = cable;
		this.thermal = thermal;
		thermal.setMaximalPower(Pmax);
	}
	ElectricalCableDescriptor cable;
	String descriptor;
	IFunction IfU;
	
	ThermalLoadInitializer thermal;
	
	public void applyTo(DiodeProcess diode)
	{
		diode.IfU = IfU;
	}
	public void applyTo(ThermalLoad load)
	{
		thermal.applyTo(load);
		
	}
	public void applyTo(ElectricalLoad load)
	{
		cable.applyTo(load,false);
	}

}
