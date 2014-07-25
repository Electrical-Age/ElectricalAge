package mods.eln.simplenode.energyconverter.toic2;

import mods.eln.Eln;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;

public class ElnToIc2NodeLvu extends EnergyConverterElnToOtherNode{

	public ElnToIc2NodeLvu() {
		conversionRatio = Eln.instance.getElnToIc2ConversionRatio();
		inStdVoltage = Eln.LVU;
		inPowerMax = Eln.LVP*3/4;
		otherOutMax = 32;
		energyBufferMax = otherOutMax/Eln.instance.getElnToIc2ConversionRatio()*20*2;

	}
	
	@Override
	public String getNodeUuid() {
		return getNodeUuidStatic();
	}
	public static String getNodeUuidStatic() {
		return "ElnToIc2_LVU";
	}

	
}
