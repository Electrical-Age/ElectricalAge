package mods.eln.sixnode.electricalcable;

import java.util.List;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalCableDescriptor extends SixNodeDescriptor  {

	double electricalNominalRs;
	public double electricalNominalVoltage, electricalNominalPower, electricalNominalPowerDropFactor;
	public boolean signalWire;
	public ElectricalCableDescriptor(String name, CableRenderDescriptor render, String description, boolean signalWire) {
		super(name, ElectricalCableElement.class, ElectricalCableRender.class);
	
		this.description = description;
		this.render = render;
		this.signalWire = signalWire;
	}

	public void setPhysicalConstantLikeNormalCable(
			double electricalNominalVoltage, double electricalNominalPower, double electricalNominalPowerDropFactor,
			double electricalMaximalVoltage, double electricalMaximalPower,
			double electricalOverVoltageStartPowerLost,
			double thermalWarmLimit, double thermalCoolLimit,
			double thermalNominalHeatTime, double thermalConductivityTao			
			) {
		this.electricalNominalVoltage = electricalNominalVoltage;
		this.electricalNominalPower = electricalNominalPower;
		this.electricalNominalPowerDropFactor = electricalNominalPowerDropFactor;
		
		this.thermalWarmLimit = thermalWarmLimit;
		this.thermalCoolLimit = thermalCoolLimit;
		this.electricalMaximalVoltage = electricalMaximalVoltage;
		
		electricalRp = 1000000000.0;
		double electricalNorminalI = electricalNominalPower / electricalNominalVoltage;
		electricalNominalRs = (electricalNominalPower * electricalNominalPowerDropFactor) / electricalNorminalI / electricalNorminalI / 2;
		electricalRs = electricalNominalRs;
		electricalC = Eln.simulator.getMinimalElectricalC(electricalNominalRs, electricalRp);
	
		electricalMaximalI = electricalMaximalPower / electricalNominalVoltage;
		double thermalMaximalPowerDissipated = electricalMaximalI * electricalMaximalI * electricalRs * 2;
		thermalC = thermalMaximalPowerDissipated * thermalNominalHeatTime / (thermalWarmLimit);
		thermalRp = thermalWarmLimit / thermalMaximalPowerDissipated;
		thermalRs = thermalConductivityTao / thermalC / 2;
		
		Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);
		
		electricalRsMin = electricalNominalRs;
		electricalRsPerCelcius = 0;
		
		dielectricBreakOhmPerVolt = 0.95;
		dielectricBreakOhm = electricalMaximalVoltage * electricalMaximalVoltage / electricalOverVoltageStartPowerLost;
		dielectricVoltage = electricalMaximalVoltage;
		dielectricBreakOhmMin = dielectricBreakOhm;
	}	
	public double electricalMaximalVoltage;
	public double electricalRp = Double.POSITIVE_INFINITY, electricalRs = Double.POSITIVE_INFINITY, electricalC = 1;
	public double thermalRp = 1, thermalRs = 1, thermalC = 1;
	public double thermalWarmLimit = 100,thermalCoolLimit = -100;
	double electricalMaximalI;
	public double electricalRsMin = 0;
	public double electricalRsPerCelcius = 0;
	
	public double dielectricBreakOhmPerVolt = 0;
	public double dielectricBreakOhm = Double.POSITIVE_INFINITY;
	public double dielectricVoltage = Double.POSITIVE_INFINITY;
	public double dielectricBreakOhmMin = Double.POSITIVE_INFINITY;
	
	String description = "todo cable";
	
	public CableRenderDescriptor render;
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
		
		if(signalWire) {
			Data.addSignal(newItemStack());
		}
	}
	
	public void applyTo(ElectricalLoad electricalLoad,double rsFactor) {
		electricalLoad.setRs(electricalRs*rsFactor);				
	}	
	public void applyTo(ElectricalLoad electricalLoad, boolean grounded) {
		applyTo(electricalLoad, 1);
	}	
	
	public void applyTo(Resistor resistor) {
		resistor.setR(electricalRs);
	}
	
	public void applyTo(ThermalLoad thermalLoad) {
		thermalLoad.Rs = this.thermalRs;
		thermalLoad.C = this.thermalC;
		thermalLoad.Rp = this.thermalRp;		
	}


	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		if(signalWire) {
			list.add("This cable is adapted to");
			list.add("transport signals quickly.");
			list.add("A signal is electrical information");
			list.add("that must be between 0V and " + Eln.SVU + "V.");
			list.add("Don't try to transport power.");
			
			/*String lol = "";
			for(int idx = 0; idx < 15; idx++) {
				if(idx < 10) {
					lol += "\u00a7" + idx + "" +  idx;
				}
				else {
					lol += "\u00a7" + "abcdef".charAt(idx - 10) + "abcdef".charAt(idx - 10);
				}
			}
			list.add(lol);*/
		}
		else {
			//list.add("Low resistor => low power lost");
			list.add("Nominal usage ->");
			list.add("  Voltage : " + (int)electricalNominalVoltage + " V");
			list.add("  Current : " + (int)(electricalNominalPower / electricalNominalVoltage) + " A");
			list.add("  Power : " + (int)electricalNominalPower + " W");
			list.add("  Power lost : " + (int)(electricalNominalPowerDropFactor * electricalNominalPower) + " W/Block");
			list.add(Utils.plotOhm("Serial resistor :", electricalNominalRs * 2));
		}
	}

	
	public int getNodeMask() {
		if(signalWire)
			return NodeBase.maskElectricalGate;
		else 
			return NodeBase.maskElectricalPower;
	}

	public static CableRenderDescriptor getCableRender(ItemStack cable) {
		if(cable == null) return null;
		GenericItemBlockUsingDamageDescriptor desc = ElectricalCableDescriptor.getDescriptor(cable);
		if(desc instanceof ElectricalCableDescriptor)
			return ((ElectricalCableDescriptor)desc).render;
		else 
			return  null;
	}

	public void bindCableTexture() {
		this.render.bindCableTexture();
	}
}
