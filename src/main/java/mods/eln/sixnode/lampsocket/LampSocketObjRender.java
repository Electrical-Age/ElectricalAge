package mods.eln.sixnode.lampsocket;

/** A lamp socket's model (standard or suspended). 26.x port: the draw methods come back with P3 rendering. */
public interface LampSocketObjRender {
    void draw(LampSocketRender render, double distanceToPlayer);

    /** The item in hand or on the ground (1.12 draw(descriptor, type, distance) without the GUI branch). */
    void drawItem(LampSocketDescriptor descriptor);
}
