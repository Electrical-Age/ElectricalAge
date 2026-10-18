package mods.eln.gametest.bench;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gametest.cases.Ids;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The cable performance benchmark (notes/cables/PERF.md; Baughn 2026-10-18: "a performance test, defined pre-port,
 * checked after"). Run alone: `gradlew runGameTestServer -PelnBench=/path/result.json [-PelnBenchLabel=...]`; then no
 * other test is registered. It builds about 20k cable blocks beside the test structure:
 * - straight runs of every tier, a source at one end and a resistor at the other (half the cable's rated power);
 * - a medium-voltage grid (rows and columns: T's and crossings) fed at its corners, loads inside;
 * - low-voltage runs over one-block bumps (inner and outer corners);
 * and then measures the simulator's per-tick cost (electrical, thermal fast, thermal slow, slow processes, total) in
 * phases: steady (after a warm-up), churn (a resistor joining two runs toggled every 20 ticks: sub-system rebuilds),
 * fault (a short in the grid until cables burn) and teardown (breaking 1000 cable blocks). The voxel build runs the
 * same scene through the converter, so the numbers compare like for like.
 */
public final class CableBench implements TestCase {
    static final int WARMUP = 200, STEADY = 1000, CHURN = 400, FAULT = 300;
    static final int RUNS = 100, RUN_LEN = 100, GRID_X = 80, GRID_Z = 80, BUMP_RUNS = 30, BUMP_LEN = 100;

    public static boolean enabled() {
        return System.getProperty("eln.bench") != null;
    }

    @Override
    public String id() {
        return "cable_bench";
    }

    @Override
    public int ticks() {
        return WARMUP + STEADY + CHURN + FAULT + 40;
    }

    record Run(String tier, ElectricalLoad first, ElectricalLoad last, Resistor load, double expectA, List<Component> parts, List<ElectricalLoad> terminals) {
    }

    final List<Component> added = new ArrayList<>();
    final List<ElectricalLoad> addedLoads = new ArrayList<>();
    final List<Run> runs = new ArrayList<>();
    final List<BlockPos> teardown = new ArrayList<>();
    final List<ElectricalLoad> gridRow = new ArrayList<>();
    final Map<String, List<long[]>> phases = new LinkedHashMap<>();
    final JsonObject result = new JsonObject();
    ServerLevel level;
    ElectricalLoad shortAt;
    Resistor churn, shortR;
    int tick = -1, cables;
    String phase = "build";
    boolean done;

    final long[] lastStage = new long[5];
    static CableBench active;
    static boolean listening;

    @Override
    public void build(TestContext ctx) {
        level = ctx.level();
        BlockPos o = ctx.at(16, 1, 0);
        long t0 = System.nanoTime();
        for (int cx = (o.getX() >> 4) - 1; cx <= ((o.getX() + 400) >> 4) + 1; cx++)
            for (int cz = (o.getZ() >> 4) - 1; cz <= ((o.getZ() + 220) >> 4) + 1; cz++) level.setChunkForced(cx, cz, true);
        String[] tiers = {"Low Voltage Cable", "Medium Voltage Cable", "High Voltage Cable", "Very High Voltage Cable", "Signal Cable"};
        List<Object[]> feeds = new ArrayList<>();   // tier, first, last
        // straight runs, 2 apart (not touching), tiers in turn
        for (int r = 0; r < RUNS; r++) {
            String tier = tiers[r % tiers.length];
            for (int i = 0; i < RUN_LEN; i++) {
                BlockPos p = o.offset(i, 0, r * 2);
                cable(ctx, tier, p, Direction.YN);
                if (r < 10) teardown.add(p);
            }
            feeds.add(new Object[]{tier, o.offset(0, 0, r * 2), o.offset(RUN_LEN - 1, 0, r * 2)});
        }
        // the grid: rows every 2 blocks, columns every 8 (crossings and T's), fed at the corners
        BlockPos g = o.offset(RUN_LEN + 10, 0, 0);
        for (int x = 0; x < GRID_X; x++)
            for (int z = 0; z < GRID_Z; z++)
                if (z % 2 == 0 || x % 8 == 0) cable(ctx, "Medium Voltage Cable", g.offset(x, 0, z), Direction.YN);
        // runs over bumps: floor, up the bump's side (inner corner), over its top (outer corners), down
        BlockPos b = g.offset(GRID_X + 10, 0, 0);
        for (int r = 0; r < BUMP_RUNS; r++) {
            for (int i = 0; i < BUMP_LEN; i++) {
                BlockPos p = b.offset(i, 0, r * 3);
                if (i % 6 == 3) {
                    ctx.floor(p);
                    cable(ctx, "Low Voltage Cable", p.above(), Direction.YN);
                    cable(ctx, "Low Voltage Cable", p.west(), Direction.XP);
                    cable(ctx, "Low Voltage Cable", p.east(), Direction.XN);
                } else cable(ctx, "Low Voltage Cable", p, Direction.YN);
            }
            BlockPos last = b.offset(BUMP_LEN - 1, 0, r * 3);
            feeds.add(new Object[]{"Low Voltage Cable", b.offset(0, 0, r * 3), (BUMP_LEN - 1) % 6 == 3 ? last.above() : last});
        }
        // the voxel build: the same scene through the converter
        if (System.getProperty("eln.bench.convert") != null) {
            int conv = 0, kept = 0;
            for (mods.eln.node.NodeBase n : new ArrayList<>(mods.eln.node.NodeManager.instance.getNodes()))
                if (n instanceof mods.eln.node.six.SixNode six && !(n instanceof mods.eln.node.voxel.VoxelNode) && n.coordonate.world() == level) {
                    if (mods.eln.node.voxel.CableConverter.convert(six) == null) conv++;
                    else kept++;
                }
            result.addProperty("converted", conv);
            result.addProperty("keptSixNode", kept);
        }
        for (Object[] f : feeds) runs.add(feed((String) f[0], loadAt((BlockPos) f[1]), loadAt((BlockPos) f[2])));
        ElectricalCableDescriptor mv = descriptor("Medium Voltage Cable");
        for (int[] c : new int[][]{{0, 0}, {GRID_X - 8, 0}, {0, GRID_Z - 2}, {GRID_X - 8, GRID_Z - 2}}) {
            VoltageSource s = new VoltageSource("bench", terminal(loadAt(g.offset(c[0], 0, c[1]))), null).setU(mv.electricalNominalVoltage);
            add(s);
        }
        for (int x = 4; x < GRID_X; x += 8)
            for (int z = 4; z < GRID_Z; z += 8)
                add(new Resistor(terminal(loadAt(g.offset(x, 0, z))), null).setR(4 * mv.electricalNominalVoltage * mv.electricalNominalVoltage / mv.electricalNominalPower));
        shortAt = loadAt(g.offset(GRID_X / 2, 0, GRID_Z / 2));
        for (int x = 0; x < GRID_X; x++) gridRow.add(loadAt(g.offset(x, 0, GRID_Z / 2)));
        result.addProperty("label", System.getProperty("eln.bench.label", "?"));
        result.addProperty("cables", cables);
        result.addProperty("buildMs", (System.nanoTime() - t0) / 1e6);
        active = this;
        Simulator.probe = (e, tf, ts, s, total) -> {
            long[] st = Eln.simulator.mna.stageNs, d = new long[st.length];
            for (int i = 0; i < st.length; i++) {
                d[i] = st[i] - lastStage[i];
                lastStage[i] = st[i];
            }
            if (active == this && !done && !"build".equals(phase) && !"warmup".equals(phase))
                phases.computeIfAbsent(phase, k -> new ArrayList<>()).add(new long[]{e, tf, ts, s, total, d[0], d[1], d[2], d[3], d[4]});
        };
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, ev -> {
                if (active != null) active.step();
            });
        }
    }

    ElectricalCableDescriptor descriptor(String tier) {
        return (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(Ids.six(tier));
    }

    void cable(TestContext ctx, String tier, BlockPos p, Direction side) {
        ctx.floor(p.relative(side.toEnumFacing()));
        ctx.placeSix(Ids.six(tier), p, side);
        cables++;
    }

    /** The floor cable's load at p: a six-node element's, or a converted voxel node's conductor. */
    ElectricalLoad loadAt(BlockPos p) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level));
        if (n instanceof mods.eln.node.six.SixNode six) return ((ElectricalCableElement) six.getElement(Direction.YN)).electricalLoad;
        if (n instanceof mods.eln.node.voxel.VoxelNode v) return v.cableLoad(0);
        throw new IllegalStateException("no cable at " + p);
    }

    /** A source at the first cable, a resistor drawing half the rated power at the last. */
    Run feed(String tier, ElectricalLoad first, ElectricalLoad last) {
        ElectricalCableDescriptor d = descriptor(tier);
        double u = d.electricalNominalVoltage, r = 2 * u * u / d.electricalNominalPower;
        int c0 = added.size(), l0 = addedLoads.size();
        add(new VoltageSource("bench", terminal(first), null).setU(u));
        Resistor load = new Resistor(terminal(last), null).setR(r);
        add(load);
        return new Run(tier, first, last, load, u / r, new ArrayList<>(added.subList(c0, added.size())),
            new ArrayList<>(addedLoads.subList(l0, addedLoads.size())));
    }

    /**
     * A device's own node joined to the cable, as devices do (a resistor straight to ground on a cable's load would
     * end a simplified Line at ground, which Line can't take).
     */
    ElectricalLoad terminal(ElectricalLoad cable) {
        ElectricalLoad t = new ElectricalLoad();
        t.setRs(1e-3);
        Eln.simulator.addElectricalLoad(t);
        addedLoads.add(t);
        add(new mods.eln.sim.ElectricalConnection(t, cable));
        return t;
    }

    void add(Component c) {
        added.add(c);
        Eln.simulator.addElectricalComponent(c);
    }

    void step() {
        if (done) return;
        tick++;
        if (tick == 0) phase = "warmup";
        else if (tick == WARMUP) {
            phase = "steady";
            int[] st = Eln.simulator.mna.stats();
            result.addProperty("subSystems", st[0]);
            result.addProperty("statesSolved", st[1]);
            result.addProperty("lines", st[2]);
            result.addProperty("statesInLines", st[3]);
            result.addProperty("interSystems", st[4]);
            int[] c = Eln.simulator.counts();
            result.add("simLists", toJson(new String[]{"EP", "TFL", "TFC", "TFP", "TSL", "TSC", "TSP", "SP"}, c));
            int dead = 0;
            StringBuilder deadList = new StringBuilder();
            for (int i = 0; i < runs.size(); i++) {
                Run r = runs.get(i);
                if (Math.abs(r.load.getCurrent()) < 0.2 * r.expectA) {
                    dead++;
                    if (deadList.length() < 300) deadList.append(i).append(' ').append(r.tier).append(String.format(" %.3g/%.3g A; ", r.load.getCurrent(), r.expectA));
                }
            }
            result.addProperty("runs", runs.size());
            // the solution, to compare builds: every run's load current, and the grid's voltages along its middle row
            JsonArray amps = new JsonArray();
            for (Run r : runs) amps.add(r.load.getCurrent());
            result.add("runAmps", amps);
            JsonArray volts = new JsonArray();
            for (ElectricalLoad l : gridRow) volts.add(l.getU());
            result.add("gridVolts", volts);
            result.addProperty("runsNotConducting", dead);
            if (dead > 0) result.addProperty("runsNotConductingDetail", deadList.toString());
        } else if (tick == WARMUP + STEADY) phase = "churn";
        else if (tick == WARMUP + STEADY + CHURN) {
            phase = "fault";
            if (churn != null) {
                Eln.simulator.removeElectricalComponent(churn);
                added.remove(churn);
                churn = null;
            }
            shortR = new Resistor(terminal(shortAt), null).setR(1e-3);
            add(shortR);
        } else if (tick == WARMUP + STEADY + CHURN + FAULT) {
            phase = "teardown";
            long t0 = System.nanoTime();
            // the devices on the torn-down runs go first (as a player breaking them would)
            for (int r = 0; r < 10; r++) for (Component c : runs.get(r).parts) {
                Eln.simulator.removeElectricalComponent(c);
                added.remove(c);
            }
            for (int r = 0; r < 10; r++) for (ElectricalLoad l : runs.get(r).terminals) {
                Eln.simulator.removeElectricalLoad(l);
                addedLoads.remove(l);
            }
            for (BlockPos p : teardown) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            result.addProperty("teardownMs", (System.nanoTime() - t0) / 1e6);
            result.addProperty("teardownCables", teardown.size());
        } else if (tick == WARMUP + STEADY + CHURN + FAULT + 5) {
            finish();
            return;
        }
        if ("churn".equals(phase) && (tick - WARMUP - STEADY) % 20 == 0) {
            if (churn == null) {
                churn = new Resistor(terminal(runs.get(10).last), terminal(runs.get(15).last)).setR(10);
                add(churn);
            } else {
                Eln.simulator.removeElectricalComponent(churn);
                added.remove(churn);
                churn = null;
            }
        }
    }

    void finish() {
        done = true;
        Simulator.probe = null;
        JsonObject ph = new JsonObject();
        for (var e : phases.entrySet()) ph.add(e.getKey(), summary(e.getValue()));
        result.add("phases", ph);
        // the scene stays as it is: the benchmark server exits after this test
        String path = System.getProperty("eln.bench");
        try (Writer w = new FileWriter(path)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(result, w);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static JsonObject summary(List<long[]> ticks) {
        String[] names = {"E", "TF", "TS", "S", "total", "E.generate", "E.interSystem", "E.calc", "E.flush", "E.lineFlush"};
        JsonObject o = new JsonObject();
        o.addProperty("ticks", ticks.size());
        for (int k = 0; k < names.length; k++) {
            long[] v = new long[ticks.size()];
            double sum = 0;
            for (int i = 0; i < v.length; i++) {
                v[i] = ticks.get(i)[k];
                sum += v[i];
            }
            Arrays.sort(v);
            JsonObject s = new JsonObject();
            if (v.length > 0) {
                s.addProperty("meanUs", sum / v.length / 1e3);
                s.addProperty("p50Us", v[v.length / 2] / 1e3);
                s.addProperty("p95Us", v[(int) (v.length * 0.95)] / 1e3);
                s.addProperty("p99Us", v[Math.min(v.length - 1, (int) (v.length * 0.99))] / 1e3);
                s.addProperty("maxUs", v[v.length - 1] / 1e3);
            }
            o.add(names[k], s);
        }
        return o;
    }

    static JsonObject toJson(String[] names, int[] v) {
        JsonObject o = new JsonObject();
        for (int i = 0; i < names.length; i++) o.addProperty(names[i], v[i]);
        return o;
    }

    @Override
    public void measure(TestContext ctx) {
        if (!done) finish();
        ctx.check("cable bench: every run conducts", result.has("runsNotConducting") && result.get("runsNotConducting").getAsInt() == 0,
            result.has("runsNotConductingDetail") ? result.get("runsNotConductingDetail").getAsString() : "");
    }
}
