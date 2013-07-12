package mods.eln.electricalswitch;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ThermalLoadInitializer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;


public class ElectricalSwitchDescriptor extends SixNodeDescriptor{

	public ElectricalSwitchDescriptor(
			String name, CableRenderDescriptor cableRender,String objName,
			double nominalVoltage,double nominalPower,double nominalDropFactor,
			double maximalVoltage,double maximalPower,
			ThermalLoadInitializer thermal,
			boolean signalSwitch
			) {
		super(name, ElectricalSwitchElement.class,ElectricalSwitchRender.class);
		this.nominalVoltage = nominalVoltage;
		this.nominalPower = nominalPower;
		this.maximalPower = maximalPower;
		this.maximalVoltage = maximalVoltage;
		this.nominalDropFactor = nominalDropFactor;
		this.cableRender = cableRender;
		electricalRs = nominalVoltage*nominalVoltage / nominalPower * nominalDropFactor / 3;
		this.objName = objName;
		
		this.thermal = thermal;
		double I = maximalPower / nominalVoltage;
		thermal.setMaximalPower(I*I*electricalRs*3);
		this.signalSwitch = signalSwitch;
	}
	boolean signalSwitch;
	String objName;
	double electricalRs;
	ThermalLoadInitializer thermal;
	

	CableRenderDescriptor cableRender;
	
	double nominalVoltage, nominalPower,nominalDropFactor;
	double maximalVoltage, maximalPower;	
	public void applyTo(ElectricalLoad load)
	{
		load.setRs(electricalRs);
		load.setMinimalC(Eln.simulator);
	}
	public void applyTo(ElectricalResistor resistor,boolean state)
	{
		if(state)
		{
			resistor.setR(electricalRs);
		}
		else
		{
			resistor.highImpedance();
		}
	}
	
	Obj3D obj = null;
	public Obj3D getObj()
	{
		if(obj == null) obj = Eln.obj.getObj(objName);
		return obj;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		if(getObj() != null)
		{
			String objType = obj.getString("type");
			if(objType.equals("lever"))
			{
				obj.draw("case");

				Obj3DPart part = obj.getPart("lever");
					
				if(part != null) 
				{
					float switchDelta;			
					part.draw(part.getFloat("alphaOff"), 0, 0, 1);
				}
			}
		}
	}	
	public int getNodeMask()
	{
		if(signalSwitch)
			return Node.maskElectricalGate;
		else 
			return Node.maskElectricalPower;
	}
}
