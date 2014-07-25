package mods.eln.simplenode.energyconverter.toic2;

import mods.eln.Eln;
import mods.eln.misc.DescriptorBase;

public class ElnToIc2Descriptor extends DescriptorBase {

	public ElnToIc2Descriptor(String key, double nominalU, double maxP, double otherOutMax) {
		super(key);
		this.nominalU = nominalU;
		this.maxP = maxP;
		this.otherOutMax = otherOutMax;
	}

	double nominalU;
	double maxP;
	double otherOutMax;

	void applyTo(ElnToIc2Node node) {
		node.conversionRatio = Eln.instance.getElnToIc2ConversionRatio();
		node.inStdVoltage = nominalU;
		node.inPowerMax = maxP;
		node.otherOutMax = otherOutMax;
		node.energyBufferMax = otherOutMax / node.conversionRatio * 20;
	}
}
