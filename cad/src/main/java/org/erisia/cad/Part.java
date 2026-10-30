package org.erisia.cad;

/**
 * A movable piece of a model with its own frame (an entity model's ModelPart): a pivot (offset from its parent's
 * origin) and a rotation, applied as Minecraft's ModelPart does: z, then y, then x (rotationZYX), all in the authoring
 * frame (px, Y up). Solids in it are given in its frame. Animation turns parts; the checker sees the pose they're built in.
 */
public final class Part {
    public final String name;
    public final Part parent;
    public final double ox, oy, oz, rx, ry, rz;

    public Part(String name, Part parent, double ox, double oy, double oz, double rx, double ry, double rz) {
        this.name = name;
        this.parent = parent;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    /** Its frame in model space. */
    public Frame world() {
        Frame local = Frame.translation(ox, oy, oz).then(Frame.rotZ(rz)).then(Frame.rotY(ry)).then(Frame.rotX(rx));
        return parent == null ? local : parent.world().then(local);
    }

    /** Its path from the model's root, a/b/c (ModelPart.visit's partPath, without the leading root). */
    public String path() {
        return parent == null ? name : parent.path() + "/" + name;
    }

    @Override
    public String toString() {
        return path();
    }
}
