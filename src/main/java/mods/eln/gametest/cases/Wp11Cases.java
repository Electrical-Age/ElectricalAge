package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import mods.eln.registry.batch.Wp11Content;
import mods.eln.gridnode.electricalpole.ElectricalPoleElement;
import mods.eln.mechanical.GeneratorDescriptor;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.mechanical.ShaftElement;
import mods.eln.mechanical.ShaftNetwork;
import mods.eln.mechanical.TurbineElement;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.registry.batch.Wp11Content;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Selftest cases of device batch Wp11 (registry/batch/Wp11Content). Feeds use a 50 V electrical source and one LV
 * cable on the platform floor; model as SelfTest.measure: two loads connect through Rs(a) + Rs(b), and the source,
 * the LV cable and every load set by lowVoltageCableDescriptor/applySmallRs have Rs = rs = Rs(LV cable).
 * Not tested: steam/gas turbine power (needs a steam/gas fluid in the tank; only the idle case is checked), grid
 * links between poles (made by a player using a cable item on two poles), the downlink and grid transformer (not
 * registered in 1.7.10 either), FE/RF push into a neighbour (no FE receiver block on a bare server), TOP rendering
 * (client; the server-side ghost lookup it uses is checked).
 */
public final class Wp11Cases {
    private Wp11Cases() {
    }

    static final int SOURCE = 0 + (3 << 6);          // Electrical Source
    static final int LV_CABLE = 4 + (32 << 6);       // Low Voltage Cable
    static final int STEAM_TURBINE = 9 + (4 << 6);
    static final int GENERATOR = 10 + (4 << 6);
    static final int JOINT = 12 + (4 << 6);
    static final int FLYWHEEL = 14 + (4 << 6);
    static final int UTILITY_POLE = 4 + (123 << 6);
    static final int UTILITY_POLE_DCDC = 5 + (123 << 6);
    static final double U = 50.0;

    public static void addTo(List<TestCase> cases) {
        cases.add(new ShaftNetworkCase());
        cases.add(new GeneratorCase());
        cases.add(new TurbineIdleCase());
        cases.add(new PoleCase());
        cases.add(new EnergyConverterCase());
    }

    // ---- helpers ----

    static double rs() {
        return ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs;
    }

    static BlockPos off(BlockPos p, Direction d) {
        int[] v = {0, 0, 0};
        d.applyTo(v, 1);
        return p.offset(v[0], v[1], v[2]);
    }

    @SuppressWarnings("unchecked")
    static <T> T field(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(o);
            } catch (NoSuchFieldException ignored) {
                // superclass
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("no field " + name + " in " + o.getClass());
    }

    /** 50 V source at `src` feeding an LV cable at `cable` (both on the floor). */
    static SixNodeElement feed(TestContext ctx, BlockPos cable, BlockPos src) {
        SixNodeElement c = ctx.placeSix(LV_CABLE, cable);
        ElectricalSourceElement s = (ElectricalSourceElement) ctx.placeSix(SOURCE, src);
        s.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat((float) U);
        }));
        return c;
    }

    static ShaftNetwork shaft(TransparentNodeElement e) {
        return ((ShaftElement) e).getShaft();
    }

    /**
     * Shaft parts in a line: the first at (3,1,1), the others along its shaft axis (front.left(), as every part
     * placed by the same player has the same front). Returns them in placement order.
     */
    static TransparentNodeElement[] shaftLine(TestContext ctx, int... damages) {
        TransparentNodeElement[] out = new TransparentNodeElement[damages.length];
        BlockPos p0 = ctx.at(3, 1, 1);
        out[0] = ctx.placeTransparent(damages[0], p0);
        Direction axis = out[0].front.left();
        BlockPos[] spots = {off(p0, axis), off(p0, axis.getInverse())};
        for (int i = 1; i < damages.length; i++) out[i] = ctx.placeTransparent(damages[i], spots[i - 1]);
        return out;
    }

    // ---- cases ----

    /**
     * Joint + flywheel + joint on one axis merge into one ShaftNetwork: mass = 0.5 + 100 + 0.5 = 101, J/rad =
     * m^2 * shapeFactor(0.5) / 2. Passive parts have no drag, so 100 rad/s set at build stays 100 rad/s.
     */
    static final class ShaftNetworkCase implements TestCase {
        TransparentNodeElement[] parts;

        public String name() {
            return "wp11 shaft network";
        }

        public void build(TestContext ctx) {
            parts = shaftLine(ctx, FLYWHEEL, JOINT, JOINT);
            shaft(parts[0]).setRads(100.0);
        }

        public void measure(TestContext ctx) {
            ShaftNetwork s = shaft(parts[0]);
            boolean same = s == shaft(parts[1]) && s == shaft(parts[2]) && s.getElements().size() == 3;
            ctx.check("wp11 joint + flywheel + joint share one shaft network", same, s.getElements().size() + " element(s)");
            ctx.checkValue("wp11 shaft mass [kg] (0.5 + 100 + 0.5)", s.getMass(), 101.0);
            ctx.checkValue("wp11 shaft speed [rad/s] (no drag in passive parts: 100 kept)", s.getRads(), 100.0);
            ctx.checkValue("wp11 shaft energy [J] (m^2*0.5/2 * rads = 255025)", s.getEnergy(), 101.0 * 101.0 * 0.25 * 100.0);
        }
    }

    /**
     * Generator + flywheel spun to 400 rad/s, generator electrically open: its source sets the open-circuit EMF
     * U = RtoU(rads) = 3200 V * rads / 800 rad/s (= 4 V per rad/s), seen unchanged on the input load (no current).
     * Shaft drag (0.95 * 0.02 J * rads per tick, mass 105) slows it only slightly.
     * WP16b thermal: sized for 4000 (1/0.95 - 1) + 0.02 * 20 * 800 * 0.05 = 226.5 W at 65 % of the 130 K warm limit
     * (Rp = 130 / (226.5 / 0.65) = 0.3731 K/W); open circuit the only heat is the drag share, 0.02 * 20 * rads * 0.05
     * = 0.02 * rads W (1.7.10 / WP16 heated with 1/20 of it).
     */
    static final class GeneratorCase implements TestCase {
        GeneratorElement gen;

        public String name() {
            return "wp11 generator";
        }

        public void build(TestContext ctx) {
            TransparentNodeElement[] parts = shaftLine(ctx, GENERATOR, FLYWHEEL);
            gen = (GeneratorElement) parts[0];
            shaft(gen).setRads(400.0);
        }

        public void measure(TestContext ctx) {
            ShaftNetwork s = shaft(gen);
            double rads = s.getRads();
            GeneratorDescriptor d = gen.getDesc();
            ElectricalLoad input = field(gen, "inputLoad");
            ctx.check("wp11 generator shaft slowed by drag only (400 > rads > 200)", rads < 400.0 && rads > 200.0, String.format("%.3f rad/s", rads));
            ctx.checkValue("wp11 generator mass [kg] (5 + 100)", s.getMass(), 105.0);
            ctx.checkValue("wp11 generator open-circuit voltage [V] (3200 * rads / 800)", input.getU(), d.getRtoU().getValue(rads));
            ctx.checkValue("wp11 generator open-circuit voltage = 4 V per rad/s [V]", input.getU(), 4.0 * rads);
            mods.eln.sim.ThermalLoad th = field(gen, "thermal");
            ctx.checkValue("wp11 generator thermal Rp [K/W] (130 / (226.5 W / 0.65))", th.Rp, 130 / (226.5 / 0.65));
            double heat = th.Pc + (th.Tc - th.Pc * 0.05 / th.C) / th.Rp; // heat in the last slow step, before the Rp leak
            ctx.checkValue("wp11 generator open-circuit heat [W] (drag share 0.02 * rads)", heat, 0.02 * rads);
        }
    }

    /** Steam turbine with an empty tank: no fluid drained, no shaft energy (0 rad/s stays 0). */
    static final class TurbineIdleCase implements TestCase {
        TurbineElement turbine;

        public String name() {
            return "wp11 steam turbine idle";
        }

        public void build(TestContext ctx) {
            turbine = (TurbineElement) ctx.placeTransparent(STEAM_TURBINE, ctx.at(3, 1, 1));
        }

        public void measure(TestContext ctx) {
            double rads = shaft(turbine).getRads();
            ctx.check("wp11 steam turbine without steam: shaft stays at 0 rad/s, fluid rate 0",
                rads == 0.0 && turbine.getFluidRate() == 0f, String.format("%.4f rad/s, %.3f mB/s", rads, turbine.getFluidRate()));
        }
    }

    /**
     * Utility pole w/DC-DC converter fed 50 V on its ground side (LV cable + source), grid side open: the transformer
     * process gives grid U = ground U / ratio(0.25) = 200 V (no current: ground U = 50 V). Both poles get their 3 ghost
     * blocks above; the TOP ghost lookup resolves a ghost to the pole's item. Plain utility pole: placement + ghosts.
     */
    static final class PoleCase implements TestCase {
        ElectricalPoleElement dcdc, plain;
        BlockPos dcdcPos, plainPos;

        public String name() {
            return "wp11 utility poles";
        }

        /** Three ghosts above each pole; GameTest encases each box in barriers, so the box must hold them. */
        public int height() {
            return 5;
        }

        public void build(TestContext ctx) {
            dcdcPos = ctx.at(1, 1, 1);
            plainPos = ctx.at(6, 1, 1);
            dcdc = (ElectricalPoleElement) ctx.placeTransparent(UTILITY_POLE_DCDC, dcdcPos);
            feed(ctx, ctx.at(2, 1, 1), ctx.at(3, 1, 1));
            plain = (ElectricalPoleElement) ctx.placeTransparent(UTILITY_POLE, plainPos);
        }

        void ghosts(TestContext ctx, String what, BlockPos pole, int damage) {
            int n = 0;
            for (int dy = 1; dy <= 3; dy++)
                if (ctx.world().getBlockState(pole.above(dy)).getBlock() == Eln.ghostBlock) n++;
            ctx.check("wp11 " + what + ": 3 ghost blocks above", n == 3, n + " ghost block(s)");
            // 1.12 also checked TheOneProbe's ghost lookup; TOP comes back in P6
        }

        public void measure(TestContext ctx) {
            ghosts(ctx, "DC-DC pole", dcdcPos, UTILITY_POLE_DCDC);
            ghosts(ctx, "utility pole", plainPos, UTILITY_POLE);
            ctx.checkValue("wp11 DC-DC pole ground side [V] (no current: = 50)", dcdc.secondaryLoad.getU(), U);
            ctx.checkValue("wp11 DC-DC pole grid side [V] (ground / 0.25 = 200)", dcdc.electricalLoad.getU(), U / 0.25);
            ctx.check("wp11 utility pole has no ground connection (no transformer)", plain.secondaryLoad == null && plain.getConnectionMask(Direction.XP, LRDU.Down) == 0, "");
        }
    }

    /**
     * 50V energy converter (LVU) fed by source + LV cable on its front, GUI input-power factor set to 0.2. While the
     * buffer is below half of its max (2 * LVP J), the input resistor is R = U^2 / (factor(1) * LVP * 0.2); the circuit
     * is U / (src-cable 2rs + cable-converter 2rs + R). The buffer integrates the resistor power over the elapsed
     * ticks (0.05 s each). Forge Energy on the back face (only): stored = buffer * ratio (ElnToThermalExpansion,
     * FE per J); extracting N FE removes N / ratio J (an aborted transaction gives it back); the front face has no
     * capability; insert is refused. 26.x: FE only (1.12 also had RF and IC2 EU). The converter is a simple node:
     * this case removes it at the end of measure.
     */
    static final class EnergyConverterCase implements TestCase {
        BlockPos pos;
        EnergyConverterElnToOtherNode node;
        long builtAt;

        public String name() {
            return "wp11 energy converter";
        }

        public void build(TestContext ctx) {
            pos = ctx.at(1, 1, 1);
            feed(ctx, ctx.at(2, 1, 1), ctx.at(3, 1, 1));
            if (Wp11Content.elnToOtherBlockLvu == null) throw new IllegalStateException("energy converters disabled (ElnToOtherEnergyConverterEnable)");
            EnergyConverterElnToOtherBlock block = Wp11Content.elnToOtherBlockLvu.get();
            // front = player view inverse: yaw 90 (looking -X) -> front XP, towards the cable at x = 2
            net.neoforged.neoforge.common.util.FakePlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.world());
            float yaw = player.getYRot(), pitch = player.getXRot();
            player.setYRot(90f);
            player.setXRot(0f);
            try {
                mods.eln.node.simple.SimpleNodeItem item = (mods.eln.node.simple.SimpleNodeItem) block.asItem();
                if (item.placeAt(new ItemStack(item), player, ctx.world(), pos) == null)
                    throw new IllegalStateException("placing the energy converter at " + pos + " failed");
            } finally {
                player.setYRot(yaw);
                player.setXRot(pitch);
            }
            NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos, ctx.world()));
            if (!(n instanceof EnergyConverterElnToOtherNode)) throw new IllegalStateException("no converter node at " + pos);
            node = (EnergyConverterElnToOtherNode) n;
            node.networkUnserialize(ctx.stream(out -> {
                out.writeByte(EnergyConverterElnToOtherNode.setInPowerFactor);
                out.writeFloat(0.2f);
            }), null);
            builtAt = ctx.world().getGameTime();
        }

        public void measure(TestContext ctx) {
            try {
                measureConverter(ctx);
            } finally {
                ctx.world().removeBlock(pos, false);
                boolean gone = ctx.world().getBlockState(pos).isAir()
                    && NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos, ctx.world())) == null;
                ctx.check("wp11 energy converter removed (own cleanup)", gone, "");
            }
        }

        void measureConverter(TestContext ctx) {
            double lvp = Eln.LVP(), r = rs(), ratio = Eln.ElnToTeConversionRatio;
            ctx.check("wp11 converter front faces the feed cable (XP)", node.getFront() == Direction.XP, String.valueOf(node.getFront()));
            ctx.checkValue("wp11 converter input power factor (GUI packet)", node.inPowerFactor, 0.2);
            Resistor in = field(node, "powerInResistor");
            double buffer = node.energyBuffer, max = node.energyBufferMax;
            ctx.checkValue("wp11 converter buffer max [J] (2 * LVP)", max, 2 * lvp);
            double factor = Math.min(1, (max - buffer) / max * 2);
            double rExp = U * U / (factor * lvp * 0.2);
            double i = U / (4 * r + rExp);
            double p = i * i * rExp;
            ctx.line(String.format("wp11 converter model: LVP = %.1f W, factor %.3f, R = U^2/(factor*LVP*0.2) = %.4f ohm, I = %.5f A, P = %.3f W",
                lvp, factor, rExp, i, p));
            if (factor >= 1) {
                ctx.checkValue("wp11 converter input resistor [ohm]", in.getR(), rExp);
                ctx.checkValue("wp11 converter input power [W]", in.getP(), p);
                long ticks = ctx.world().getGameTime() - builtAt;
                double lo = p * 0.05 * (ticks - 2), hi = p * 0.05 * (ticks + 1);
                ctx.check("wp11 converter buffer = P * elapsed time [J]", buffer >= lo && buffer <= hi,
                    String.format("%.2f J, expected %.2f..%.2f (%d ticks of %.3f W)", buffer, lo, hi, ticks, p));
            } else {
                ctx.check("wp11 converter input resistor [ohm] (buffer above half: factor < 1, +-5%)", Math.abs(in.getR() - rExp) <= 0.05 * rExp,
                    String.format("%.4f, expected %.4f", in.getR(), rExp));
            }
            int stateFront = ctx.world().getBlockState(pos).getValue(EnergyConverterElnToOtherBlock.FRONT);
            ctx.check("wp11 converter block state FRONT = east (the node's front)", stateFront == net.minecraft.core.Direction.EAST.get3DDataValue(), "FRONT " + stateFront);

            net.minecraft.core.Direction back = net.minecraft.core.Direction.WEST, frontFace = net.minecraft.core.Direction.EAST;
            var cap = net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK;
            net.neoforged.neoforge.transfer.energy.EnergyHandler fe = ctx.world().getCapability(cap, pos, back);
            ctx.check("wp11 converter FE capability on the back face only", fe != null && ctx.world().getCapability(cap, pos, frontFace) == null
                && ctx.world().getCapability(cap, pos, net.minecraft.core.Direction.UP) == null, "");
            if (fe == null) return;
            try (net.neoforged.neoforge.transfer.transaction.Transaction tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                ctx.check("wp11 converter FE insert refused", fe.insert(1000, tx) == 0, "");
            }
            buffer = node.energyBuffer;
            long stored = fe.getAmountAsLong();
            ctx.check("wp11 converter FE stored = (long)(buffer J * " + ratio + ")", stored == (long) (buffer * ratio), stored + " FE, buffer " + buffer + " J");
            int n = (int) (stored / 2);
            int sim;
            try (net.neoforged.neoforge.transfer.transaction.Transaction tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                sim = fe.extract(n, tx); // not committed: rolled back
            }
            ctx.check("wp11 converter FE aborted extract leaves the buffer", sim == n && node.energyBuffer == buffer, sim + " FE, buffer " + node.energyBuffer);
            int got;
            try (net.neoforged.neoforge.transfer.transaction.Transaction tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                got = fe.extract(n, tx);
                tx.commit();
            }
            ctx.check("wp11 converter FE extract returns the request (below stored)", got == n, got + " of " + n + " FE");
            ctx.checkValue("wp11 converter buffer after FE extract [J] (- N/ratio)", node.energyBuffer, buffer - n / ratio);
            int all;
            try (net.neoforged.neoforge.transfer.transaction.Transaction tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                all = fe.extract(Integer.MAX_VALUE, tx);
            }
            ctx.check("wp11 converter FE extract capped by the buffer", all == (int) (node.energyBuffer * ratio), all + " FE");
        }
    }


}
