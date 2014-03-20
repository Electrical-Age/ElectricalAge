package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;

public class EntitySensorFilterDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

	public Class entityClass;

	public EntitySensorFilterDescriptor(String name,Class entityClass) {
		super(name);
		this.entityClass = entityClass;
	}

}
