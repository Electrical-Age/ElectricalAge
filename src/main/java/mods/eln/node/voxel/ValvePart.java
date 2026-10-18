package mods.eln.node.voxel;

import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A valve in a pipe (notes/pipes/DESIGN.md section 5: valves scale an edge's conductance): it sits at a point inside
 * the block, between two pipes that end there (the pipe it was fitted to, cut in two). Open, it passes what a short
 * piece of pipe would; shut, nothing. A right-click turns it.
 */
public final class ValvePart extends VoxelPart {
    public static final String KIND = "valve";
    public final int x, y, z, axis, bore;
    public boolean open = true;

    public ValvePart(int x, int y, int z, int axis, int bore) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.axis = axis;
        this.bore = bore;
    }

    @Override
    public String kind() {
        return KIND;
    }

    /** The body (a cube a little wider than the bore) and the stem with its handwheel on top (or on the side, for a
     *  vertical pipe). */
    @Override
    public List<int[]> boxes() {
        int h = bore / 2 + 1;
        List<int[]> out = new ArrayList<>();
        out.add(clamp(new int[]{x - h, y - h, z - h, x + h, y + h, z + h}));
        if (axis == 1) out.add(clamp(new int[]{x + h, y - 1, z - 1, x + h + 2, y + 1, z + 1}));
        else out.add(clamp(new int[]{x - 1, y + h, z - 1, x + 1, y + h + 2, z + 1}));
        return out;
    }

    static int[] clamp(int[] b) {
        for (int i = 0; i < 6; i++) b[i] = Math.max(0, Math.min(16, b[i]));
        return b;
    }

    @Override
    public void write(CompoundTag t) {
        t.putIntArray("at", new int[]{x, y, z, axis, bore});
        t.putBoolean("open", open);
    }

    static ValvePart read(CompoundTag t) {
        int[] a = t.getIntArray("at").orElse(new int[]{8, 8, 8, 0, 4});
        ValvePart v = new ValvePart(a[0], a[1], a[2], a[3], a[4]);
        v.open = t.getBooleanOr("open", true);
        return v;
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(x);
        out.writeByte(y);
        out.writeByte(z);
        out.writeByte(axis | (open ? 0x10 : 0));
        out.writeByte(bore);
    }

    static ValvePart deserialize(DataInputStream in) throws IOException {
        int x = in.readByte(), y = in.readByte(), z = in.readByte(), a = in.readByte(), b = in.readByte();
        ValvePart v = new ValvePart(x, y, z, a & 3, b);
        v.open = (a & 0x10) != 0;
        return v;
    }
}
