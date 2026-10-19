package mods.eln.transparentnode.energyconverter;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Set;

/**
 * Saved exporters from before they were transparent nodes (node file electricalAgeWorld&lt;dim&gt;.dat): the simple node
 * wrote {@code tag:"ElnToOther"} with everything at the root (coordinates cx/cy/cz/cd, NBOpaque/NBWrap, SNfront,
 * SNdescriptorKey, then its own loadUc, powerInResistorR, energyBuffer, inPowerFactor). A transparent node reads
 * {@code tag:"t"}, its coordinates from a nested {@code node} compound, {@code eid} (the device id) and the element's
 * state from {@code element}, with the front in {@code others}. Applied by NodeManager before a node is built; a tag
 * it can't place (unknown descriptor key, no coordinates) is returned unchanged, so NodeManager keeps it as an orphan
 * rather than dropping the player's exporter.
 */
public final class ExporterSaveUpgrade {
    private ExporterSaveUpgrade() {
    }

    /** The simple-node exporter's node type. */
    public static final String OLD_TYPE = "ElnToOther";

    /** Device ids (transparent node, group 84; new in 26.x, no 1.12 transparent twin). */
    public static final int GROUP = 84;
    public static final int LVU_ID = 0 + (GROUP << 6), MVU_ID = 4 + (GROUP << 6), HVU_ID = 8 + (GROUP << 6);

    /** 1.12 descriptor key -> device id. */
    public static final Map<String, Integer> IDS = Map.of(
        "EnergyConverterElnToOtherLVU", LVU_ID,
        "EnergyConverterElnToOtherMVU", MVU_ID,
        "EnergyConverterElnToOtherHVU", HVU_ID);

    /** Root keys of the old tag that belong to the node (or are dropped), not the element. */
    static final Set<String> NODE_KEYS = Set.of("tag", "cx", "cy", "cz", "cd", "NBOpaque", "NBWrap", "SNfront", "SNdescriptorKey");

    /** The transparent-node tag for an old exporter tag; any other tag (or one it can't convert) unchanged. */
    public static CompoundTag upgrade(CompoundTag old) {
        if (!OLD_TYPE.equals(old.getStringOr("tag", ""))) return old;
        Integer eid = IDS.get(old.getStringOr("SNdescriptorKey", ""));
        if (eid == null || !old.contains("cx") || !old.contains("cy") || !old.contains("cz")) return old;

        CompoundTag node = new CompoundTag();
        node.putInt("cx", old.getIntOr("cx", 0));
        node.putInt("cy", old.getIntOr("cy", 0));
        node.putInt("cz", old.getIntOr("cz", 0));
        node.putInt("cd", old.getIntOr("cd", 0));
        node.putByte("NBOpaque", old.getByteOr("NBOpaque", (byte) 0));
        node.putByte("NBWrap", old.getByteOr("NBWrap", (byte) 0));
        node.putByte("lastLight", (byte) 0);

        CompoundTag element = new CompoundTag();
        for (String k : old.keySet())
            if (!NODE_KEYS.contains(k)) element.put(k, old.get(k).copy());
        int front = old.getByteOr("SNfront", (byte) 0) & 0x7;
        if (front > 5) front = 0;
        element.putByte("others", (byte) (front + 8)); // grounded, as a freshly placed element

        CompoundTag out = new CompoundTag();
        out.putString("tag", "t");
        out.put("node", node);
        out.putShort("eid", (short) (int) eid);
        out.put("element", element);
        return out;
    }
}
