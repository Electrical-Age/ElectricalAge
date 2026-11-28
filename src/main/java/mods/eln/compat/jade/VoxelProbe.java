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
     * The most entries sent: two 16-wire ribbons and their devices fit. Past it, the ones nearest the asker's line of
     * sight are sent; one without an entry shows the block's lines as a whole until the server's next answer, whose
     * first entry is the one then under the crosshair.
     */
    public static final int MAX = 64;

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
     * first box, by which the client knows it: it is not sent ids) for a part), in the order the asker's line of sight
     * favours them (VoxelPick.ranked from eye through hit, in the world: the one aimed at, then the nearest), max at
     * most: so the neighbours of the one aimed at are there when the crosshair moves on. No entry for a junction (never
     * aimed at) or for one whose lines can't be had this time (mid-change). Last, beyond max and with no key, the
     * block's lines as a whole: for a crosshair on none of them (the imitated block).
     */
    public static ListTag entries(VoxelNode v, net.minecraft.world.phys.Vec3 eye, net.minecraft.world.phys.Vec3 hit, int headline, String ambient, int max) {
        ListTag out = new ListTag();
        for (VoxelNode.Aimed a : mods.eln.node.voxel.VoxelPick.ranked(v.scene(), v.coordonate.pos(), eye, hit)) {
            if (out.size() >= max) break;
            add(out, v, a.element(), a.part(), headline, ambient);
        }
        try {
            CompoundTag t = new CompoundTag();
            put(t, null, lines(v.getWaila(true, -1), null, headline), ambient);
            out.add(t);
        } catch (RuntimeException e) {
            // (mid-change: none this time)
        }
        return out;
    }

    public static ListTag entries(VoxelNode v, net.minecraft.world.phys.Vec3 eye, net.minecraft.world.phys.Vec3 hit, int headline, String ambient) {
        return entries(v, eye, hit, headline, ambient, MAX);
    }

    private static void add(ListTag out, VoxelNode v, int slot, int part, int headline, String ambient) {
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
     * The entry for what is aimed at among parts (the client's own, or the node's): the device's by its slot; the
     * part's by its id where the parts have ids (the server), else by its first box. Aimed at nothing, or at one with
     * no entry (past the cap, or laid since the server answered): the keyless one, the block as a whole. Null only if
     * that is missing too.
     */
    public static CompoundTag choose(ListTag entries, VoxelNode.Aimed a, List<VoxelPart> parts) {
        VoxelPart p = a.element() < 0 && a.part() >= 0 && a.part() < parts.size() ? parts.get(a.part()) : null;
        CompoundTag whole = null;
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag t = entries.getCompoundOrEmpty(i);
            if (!t.contains("slot") && !t.contains("id")) whole = t;
            else if (a.element() >= 0 ? t.getIntOr("slot", -1) == a.element()
                : p != null && (p.id >= 0 ? t.getIntOr("id", -1) == p.id : java.util.Arrays.equals(t.getIntArray("box").orElse(null), p.boxes().get(0))))
                return t;
        }
        return whole;
    }
}
