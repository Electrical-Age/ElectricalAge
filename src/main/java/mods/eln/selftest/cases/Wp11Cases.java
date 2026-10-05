package mods.eln.selftest.cases;

import cofh.redstoneflux.api.IEnergyProvider;
import ic2.api.energy.tile.IEnergySource;
import mods.eln.Eln;
import mods.eln.Other;
import mods.eln.compat.top.TopIntegration;
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
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;

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

    public static void addTo(List<SelfTestCase> cases) {
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
        return p.add(v[0], v[1], v[2]);
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
    static SixNodeElement feed(SelfTestContext ctx, BlockPos cable, BlockPos src) {
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
    static TransparentNodeElement[] shaftLine(SelfTestContext ctx, int... damages) {
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
    static final class ShaftNetworkCase implements SelfTestCase {
        TransparentNodeElement[] parts;

        public String name() {
            return "wp11 shaft network";
        }

        public void build(SelfTestContext ctx) {
            parts = shaftLine(ctx, FLYWHEEL, JOINT, JOINT);
            shaft(parts[0]).setRads(100.0);
        }

        public void measure(SelfTestContext ctx) {
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
     * Shaft drag (0.95 * 0.02 J * rads per electrical step, mass 105) slows it only slightly.
     */
    static final class GeneratorCase implements SelfTestCase {
        GeneratorElement gen;

        public String name() {
            return "wp11 generator";
        }

        public void build(SelfTestContext ctx) {
            TransparentNodeElement[] parts = shaftLine(ctx, GENERATOR, FLYWHEEL);
            gen = (GeneratorElement) parts[0];
            shaft(gen).setRads(400.0);
        }

        public void measure(SelfTestContext ctx) {
            ShaftNetwork s = shaft(gen);
            double rads = s.getRads();
            GeneratorDescriptor d = gen.getDesc();
            ElectricalLoad input = field(gen, "inputLoad");
            ctx.check("wp11 generator shaft slowed by drag only (400 > rads > 200)", rads < 400.0 && rads > 200.0, String.format("%.3f rad/s", rads));
            ctx.checkValue("wp11 generator mass [kg] (5 + 100)", s.getMass(), 105.0);
            ctx.checkValue("wp11 generator open-circuit voltage [V] (3200 * rads / 800)", input.getU(), d.getRtoU().getValue(rads));
            ctx.checkValue("wp11 generator open-circuit voltage = 4 V per rad/s [V]", input.getU(), 4.0 * rads);
        }
    }

    /** Steam turbine with an empty tank: no fluid drained, no shaft energy (0 rad/s stays 0). */
    static final class TurbineIdleCase implements SelfTestCase {
        TurbineElement turbine;

        public String name() {
            return "wp11 steam turbine idle";
        }

        public void build(SelfTestContext ctx) {
            turbine = (TurbineElement) ctx.placeTransparent(STEAM_TURBINE, ctx.at(3, 1, 1));
        }

        public void measure(SelfTestContext ctx) {
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
    static final class PoleCase implements SelfTestCase {
        ElectricalPoleElement dcdc, plain;
        BlockPos dcdcPos, plainPos;

        public String name() {
            return "wp11 utility poles";
        }

        public void build(SelfTestContext ctx) {
            dcdcPos = ctx.at(1, 1, 1);
            plainPos = ctx.at(6, 1, 1);
            dcdc = (ElectricalPoleElement) ctx.placeTransparent(UTILITY_POLE_DCDC, dcdcPos);
            feed(ctx, ctx.at(2, 1, 1), ctx.at(3, 1, 1));
            plain = (ElectricalPoleElement) ctx.placeTransparent(UTILITY_POLE, plainPos);
        }

        void ghosts(SelfTestContext ctx, String what, BlockPos pole, int damage) {
            int n = 0;
            for (int dy = 1; dy <= 3; dy++)
                if (ctx.world().getBlockState(pole.up(dy)).getBlock() == Eln.ghostBlock) n++;
            ctx.check("wp11 " + what + ": 3 ghost blocks above", n == 3, n + " ghost block(s)");
            BlockPos g = pole.up(2);
            TopIntegration.GhostTarget target = TopIntegration.ghostTarget(ctx.world(), new Coordonate(g.getX(), g.getY(), g.getZ(), ctx.world()));
            ItemStack stack = target == null ? ItemStack.EMPTY : target.stack;
            ctx.check("wp11 " + what + ": TOP ghost lookup resolves to the pole item", !stack.isEmpty()
                && stack.getItem() == Eln.transparentNodeItem && stack.getItemDamage() == damage, stack.toString());
        }

        public void measure(SelfTestContext ctx) {
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
     * FE per J); extracting N FE removes N / ratio J; front face has no capability; receive is refused.
     * With RedstoneFlux / IC2 loaded: RF extract (simulated) and IC2 offered EU = min(buffer * ic2 ratio, 32), tier 1.
     * The converter is a simple node (not tracked by the selftest cleanup): this case removes it at the end of measure.
     */
    static final class EnergyConverterCase implements SelfTestCase {
        BlockPos pos;
        EnergyConverterElnToOtherNode node;
        long builtAt;

        public String name() {
            return "wp11 energy converter";
        }

        public void build(SelfTestContext ctx) {
            pos = ctx.at(1, 1, 1);
            feed(ctx, ctx.at(2, 1, 1), ctx.at(3, 1, 1));
            EnergyConverterElnToOtherBlock block = Wp11Content.elnToOtherBlockLvu;
            if (block == null) throw new IllegalStateException("energy converters disabled (ElnToOtherEnergyConverterEnable)");
            // front = player view inverse: yaw 90 (looking -X) -> front XP, towards the cable at x = 2
            FakePlayer player = FakePlayerFactory.getMinecraft(ctx.world());
            float yaw = player.rotationYaw, pitch = player.rotationPitch;
            player.rotationYaw = 90f;
            player.rotationPitch = 0f;
            try {
                ItemBlock item = (ItemBlock) net.minecraft.item.Item.getItemFromBlock(block);
                boolean ok = item.placeBlockAt(new ItemStack(item), player, ctx.world(), pos, EnumFacing.UP, 0.5F, 1F, 0.5F, block.getDefaultState());
                if (!ok) throw new IllegalStateException("placeBlockAt failed for the energy converter at " + pos);
            } finally {
                player.rotationYaw = yaw;
                player.rotationPitch = pitch;
            }
            NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos.getX(), pos.getY(), pos.getZ(), ctx.world()));
            if (!(n instanceof EnergyConverterElnToOtherNode)) throw new IllegalStateException("no converter node at " + pos);
            node = (EnergyConverterElnToOtherNode) n;
            node.networkUnserialize(ctx.stream(out -> {
                out.writeByte(EnergyConverterElnToOtherNode.setInPowerFactor);
                out.writeFloat(0.2f);
            }), null);
            builtAt = ctx.world().getTotalWorldTime();
        }

        public void measure(SelfTestContext ctx) {
            try {
                measureConverter(ctx);
            } finally {
                boolean drops = ctx.world().getGameRules().getBoolean("doTileDrops");
                ctx.world().getGameRules().setOrCreateGameRule("doTileDrops", "false");
                try {
                    ctx.world().setBlockToAir(pos);
                } finally {
                    ctx.world().getGameRules().setOrCreateGameRule("doTileDrops", Boolean.toString(drops));
                }
                boolean gone = ctx.world().isAirBlock(pos)
                    && NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos.getX(), pos.getY(), pos.getZ(), ctx.world())) == null;
                ctx.check("wp11 energy converter removed (own cleanup)", gone, "");
            }
        }

        void measureConverter(SelfTestContext ctx) {
            double lvp = Eln.LVP(), r = rs(), ratio = Other.getElnToTeConversionRatio();
            ctx.check("wp11 converter front faces the feed cable (XP)", node.getFront() == Direction.XP, String.valueOf(node.getFront()));
            ctx.checkValue("wp11 converter input power factor (GUI packet)", node.inPowerFactor, 0.2);
            Resistor in = field(node, "powerInResistor");
            double buffer = node.energyBuffer, max = node.energyBufferMax;
            ctx.checkValue("wp11 converter buffer max [J] (2 * LVP)", max, 2 * lvp);
            double factor = Math.min(1, (max - buffer) / max * 2);
            double rExp = U * U / (factor * lvp * 0.2);
            double i = U / (4 * r + rExp);
            double p = i * i * rExp;
            ctx.line(String.format("wp11 converter model: LVP = %.1f W, factor %.3f, R = U^2/(factor*LVP*0.2) = %.4f ohm, I = %.5f A, P = %.3f W, FE/t at that power = P*%.4f/20 = %.2f",
                lvp, factor, rExp, i, p, ratio, p * ratio / 20));
            if (factor >= 1) {
                ctx.checkValue("wp11 converter input resistor [ohm]", in.getR(), rExp);
                ctx.checkValue("wp11 converter input power [W]", in.getP(), p);
                long ticks = ctx.world().getTotalWorldTime() - builtAt;
                double lo = p * 0.05 * (ticks - 2), hi = p * 0.05 * (ticks + 1);
                ctx.check("wp11 converter buffer = P * elapsed time [J]", buffer >= lo && buffer <= hi,
                    String.format("%.2f J, expected %.2f..%.2f (%d ticks of %.3f W)", buffer, lo, hi, ticks, p));
            } else {
                ctx.check("wp11 converter input resistor [ohm] (buffer above half: factor < 1, +-5%)", Math.abs(in.getR() - rExp) <= 0.05 * rExp,
                    String.format("%.4f, expected %.4f", in.getR(), rExp));
            }

            TileEntity te = ctx.world().getTileEntity(pos);
            if (!(te instanceof EnergyConverterElnToOtherEntity)) {
                ctx.check("wp11 converter tile entity", false, String.valueOf(te));
                return;
            }
            EnergyConverterElnToOtherEntity e = (EnergyConverterElnToOtherEntity) te;
            EnumFacing back = Direction.XN.toEnumFacing(), frontFace = Direction.XP.toEnumFacing();
            IEnergyStorage fe = e.getCapability(CapabilityEnergy.ENERGY, back);
            ctx.check("wp11 converter FE capability on the back face only", fe != null && e.hasCapability(CapabilityEnergy.ENERGY, back)
                && !e.hasCapability(CapabilityEnergy.ENERGY, frontFace) && e.getCapability(CapabilityEnergy.ENERGY, EnumFacing.UP) == null, "");
            if (fe == null) return;
            ctx.check("wp11 converter FE extract only", fe.canExtract() && !fe.canReceive() && fe.receiveEnergy(1000, false) == 0, "");
            buffer = node.energyBuffer;
            int stored = fe.getEnergyStored();
            ctx.check("wp11 converter FE stored = (int)(buffer J * " + ratio + ")", stored == (int) (buffer * ratio), stored + " FE, buffer " + buffer + " J");
            int n = stored / 2;
            int sim = fe.extractEnergy(n, true);
            ctx.check("wp11 converter FE simulated extract leaves the buffer", sim == n && node.energyBuffer == buffer, sim + " FE");
            int got = fe.extractEnergy(n, false);
            ctx.check("wp11 converter FE extract returns the request (below stored)", got == n, got + " of " + n + " FE");
            ctx.checkValue("wp11 converter buffer after FE extract [J] (- N/ratio)", node.energyBuffer, buffer - n / ratio);
            int all = fe.extractEnergy(Integer.MAX_VALUE, true);
            ctx.check("wp11 converter FE extract capped by the buffer", all == (int) (node.energyBuffer * ratio), all + " FE");

            if (Loader.isModLoaded(EnergyConverterElnToOtherEntity.MODID_RF)) RfCheck.run(ctx, e, node, back, frontFace, ratio);
            else ctx.line("SKIP wp11 converter RF: redstoneflux not loaded");
            if (Loader.isModLoaded(EnergyConverterElnToOtherEntity.MODID_IC2)) Ic2Check.run(ctx, e, node);
            else ctx.line("SKIP wp11 converter IC2: ic2 not loaded");
        }
    }

    /** Separate classes: they reference RF / IC2 API types, loaded only when those mods are present. */
    static final class RfCheck {
        static void run(SelfTestContext ctx, EnergyConverterElnToOtherEntity e, EnergyConverterElnToOtherNode node, EnumFacing back, EnumFacing frontFace, double ratio) {
            IEnergyProvider rf = e;
            int rfSim = rf.extractEnergy(back, 10, true);
            ctx.check("wp11 converter RF: connects on the back only, extract (simulated) 10 RF",
                rf.canConnectEnergy(back) && !rf.canConnectEnergy(frontFace) && rfSim == Math.min(10, (int) (node.energyBuffer * ratio)), rfSim + " RF");
        }
    }

    static final class Ic2Check {
        static void run(SelfTestContext ctx, EnergyConverterElnToOtherEntity e, EnergyConverterElnToOtherNode node) {
            IEnergySource eu = e;
            double offered = eu.getOfferedEnergy();
            double exp = Math.min(node.energyBuffer * Other.getElnToIc2ConversionRatio(), 32);
            ctx.check("wp11 converter IC2: tier 1, offered EU = min(buffer * " + Other.getElnToIc2ConversionRatio() + ", 32)",
                eu.getSourceTier() == 1 && Math.abs(offered - exp) <= 1e-6 * Math.max(1, exp), String.format("%.4f EU, expected %.4f", offered, exp));
        }
    }
}
