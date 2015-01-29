package mods.eln.item;

import org.lwjgl.opengl.GL11;

public class EntitySensorFilterDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

	public Class entityClass;

	float r, g, b;

	public EntitySensorFilterDescriptor(String name, Class entityClass, float r, float g, float b) {
		super(name);
		this.entityClass = entityClass;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public void glColor() {
		GL11.glColor3f(r, g, b);
	}

    public void glColor(float intensity) {
        GL11.glColor3f(r * intensity, g * intensity, b * intensity);
    }

    public void glInverseColor(float intensity) {
        GL11.glColor3f(1.0f - r * intensity, 1 - 0f - g * intensity, 1.0f - b * intensity);
    }
}
