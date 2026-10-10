package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeDescriptor;

import java.util.Map;
import java.util.TreeMap;

/**
 * Device ids by descriptor name (as in golden/1.12/inventory.json "name"). New cases use these instead of raw
 * `sub + (id << 6)` damage values, so they port to a registry without damage sub-items (notes/port26/T2-audit.md).
 */
public final class Ids {
    private Ids() {
    }

    public static int six(String name) {
        for (Map.Entry<Integer, SixNodeDescriptor> e : new TreeMap<>(Eln.sixNodeItem.subItemList).entrySet())
            if (name.equals(e.getValue().name)) return e.getKey();
        throw new IllegalStateException("no six-node device named " + name);
    }

    public static int transparent(String name) {
        for (Map.Entry<Integer, TransparentNodeDescriptor> e : new TreeMap<>(Eln.transparentNodeItem.subItemList).entrySet())
            if (name.equals(e.getValue().name)) return e.getKey();
        throw new IllegalStateException("no transparent-node device named " + name);
    }
}
