package mods.eln.selftest.cases;

import mods.eln.node.six.SixNodeElement;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.resistor.ResistorElement;
import mods.eln.node.transparent.TransparentNodeElement;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static mods.eln.selftest.cases.Wp9aCases.*;

/**
 * Failure modes (notes/failure-modes.md): devices burn out in place instead of exploding.
 * Rows along +X: Electrical Source -> Low Voltage Cable -> Power Resistor (100 ohm) -> Ground Cable.
 * <ul>
 * <li>z=0: source at 200 V, 4x the cable's rating: its voltage watchdog trips and the cable burns out
 * (open circuit, block still there, floor intact).</li>
 * <li>z=2: 50 V; the cable is burnt by hand, then repaired: it conducts again.</li>
 * </ul>
 */
public final class FailureCases {
    private FailureCases() {
    }

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new CableOvervoltageCase());
        cases.add(new OutcomesCase());
    }

    static final class CableOvervoltageCase implements SelfTestCase {
        SixNodeElement hot, fixed;
        ResistorElement rHot, rFixed;
        SixNodeElement gFixed;
        ElectricalSourceElement sFixed;
        boolean burntAtBuild, repairedAtBuild;

        public String name() {
            return "failure cable";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            source(ctx, SOURCE, 0, 0, 4 * U);
            hot = ctx.placeSix(LV_CABLE, ctx.at(1, 1, 0));
            rHot = resistor(ctx, POWER_RESISTOR, 2, 0, 48);
            ctx.placeSix(GROUND, ctx.at(3, 1, 0));

            sFixed = source(ctx, SOURCE, 0, 2, U);
            fixed = ctx.placeSix(LV_CABLE, ctx.at(1, 1, 2));
            rFixed = resistor(ctx, POWER_RESISTOR, 2, 2, 48);
            gFixed = ctx.placeSix(GROUND, ctx.at(3, 1, 2));
            fixed.burn(FailureCause.OVERHEAT, Failure.Kind.CABLE);
            burntAtBuild = fixed.isBurnt();
            fixed.repair();
            repairedAtBuild = !fixed.isBurnt();
        }

        public void measure(SelfTestContext ctx) {
            ctx.check("failure: 200 V on a 50 V cable burns it out", hot.isBurnt(),
                "burnt = " + hot.getBurnt());
            ctx.check("failure: burnt cable stays in the world",
                ctx.world().getBlockState(ctx.at(1, 1, 0)).getBlock() != Blocks.AIR, "");
            boolean floor = true;
            for (int x = 0; x < 4; x++)
                for (int z = 0; z < 3; z++)
                    if (ctx.world().getBlockState(ctx.at(x, 0, z)).getBlock() == Blocks.AIR) floor = false;
            ctx.check("failure: no crater (platform intact)", floor, "");
            double i = current(rHot);
            ctx.check("failure: burnt cable is an open circuit", Math.abs(i) < OPEN_I, f("I = %.3g A", i));

            ctx.check("failure: burn() then repair() round trip", burntAtBuild && repairedAtBuild && !fixed.isBurnt(), "");
            double rt = link(sFixed, fixed) + link(fixed, rFixed) + rFixed.nominalRs + link(rFixed, gFixed);
            ctx.checkValue("failure: repaired cable conducts [A] (50 V / R_total)", current(rFixed), U / rt);
        }
    }

    /**
     * The other outcomes, triggered directly (Failure.fail, as a watchdog would) on placed devices:
     * capacitor pop (burnt, no crater), machine overheat next to planks (burnt + fire), transformer (oil fire on stone),
     * HV arc flash on a cable (burnt), shaft overspeed (the node is gone: the one real explosion; smallest strength).
     */
    static final class OutcomesCase implements SelfTestCase {
        TransparentNodeElement cap, furnace, xfmr, shaft;
        SixNodeElement hv;
        BlockPos shaftPos;

        public String name() {
            return "failure outcomes";
        }

        public int width() {
            return 16;
        }

        public void build(SelfTestContext ctx) {
            cap = ctx.placeTransparent(Wp10aCases.POWER_CAPACITOR, ctx.at(1, 1, 1));
            furnace = ctx.placeTransparent(Wp10aCases.FURNACE, ctx.at(4, 1, 1));
            ctx.world().setBlockState(ctx.at(5, 1, 1), Blocks.PLANKS.getDefaultState());
            xfmr = ctx.placeTransparent(Wp10aCases.TRANSFORMER, ctx.at(8, 1, 1));
            hv = ctx.placeSix(LV_CABLE, ctx.at(11, 1, 1));
            shaft = ctx.placeTransparent(Wp11Cases.FLYWHEEL, ctx.at(14, 1, 1));
            shaftPos = new BlockPos(shaft.node.coordonate.x, shaft.node.coordonate.y, shaft.node.coordonate.z);

            new Failure(cap).capacitor().fail(FailureCause.OVERVOLTAGE, 1000);
            new Failure(furnace).machine().fail(FailureCause.OVERHEAT, 500);
            new Failure(xfmr).transformer().fail(FailureCause.OVERHEAT, 500);
            new Failure(hv).hvLine().fail(FailureCause.OVERVOLTAGE, 10000);
            new Failure(shaft).rotating(() -> 0).fail(FailureCause.OVERSPEED, 1500);
        }

        static boolean fireNear(SelfTestContext ctx, BlockPos c) {
            for (BlockPos p : BlockPos.getAllInBox(c.add(-1, -1, -1), c.add(1, 1, 1)))
                if (ctx.world().getBlockState(p).getBlock() == Blocks.FIRE) return true;
            return false;
        }

        public void measure(SelfTestContext ctx) {
            ctx.check("failure: capacitor overvoltage pops and burns out", cap.isBurnt(), "burnt = " + cap.getBurnt());
            ctx.check("failure: capacitor pop leaves the floor", ctx.world().getBlockState(ctx.at(1, 0, 1)).getBlock() != Blocks.AIR, "");
            ctx.check("failure: machine overheat burns out", furnace.isBurnt(), "burnt = " + furnace.getBurnt());
            ctx.check("failure: machine overheat sets the planks next to it alight", fireNear(ctx, ctx.at(5, 1, 1)), "");
            ctx.check("failure: transformer burns out", xfmr.isBurnt(), "");
            ctx.check("failure: transformer oil fire (on stone)", fireNear(ctx, ctx.at(8, 1, 1)), "");
            ctx.check("failure: HV arc flash burns the cable out", hv.isBurnt(), "burnt = " + hv.getBurnt());
            ctx.check("failure: shaft overspeed destroys the shaft", ctx.world().getBlockState(shaftPos).getBlock() == Blocks.AIR
                && shaft.node.isDestructing(), "");
        }
    }

    static String f(String fmt, Object... a) {
        return String.format(fmt, a);
    }
}
