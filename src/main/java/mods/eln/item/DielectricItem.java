package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;

public class DielectricItem extends GenericItemUsingDamageDescriptor{

	public DielectricItem(String name,double uNominal) {
		super(name);
		this.uNominal = uNominal;
	}
	
	public double uNominal;
}
