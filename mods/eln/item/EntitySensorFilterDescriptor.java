package mods.eln.item;

import org.lwjgl.opengl.GL11;

import mods.eln.generic.GenericItemUsingDamageDescriptor;

public class EntitySensorFilterDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

	public Class entityClass;

	float r, g, b;
	public EntitySensorFilterDescriptor(String name,Class entityClass,float r,float g,float b) {
		super(name);
		this.entityClass = entityClass;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public void glColor() {
		GL11.glColor3f(r, g, b);
	}

}
