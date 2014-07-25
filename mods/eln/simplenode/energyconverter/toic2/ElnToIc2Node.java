package mods.eln.simplenode.energyconverter.toic2;

import mods.eln.Eln;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;

public class ElnToIc2Node extends EnergyConverterElnToOtherNode{
	
	ElnToIc2Descriptor descriptor;
	
	
	@Override
	public void initialize() {
		descriptor = (ElnToIc2Descriptor) getDescriptor();
		descriptor.applyTo(this);
		super.initialize();
	}
	
	@Override
	public String getNodeUuid() {
		return getNodeUuidStatic();
	}
	public static String getNodeUuidStatic() {
		return "ElnToIc2";
	}

	
}
