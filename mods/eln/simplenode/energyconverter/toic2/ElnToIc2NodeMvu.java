package mods.eln.simplenode.energyconverter.toic2;

import mods.eln.Eln;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;

public class ElnToIc2NodeMvu extends EnergyConverterElnToOtherNode{

	public ElnToIc2NodeMvu() {
		conversionRatio = Eln.instance.getElnToIc2ConversionRatio();
		inStdVoltage = Eln.MVU;
		inPowerMax = Eln.MVP*3/4;
		otherOutMax = 128;
		energyBufferMax = otherOutMax/Eln.instance.getElnToIc2ConversionRatio()*20;
	}
	
	@Override
	public String getNodeUuid() {
		return getNodeUuidStatic();
	}
	public static String getNodeUuidStatic() {
		return "ElnToIc2_MVU";
	}

	
}
