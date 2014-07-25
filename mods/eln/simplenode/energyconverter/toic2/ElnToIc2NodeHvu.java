package mods.eln.simplenode.energyconverter.toic2;

import mods.eln.Eln;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;

public class ElnToIc2NodeHvu extends EnergyConverterElnToOtherNode{

	public ElnToIc2NodeHvu() {
		conversionRatio = Eln.instance.getElnToIc2ConversionRatio();
		inStdVoltage = Eln.HVU;
		inPowerMax = Eln.HVP*3/4;
		otherOutMax = 512;
		energyBufferMax = otherOutMax/Eln.instance.getElnToIc2ConversionRatio()*20;

	}
	
	@Override
	public String getNodeUuid() {
		return getNodeUuidStatic();
	}
	public static String getNodeUuidStatic() {
		return "ElnToIc2_HVU";
	}

	
}
