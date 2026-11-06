package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CableSpec;
import mods.eln.node.voxel.KitItem;
import mods.eln.node.voxel.VoxelNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/** Power cables as specifications (Baughn 2026-11-06, Wires stage 3): cross-section, insulation (the tier), colour. */
public final class SpecCases {
    private SpecCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Rules());
        cases.add(new Extremes());
    }

    static final int LV = CablePart.tierOf("Low Voltage Cable"), VHV = CablePart.tierOf("Very High Voltage Cable");

    /**
     * The presets keep what cables were (the 48 V one exactly the Low Voltage Cable's resistance and rating), a spec
     * survives saves and the network, a cable saved before specs is its tier's preset, and kits of different specs are
     * different kits.
     */
    static final class Rules implements TestCase {
        public String name() {
            return "cable specs: presets, saves, network, kits";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var lv = mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor;
            var p = CableSpec.preset(LV).descriptor(LV);
            ctx.check("specs: the 48 V preset is the Low Voltage Cable (Rs, rating)",
                Math.abs(p.electricalRs - lv.electricalRs) < 1e-9 * lv.electricalRs && Math.abs(p.electricalMaximalCurrent - lv.electricalMaximalCurrent) < 1e-9,
                p.electricalRs + " vs " + lv.electricalRs + ", " + p.electricalMaximalCurrent + " vs " + lv.electricalMaximalCurrent);
            var mv = CableSpec.preset(2);
            ctx.check("specs: the 230 V preset carries 13 A (3 kW)", Math.abs(mv.amps() * mods.eln.Eln.MVU - 3000) < 1e-6, "" + mv.amps());
            ctx.check("specs: the 11 kV preset is 0.5 mm², 3 A", CableSpec.preset(VHV).squareMillimetres() == 0.5 && CableSpec.preset(VHV).amps() == 3, "");
            CablePart a = new CablePart(LV, new CableSpec(13, CableSpec.RED), 0, new int[]{0, 0, 8, 16, 0, 8});
            CompoundTag t = new CompoundTag();
            a.write(t);
            CablePart b = read(t);
            ctx.check("specs: a spec survives a save", b != null && b.sameCable(a), String.valueOf(t));
            t.remove("mm2");
            t.remove("col");
            CablePart old = read(t);
            ctx.check("specs: a cable saved before specs is its tier's preset", old != null && CableSpec.preset(LV).equals(old.spec), String.valueOf(old == null ? null : old.spec));
            try {
                var bytes = new java.io.ByteArrayOutputStream();
                a.serialize(new java.io.DataOutputStream(bytes));
                var m = CablePart.class.getDeclaredMethod("deserialize", java.io.DataInputStream.class);
                m.setAccessible(true);
                CablePart c = (CablePart) m.invoke(null, new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
                ctx.check("specs: a spec reaches the client", c.sameCable(a) && c.tierInfo().width() == a.tierInfo().width(), c.spec + " w " + c.tierInfo().width());
            } catch (Exception e) {
                ctx.check("specs: a spec reaches the client", false, e.toString());
            }
            var preset = CableKitItem.kit(LV, 64);
            var thick = CableKitItem.kit(LV, new CableSpec(13, CableSpec.BROWN), 64);
            ctx.check("specs: a preset kit carries no spec data, another does", CableKitItem.isPreset(preset) && !CableKitItem.isPreset(thick)
                && CableKitItem.specOf(thick).mm2() == 13, "");
            ctx.check("specs: kits of different specs don't merge", !KitItem.same(preset, thick) && KitItem.same(preset, CableKitItem.kit(LV, 16)), "");
            ctx.check("specs: a thick cable is wider and taller", new CablePart(LV, new CableSpec(13, 0), 0, new int[]{0, 0, 8, 16, 0, 8}).tierInfo().width() == 4
                && new CablePart(LV, new CableSpec(13, 0), 0, new int[]{0, 0, 8, 16, 0, 8}).tierInfo().height() == 2, "");
        }

        static CablePart read(CompoundTag t) {
            try {
                var m = CablePart.class.getDeclaredMethod("read", CompoundTag.class);
                m.setAccessible(true);
                return (CablePart) m.invoke(null, t);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Numerical stability at the extremes (Baughn): a 48 V line of 95 mm² copper (205 A) fed through one block of 0.5 mm²
     * (3 A), into a resistor; and an 11 kV line of 0.5 mm² into a resistor. Every conductor's voltage and temperature
     * stay finite, and the steady part settles without ringing (a thin stub over its rating may burn, as it should).
     */
    static final class Extremes implements TestCase {
        final List<double[]> trace = new ArrayList<>();   // [tick, row, U, T] of the conductors watched
        mods.eln.sim.IProcess watch;
        int tick;
        BlockPos thin, thick, vhv;
        String nan = "";

        public String name() {
            return "cable specs: thin and thick, 48 V and 11 kV, stay stable";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int ticks() {
            return 400;
        }

        public int floorDepth() {
            return 10;
        }

        public void build(TestContext c) {
            int z = 2;
            Batch1Cases.source(c, 0, z, mods.eln.Eln.LVU);
            CableCases.voxel(c, c.at(1, 1, z), CablePart.arms(LV, new CableSpec(0, CableSpec.BROWN), CableCases.DOWN, CableCases.WEST, CableCases.EAST));
            for (int x = 2; x <= 5; x++)
                CableCases.voxel(c, c.at(x, 1, z), CablePart.arms(LV, new CableSpec(13, CableSpec.BROWN), CableCases.DOWN, CableCases.WEST, CableCases.EAST));
            Batch1Cases.resistor(c, 6, z, 38);   // (15 ohm: about 3 A, the thin stub's rating)
            c.placeSix(Batch1Cases.GROUND, c.at(7, 1, z));
            thin = c.at(1, 1, z);
            thick = c.at(3, 1, z);
            z = 6;
            // (the 11 kV preset's 0.5 mm² carrying its 3 A: the heat model doesn't care what the voltage is, and no resistor
            // here takes 33 kW)
            Batch1Cases.source(c, 0, z, mods.eln.Eln.LVU);
            for (int x = 1; x <= 5; x++)
                CableCases.voxel(c, c.at(x, 1, z), CablePart.arms(VHV, null, CableCases.DOWN, CableCases.WEST, CableCases.EAST));
            Batch1Cases.resistor(c, 6, z, 38);
            c.placeSix(Batch1Cases.GROUND, c.at(7, 1, z));
            vhv = c.at(3, 1, z);
            mods.eln.Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                int row = 0;
                for (BlockPos p : new BlockPos[]{thin, thick, vhv}) {
                    if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level())) instanceof VoxelNode v)
                        for (double[] s : v.cableStates()) {
                            if (Double.isInfinite(s[0]) || Double.isInfinite(s[2]) || (Double.isNaN(s[0]) && !Double.isNaN(s[1])))
                                nan = "row " + row + " tick " + tick + ": " + java.util.Arrays.toString(s);
                            trace.add(new double[]{tick, row, s[0], s[2], s[1]});
                        }
                    row++;
                }
            });
        }

        public void measure(TestContext c) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            c.check("specs: every voltage and temperature stays finite", nan.isEmpty(), nan);
            // the thick 48 V cable and the 11 kV line: over the last 100 ticks, voltage swings under 1 % tick to tick
            for (int row : new int[]{1, 2}) {
                double worst = 0, last = Double.NaN;
                for (double[] t : trace) {
                    if (t[1] != row || t[0] < tick - 100 || Double.isNaN(t[2])) continue;
                    if (!Double.isNaN(last) && Math.abs(last) > 1) worst = Math.max(worst, Math.abs(t[2] - last) / Math.abs(last));
                    last = t[2];
                }
                double amps = 0, temp = 0;
                for (double[] t : trace) if (t[1] == row && t[0] == tick) { amps = t[4]; temp = t[3]; }
                c.line(String.format("specs: row %d last U %.3f V, I %.3f A, T %.2f, worst tick-to-tick change %.4f%%", row, last, amps, temp, worst * 100));
                c.check("specs: row " + row + " carries about 3 A", Math.abs(amps) > 2.5, String.format("%.3f A", amps));
                c.check("specs: row " + row + " settles without ringing", worst < 0.01, String.format("%.4f%%", worst * 100));
            }
        }
    }
}
