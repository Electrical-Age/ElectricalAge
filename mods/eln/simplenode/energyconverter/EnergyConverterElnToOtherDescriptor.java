package mods.eln.simplenode.energyconverter;

import mods.eln.Eln;
import mods.eln.misc.DescriptorBase;

public class EnergyConverterElnToOtherDescriptor extends DescriptorBase {

	public EnergyConverterElnToOtherDescriptor(String key, ElnDescriptor eln,Ic2Descriptor ic2) {
		super(key);
		this.eln = eln;
		this.ic2 = ic2;

	}

	public ElnDescriptor eln;
	public Ic2Descriptor ic2;

	void applyTo(EnergyConverterElnToOtherNode node) {
		node.inStdVoltage = eln.nominalU;
		node.inPowerMax = eln.maxP;
		node.energyBufferMax = eln.maxP*2;
		/*node.otherOutMax = otherOutMax;
		node.energyBufferMax = otherOutMax / node.conversionRatio * 20;*/
	}
	
	public static class ElnDescriptor{
		public ElnDescriptor(double nominalU,double maxP) {
			this.nominalU = nominalU;
			this.maxP = maxP;

		}

		public double nominalU;
		public double maxP;


	}
	
	public static class Ic2Descriptor{
		public Ic2Descriptor(double outMax,int tier) {
			this.outMax = outMax;
			this.tier = tier;
		}
		public double outMax;
		public int tier;

	}
}
