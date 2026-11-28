package mods.eln.compat.jade;

import mods.eln.node.voxel.JunctionPart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.sim.process.destruct.IBurnable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.List;
import java.util.Map;

/**
 * Jade's data for a voxel block: the lines of every device, pipe, cable and fitting in it, and the choice among them
 * on the client (Baughn 2026-11-28: the server sent only the one under the crosshair when it was asked, and Jade asks
 * a few times a second, so the tooltip named the last cable for a moment after the crosshair moved to the next, and
 * by its own pick, not the outline's). The client picks with VoxelNodeEntity.aimed, as the outline does, every time
 * the tooltip is built. No Jade classes here: the GameTests read it.
 */
public final class VoxelProbe {
    private VoxelProbe() {
    }

    /** The entries, under this key of the server data: a list of compounds. */
    public static final String KEY = "elnParts";
    /**
     * The most entries sent. A block rarely holds more (a 16-wire ribbon and a few devices); past it the others have
     * none, and the tooltip shows only the header for one of those until the server's next answer, whose first entry
     * is the one then under the crosshair.
     */
    public static final int MAX = 24;

    /** A device's or part's lines as they are sent: burnt out, only the cause and the repair in the headline. */
    public static ProbeLines lines(Map<String, String> m, IBurnable b, int headline) {
        ProbeLines p = ProbeLines.from(m, headline);
        if (b == null || !b.isBurnt()) return p;
        ProbeLines burnt = ProbeLines.of();
        burnt.headline(mods.eln.i18n.I18N.tr("Burnt out"), b.getBurnt().text);
        burnt.headline(mods.eln.i18n.I18N.tr("Repair with"), b.getBurntKind() == mods.eln.sim.process.destruct.Failure.Kind.CABLE
            ? mods.eln.i18n.I18N.tr("matching cable kit") : "1 " + mods.eln.sim.process.destruct.Repair.partName(b));
        burnt.addAllDetail(p);
        return burnt;
    }

    /** Lines as sent: key TAB value. */
    public static ListTag list(Map<String, String> m) {
        ListTag list = new ListTag();
        for (var e : m.entrySet()) list.add(StringTag.valueOf(e.getKey() + "\t" + e.getValue()));
        return list;
    }

    /** One thing's tooltip into tag: elnTitle (if any), and if it has lines elnProbe and elnHead, the ambient last. */
    public static void put(CompoundTag tag, String title, ProbeLines p, String ambient) {
        if (title != null) tag.putString("elnTitle", title);
        if (p.isEmpty()) return;
        if (ambient != null) p.detail(mods.eln.i18n.I18N.tr("Ambient"), ambient);
        tag.put("elnProbe", list(p));
        tag.putInt("elnHead", p.headlineCount());
    }

    /**
     * Every device and part of v as an entry (put, and its key: "slot" for a device; "id" (VoxelPart.id) and "box" (its
     * first box, by which the client knows it: it is not sent ids) for a part). first's entry first, then the devices
     * by slot, then the parts in order; MAX at most. No entry for a junction (never aimed at) or for one whose lines
     * can't be had this time (mid-change). Last, beyond MAX and with no key, the block's lines as a whole: for a
     * crosshair on none of them (the imitated block).
     */
    public static ListTag entries(VoxelNode v, VoxelNode.Aimed first, int headline, String ambient) {
        ListTag out = new ListTag();
        if (first != null && first.element() >= 0) add(out, v, first.element(), -1, headline, ambient);
        else if (first != null && first.part() >= 0) add(out, v, -1, first.part(), headline, ambient);
        for (int i = 0; i < v.sideElementList.length; i++)
            if (first == null || i != first.element()) add(out, v, i, -1, headline, ambient);
        for (int i = 0; i < v.parts.size(); i++)
            if (first == null || first.element() >= 0 || i != first.part()) add(out, v, -1, i, headline, ambient);
        // (the crosshair on none of them, the imitated block: the block's lines as a whole, as before)
        try {
            CompoundTag t = new CompoundTag();
            put(t, null, lines(v.getWaila(true, -1), null, headline), ambient);
            out.add(t);
        } catch (RuntimeException e) {
            // (mid-change: none this time)
        }
        return out;
    }

    private static void add(ListTag out, VoxelNode v, int slot, int part, int headline, String ambient) {
        if (out.size() >= MAX) return;
        CompoundTag t = new CompoundTag();
        try {
            if (slot >= 0) {
                var e = v.sideElementList[slot];
                if (e == null) return;
                t.putInt("slot", slot);
                put(t, v.nameOf(new VoxelNode.Aimed(slot, -1)), lines(e.getWaila(), e, headline), ambient);
            } else {
                VoxelPart p = part < v.parts.size() ? v.parts.get(part) : null;
                if (p == null || p instanceof JunctionPart) return;
                t.putInt("id", p.id);
                t.putIntArray("box", p.boxes().get(0));
                put(t, v.nameOf(new VoxelNode.Aimed(-1, part)),
                    lines(v.getWaila(true, part), p instanceof mods.eln.node.voxel.CablePart c ? v.burnableOf(c) : null, headline), ambient);
            }
        } catch (RuntimeException e) {
            return;   // (a device mid-change: no entry this time)
        }
        out.add(t);
    }

    /**
     * The entry for what is aimed at among parts (the client's own, or the node's), or null: the device's by its slot;
     * the part's by its id where the parts have ids (the server), else by its first box; aimed at nothing, the keyless
     * one.
     */
    public static CompoundTag choose(ListTag entries, VoxelNode.Aimed a, List<VoxelPart> parts) {
        boolean none = a.element() < 0 && a.part() < 0;
        VoxelPart p = a.element() < 0 && a.part() >= 0 && a.part() < parts.size() ? parts.get(a.part()) : null;
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag t = entries.getCompoundOrEmpty(i);
            if (none ? !t.contains("slot") && !t.contains("id")
                : a.element() >= 0 ? t.getIntOr("slot", -1) == a.element()
                : p != null && (p.id >= 0 ? t.getIntOr("id", -1) == p.id : java.util.Arrays.equals(t.getIntArray("box").orElse(null), p.boxes().get(0))))
                return t;
        }
        return null;
    }
}
