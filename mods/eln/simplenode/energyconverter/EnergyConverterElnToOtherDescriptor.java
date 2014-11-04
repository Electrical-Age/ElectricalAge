package mods.eln.simplenode.energyconverter;

import mods.eln.misc.DescriptorBase;

public class EnergyConverterElnToOtherDescriptor extends DescriptorBase {

	public EnergyConverterElnToOtherDescriptor(String key, ElnDescriptor eln) {
		super(key);
		this.eln = eln;
	}

	public ElnDescriptor eln;

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
	
	public static class OcDescriptor{
		public OcDescriptor(double outMax) {
			this.outMax = outMax;
		}
		public double outMax;

	}
}
