package mods.eln.transparentnode.energyconverter;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Saved FE exporters from their simple-node days (tag "ElnToOther", everything at the root) become transparent-node
 * tags: coordinates in "node" (where TransparentNode reads them; at the root they would be lost and the exporter
 * deleted), the device id from the descriptor key, the front in the element's "others", and every value of the old
 * node in "element".
 */
public class ExporterSaveUpgradeTest {
    static final List<String> KEYS = List.of("EnergyConverterElnToOtherLVU", "EnergyConverterElnToOtherMVU", "EnergyConverterElnToOtherHVU");
    static final List<Integer> IDS = List.of(ExporterSaveUpgrade.LVU_ID, ExporterSaveUpgrade.MVU_ID, ExporterSaveUpgrade.HVU_ID);

    @Provide
    Arbitrary<Integer> coordinate() {
        return Arbitraries.oneOf(Arbitraries.integers().between(-30_000_000, 30_000_000), Arbitraries.of(0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Provide
    Arbitrary<Double> value() {
        return Arbitraries.oneOf(Arbitraries.doubles().between(-1e9, 1e9), Arbitraries.of(0.0, 1.0, Double.MIN_VALUE, Double.MAX_VALUE));
    }

    /** The tag the simple-node exporter wrote (SimpleNode/NodeBase/EnergyConverterElnToOtherNode.writeToNBT). */
    static CompoundTag oldTag(int x, int y, int z, int dim, int front, int key, double buffer, double factor, float uc, double r,
                              byte opaque, byte wrap) {
        CompoundTag t = new CompoundTag();
        t.putString("tag", "ElnToOther");
        t.putInt("cx", x);
        t.putInt("cy", y);
        t.putInt("cz", z);
        t.putInt("cd", dim);
        t.putByte("NBOpaque", opaque);
        t.putByte("NBWrap", wrap);
        t.putByte("SNfront", (byte) front);
        t.putString("SNdescriptorKey", KEYS.get(key));
        t.putFloat("loadUc", uc);
        t.putDouble("powerInResistorR", r);
        t.putDouble("energyBuffer", buffer);
        t.putDouble("inPowerFactor", factor);
        return t;
    }

    @Property
    void everyFieldIsCarried(@ForAll("coordinate") int x, @ForAll @IntRange(min = -64, max = 320) int y, @ForAll("coordinate") int z,
                             @ForAll @IntRange(min = -10, max = 10) int dim, @ForAll @IntRange(min = 0, max = 5) int front,
                             @ForAll @IntRange(min = 0, max = 2) int key, @ForAll("value") double buffer, @ForAll("value") double factor,
                             @ForAll float uc, @ForAll("value") double r, @ForAll byte opaque, @ForAll byte wrap) {
        CompoundTag old = oldTag(x, y, z, dim, front, key, buffer, factor, uc, r, opaque, wrap);
        CompoundTag before = old.copy();
        CompoundTag t = ExporterSaveUpgrade.upgrade(old);
        assertEquals(before, old, "the input tag is not changed");

        assertEquals("t", t.getStringOr("tag", ""));
        CompoundTag node = t.getCompoundOrEmpty("node");
        Coordonate c = new Coordonate();
        c.readFromNBT(node, "c"); // as NodeBase.readFromNBT, given the "node" compound by TransparentNode
        assertEquals(x, c.x);
        assertEquals(y, c.y);
        assertEquals(z, c.z);
        assertEquals(dim, c.dimention);
        assertEquals(opaque, node.getByteOr("NBOpaque", (byte) 99));
        assertEquals(wrap, node.getByteOr("NBWrap", (byte) 99));
        assertEquals((int) IDS.get(key), t.getShortOr("eid", (short) -1));

        CompoundTag e = t.getCompoundOrEmpty("element");
        byte others = e.getByteOr("others", (byte) -1);
        assertEquals(Direction.fromInt(front), Direction.fromInt(others & 7), "front (TransparentNodeElement.readFromNBT)");
        assertTrue((others & 8) != 0, "grounded, as a placed element");
        assertEquals(buffer, e.getDoubleOr("energyBuffer", Double.NaN));
        assertEquals(factor, e.getDoubleOr("inPowerFactor", Double.NaN));
        assertEquals(uc, e.getFloatOr("loadUc", Float.NaN));
        assertEquals(r, e.getDoubleOr("powerInResistorR", Double.NaN));
        for (String k : List.of("cx", "cy", "cz", "cd", "SNfront", "SNdescriptorKey", "tag"))
            assertFalse(e.contains(k), k + " stays out of the element");
        assertFalse(t.contains("cx"), "no coordinates left at the root");

        assertEquals(t, ExporterSaveUpgrade.upgrade(t), "an upgraded tag is left alone");
    }

    @Property
    void extraValuesOfTheOldNodeGoToTheElement(@ForAll @IntRange(min = 0, max = 2) int key, @ForAll("value") double v) {
        CompoundTag old = oldTag(1, 2, 3, 0, 1, key, 5, 0.5, 0, 1, (byte) 0, (byte) 0);
        old.putDouble("someLaterField", v);
        assertEquals(v, ExporterSaveUpgrade.upgrade(old).getCompoundOrEmpty("element").getDoubleOr("someLaterField", Double.NaN));
    }

    @Test
    public void tagsItCannotPlaceAreLeftAsTheyAre() {
        CompoundTag unknownKey = oldTag(1, 2, 3, 0, 1, 0, 5, 0.5, 0, 1, (byte) 0, (byte) 0);
        unknownKey.putString("SNdescriptorKey", "EnergyConverterElnToOtherEV");
        assertSame(unknownKey, ExporterSaveUpgrade.upgrade(unknownKey));

        CompoundTag noCoordinates = oldTag(1, 2, 3, 0, 1, 0, 5, 0.5, 0, 1, (byte) 0, (byte) 0);
        noCoordinates.remove("cy");
        assertSame(noCoordinates, ExporterSaveUpgrade.upgrade(noCoordinates));

        CompoundTag other = new CompoundTag();
        other.putString("tag", "s");
        other.putInt("cx", 4);
        assertSame(other, ExporterSaveUpgrade.upgrade(other));
    }

    /**
     * Through NodeManager (the mod is loaded in the test JVM, so its devices are registered): the upgrade runs before
     * the node type is looked up, the exporter loads as a transparent node with its position, front, slider and
     * buffer, and the next save writes it in the transparent-node form.
     */
    @Test
    public void nodeManagerLoadsAnOldExporter() {
        NodeManager previous = NodeManager.instance;
        try {
            NodeManager.registerUuid("t", TransparentNode.class);
            NodeManager.registerTagUpgrade(ExporterSaveUpgrade.OLD_TYPE, ExporterSaveUpgrade::upgrade);
            NodeManager nm = new NodeManager("test");
            CompoundTag file = new CompoundTag();
            file.put("n0", oldTag(10, 64, -3, 0, 2, 1, 120, 0.25, 3f, 40, (byte) 1, (byte) 2));
            nm.loadFromNbt(file, 0);
            assertTrue(nm.getOrphans(0).isEmpty(), "not kept aside: " + nm.getOrphans(0));
            assertEquals(1, nm.getNodes().size());
            TransparentNode t = (TransparentNode) nm.getNodes().get(0);
            assertEquals(new Coordonate(10, 64, -3, 0), t.coordonate);
            EnergyConverterElnToOtherElement e = (EnergyConverterElnToOtherElement) t.element;
            assertEquals(ExporterSaveUpgrade.MVU_ID, t.elementId);
            assertEquals("EnergyConverterElnToOtherMVU", e.descriptor.legacyKey);
            assertEquals(Direction.fromInt(2), e.getFront());
            assertEquals(120.0, e.energyBuffer);
            assertEquals(0.25, e.inPowerFactor);
            assertEquals(3f, (float) e.load.getU());

            CompoundTag saved = new CompoundTag();
            nm.saveToNbt(saved, 0);
            CompoundTag n0 = saved.getCompoundOrEmpty("n0");
            assertEquals("t", n0.getStringOr("tag", ""));
            assertEquals(-3, n0.getCompoundOrEmpty("node").getIntOr("cz", 0));
            assertEquals(120.0, n0.getCompoundOrEmpty("element").getDoubleOr("energyBuffer", 0));
        } finally {
            NodeManager.TAG_UPGRADES.remove(ExporterSaveUpgrade.OLD_TYPE);
            NodeManager.instance = previous;
        }
    }
}
