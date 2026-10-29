package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Incandescence;
import mods.eln.node.Node;
import mods.eln.node.NodeManager;
import mods.eln.sim.ThermalLoad;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Hot things glow (Baughn 2026-10-28): each device that heats up on the outside lights its own block by temperature
 * (Incandescence.lightLevel), checked every half second. Temperatures are held with a huge heat capacity.
 */
public final class IncandescenceCases {
    private IncandescenceCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Lights());
    }

    static final class Lights implements TestCase {
        record Probe(String what, BlockPos pos, double celsius, int expect) {
        }

        final List<Probe> probes = new ArrayList<>();
        String err = "";

        public String id() {
            return "hot_things_light_their_block";
        }

        public int width() {
            return 10;
        }

        // (an existing structure size, floor_10x5x10)
        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 40;   // a check every 10 ticks, the per-tick budget is far away
        }

        /** Hold a load at a temperature (C): Tc relative to ambient, a capacity nothing here can move. */
        static void hold(ThermalLoad t, double celsius) {
            t.Tc = celsius - mods.eln.sim.PhysicalConstant.Tamb;
            t.C = 1e15;
        }

        public void build(TestContext c) {
            // the Large Rheostat (warm limit 1000 C) orange-hot, and one cold
            var hot = (mods.eln.transparentnode.LargeRheostatElement) c.placeTransparent(Wp10bCases.LARGE_RHEOSTAT, c.at(1, 1, 1));
            hold(hot.getThermalLoad(), 905);
            probes.add(new Probe("Large Rheostat at 905 C", c.at(1, 1, 1), 905, 8));
            var cold = (mods.eln.transparentnode.LargeRheostatElement) c.placeTransparent(Wp10bCases.LARGE_RHEOSTAT, c.at(3, 1, 1));
            hold(cold.getThermalLoad(), 500);
            probes.add(new Probe("Large Rheostat at 500 C", c.at(3, 1, 1), 500, 0));
            // the electric heater's coils at 720 C (it fails at 800)
            var heater = (mods.eln.oil.ElectricHeaterElement) c.placeTransparent(Ids.transparent("50V Electric Heater"), c.at(5, 1, 1));
            hold(heater.thermal, 720);
            probes.add(new Probe("50V Electric Heater at 720 C", c.at(5, 1, 1), 720, 4));
            // a six-node thermal cable at 950 C (fails at 1000)
            var tc = c.placeSix(Ids.six("Copper Thermal Cable"), c.at(7, 1, 1));
            hold((ThermalLoad) mods.eln.gametest.LabCommands.field(tc, "thermalLoad"), 950);
            probes.add(new Probe("thermal cable (six-node) at 950 C", c.at(7, 1, 1), 950, 9));
            // a power resistor near its 130 C limit: its own warning scale
            var res = c.placeSix(Ids.six("Power Resistor"), c.at(1, 1, 4));
            double warm = 0.95 * 130;
            hold((ThermalLoad) mods.eln.gametest.LabCommands.field(res, "thermalLoad"), warm + mods.eln.sim.PhysicalConstant.Tamb);
            double rc = Incandescence.warningCelsius(warm, 130);
            probes.add(new Probe("Power Resistor at 95% of its limit (" + Math.round(rc) + " C on its scale)", c.at(1, 1, 4), rc,
                Incandescence.lightLevel(rc)));
            // a voxel thermal cable (converted) at 650 C
            c.placeSix(Ids.six("Copper Thermal Cable"), c.at(5, 1, 4));
            mods.eln.node.NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(c.at(5, 1, 4), c.level()));
            if (n instanceof mods.eln.node.six.SixNode s6) {
                String why = mods.eln.node.voxel.CableConverter.convert(s6);
                if (why != null) err += "convert: " + why;
            }
            n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(c.at(5, 1, 4), c.level()));
            if (n instanceof mods.eln.node.voxel.VoxelNode v) hold(v.cableThermal(0), 650);
            else err += "no voxel node; ";
            probes.add(new Probe("thermal cable (voxel) at 650 C", c.at(5, 1, 4), 650, 3));
        }

        public void measure(TestContext c) {
            c.check("incandescence: built", err.isEmpty(), err);
            for (Probe p : probes) {
                mods.eln.node.NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.pos(), c.level()));
                int got = n instanceof Node nn ? nn.getLightValue() : -1;
                c.check("incandescence: " + p.what() + " lights its block " + p.expect(), got == p.expect(), "light " + got);
                int emitted = c.level().getBlockState(p.pos()).getLightEmission(c.level(), p.pos());
                c.check("incandescence: " + p.what() + ": the block emits it", emitted == p.expect(), "emission " + emitted);
            }
            mods.eln.node.NodeBase v = NodeManager.instance.getNodeFromCoordonate(new Coordonate(c.at(5, 1, 4), c.level()));
            if (v instanceof mods.eln.node.voxel.VoxelNode vn) {
                byte[] st = vn.glowSteps();
                int max = 0;
                for (byte b : st) max = Math.max(max, b & 255);
                c.check("incandescence: the voxel cable's glow is published (step " + Incandescence.step(650) + ")",
                    max == Incandescence.step(650), "max step " + max);
            }
        }
    }
}
