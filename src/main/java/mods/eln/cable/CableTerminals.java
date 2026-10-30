package mods.eln.cable;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 26.x: which faces of a device carry a floor-level terminal plate (docs/cad.md, "Cable terminals"). A floor
 * cable (six-node electrical cable on YN) that connects to such a face draws a cable lug at its end: the cable turns up
 * against the face and a copper palm covers the plate's middle stud, {@link #CONDUCTOR_HEIGHT_PX} above the floor.
 * Cable-to-cable joints and devices without terminals are unchanged.
 * <p>
 * Opt in with one line in the descriptor's constructor: {@code cableTerminals = CableTerminals.FRONT_AND_BACK;}
 * ({@code TransparentNodeDescriptor} has the field). A transparent node
 * render can also override {@code TransparentNodeElementRender.cableKnuckleHeight(face)}.
 */
public final class CableTerminals {
    /** Centre of a terminal plate's conductors above the floor (px): studs 1.75..2.75 px (converter, steam turbine). */
    public static final float CONDUCTOR_HEIGHT_PX = 2.25f;

    static final int M_FRONT = 1, M_BACK = 2, M_FLANKS = 4;

    /** The front face only (the energy converters: EA side). */
    public static final CableTerminals FRONT = new CableTerminals(M_FRONT, CONDUCTOR_HEIGHT_PX);
    /** The back face only (the electrical anvil: its 800 V plate behind the bench). */
    public static final CableTerminals BACK = new CableTerminals(M_BACK, CONDUCTOR_HEIGHT_PX);
    /** Front and back (the steam turbines). */
    public static final CableTerminals FRONT_AND_BACK = new CableTerminals(M_FRONT | M_BACK, CONDUCTOR_HEIGHT_PX);
    /** The two flanks, left and right of the front (electric heaters, tar sand separators). */
    public static final CableTerminals FLANKS = new CableTerminals(M_FLANKS, CONDUCTOR_HEIGHT_PX);
    /** The front and the two flanks (the submersible pumps: signal in front, power on the flanks). */
    public static final CableTerminals FRONT_AND_FLANKS = new CableTerminals(M_FRONT | M_FLANKS, CONDUCTOR_HEIGHT_PX);
    /** Every horizontal face. */
    public static final CableTerminals ALL_SIDES = new CableTerminals(M_FRONT | M_BACK | M_FLANKS, CONDUCTOR_HEIGHT_PX);

    private final int faces;
    /** Conductor centre above the floor, px. */
    public final float heightPx;

    public CableTerminals(int faces, float heightPx) {
        this.faces = faces;
        this.heightPx = heightPx;
    }

    /** Conductor height (px) on the device face FACE when the device's front is FRONT; 0 = no terminal there. */
    public float heightAt(Direction front, Direction face) {
        if (front == null || face == null || face.isY()) return 0;
        int m;
        if (face == front) m = M_FRONT;
        else if (face == front.back()) m = M_BACK;
        else if (front.isY()) return 0; // a device facing up/down has no defined flanks
        else m = M_FLANKS;
        return (faces & m) != 0 ? heightPx : 0;
    }

    /** Client: the conductor height (px) on face FACE of the device block entity BE, 0 = none or not a device. */
    public static float heightAt(BlockEntity be, Direction face) {
        if (be instanceof TransparentNodeEntity t) {
            return t.elementRender == null ? 0 : t.elementRender.cableKnuckleHeight(face);
        }
        return 0;
    }
}
