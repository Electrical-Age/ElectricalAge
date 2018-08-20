package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;

public class DielectricItem extends GenericItemUsingDamageDescriptor {

    public double uNominal;

    public DielectricItem(String name, double uNominal) {
        super(name);
        this.uNominal = uNominal;
    }
}
