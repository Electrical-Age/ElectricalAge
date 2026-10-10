package mods.eln.item;


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

    // 26.x port: glColor/glInverseColor removed (client rendering; redone in P3)
}
