package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.fluid.FuelRegistry;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.process.PowerSourceBipole;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.transparentnode.LargeRheostatDescriptor;
import mods.eln.transparentnode.LargeRheostatElement;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxElement;
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxElement;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceElement;
import mods.eln.transparentnode.solarpanel.SolarPanelElement;
import mods.eln.transparentnode.teleporter.TeleporterElement;
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveElement;
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveElement;
import mods.eln.transparentnode.turbine.TurbineDescriptor;
import mods.eln.transparentnode.turbine.TurbineElement;
import mods.eln.transparentnode.turret.TurretElement;
import mods.eln.transparentnode.waterturbine.WaterTurbineElement;
import mods.eln.transparentnode.windturbine.WindTurbineElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.lang.reflect.Method;
import java.util.List;

import static mods.eln.selftest.cases.Wp10aCases.field;
import static mods.eln.selftest.cases.Wp10aCases.off;
import static mods.eln.selftest.cases.Wp9aCases.f;
import static mods.eln.selftest.cases.Wp9cCases.TICK;
import static mods.eln.selftest.cases.Wp9cCases.elapsed;
import static mods.eln.selftest.cases.Wp9cCases.setField;
import static mods.eln.selftest.cases.Wp9cCases.within;

/**
 * Selftest cases of device batch Wp10b (registry/batch/Wp10bContent): transparent-node generators and thermal devices.
 * <p>
 * Thermal loads hold the temperature above ambient (Tc; ambient = 0). A lone thermal load with Rp to ambient and
 * capacity C decays as T(t) = T0 exp(-t / (Rp C)); EA's dissipator descriptor sets C = P_nom tau / T_nom and
 * Rp = T_nom / P_nom, so Rp C = tau (nominalTao, 10 s for the small passive dissipator). Elapsed time = world ticks
 * between build and measure * 0.05 s, judged with +-1.5 ticks. Electrical model as wp9a/wp10a: connection(a, b) =
 * Rs(a) + Rs(b), read from the loads.
 * Generators whose input is weather/sky dependent (solar, wind) are checked against the device's own input function
 * evaluated at measure time (P = Pmax * light, P = PfW(wind)), not against a fixed number.
 * Not tested server-side: water turbine with water (flowing water would spread over the lab world; only "no water ->
 * no power" is checked), heat furnaces burning (fuel + regulator items; only idle at ambient), active dissipator fan
 * (placement only), teleporting and turret aiming/shooting (need entities; placement, ghosts and idle state only),
 * 200V variants (same elements).
 */
public final class Wp10bCases {
    private Wp10bCases() {
    }

    // damage = subId + (id << 6), 1.7.10 ids
    static final int SIGNAL_SOURCE = 1 + (3 << 6);
    static final int GROUND = 0 + (2 << 6);
    static final int HEAT_FURNACE = 0 + (3 << 6);
    static final int FUEL_HEAT_FURNACE = 1 + (3 << 6);
    static final int TURBINE_50V = 1 + (4 << 6);
    static final int ANTENNA_TX_LOW = 0 + (7 << 6);
    static final int ANTENNA_RX_LOW = 1 + (7 << 6);
    static final int SOLAR_SMALL = 1 + (48 << 6);
    static final int WIND_TURBINE = 0 + (49 << 6);
    static final int WATER_TURBINE = 16 + (49 << 6);
    static final int DISSIPATOR_PASSIVE = 0 + (64 << 6);
    static final int DISSIPATOR_ACTIVE = 32 + (64 << 6);
    static final int TELEPORTER = 0 + (65 << 6);
    static final int TURRET = 0 + (66 << 6);
    static final int FUEL_GENERATOR_50V = 1 + (67 << 6);
    static final int LARGE_RHEOSTAT = 39 + (96 << 6);

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new DissipatorCase());
        cases.add(new LargeRheostatCase());
        cases.add(new TurbineCase());
        cases.add(new FuelGeneratorCase());
        cases.add(new SolarCase());
        cases.add(new WindWaterCase());
        cases.add(new AntennaCase());
        cases.add(new TeleporterCase());
        cases.add(new IdleCase());
    }

    // ------------------------------------------------------------------ helpers

    static FakePlayer player(SelfTestContext ctx) {
        return FakePlayerFactory.getMinecraft(ctx.world());
    }

    /** Right click on a transparent node with `held` in the main hand; returns what the hand holds afterwards. */
    static ItemStack click(SelfTestContext ctx, TransparentNodeElement e, ItemStack held) {
        EntityPlayer p = player(ctx);
        p.setHeldItem(EnumHand.MAIN_HAND, held);
        try {
            e.onBlockActivated(p, Direction.YP, 0.5F, 1F, 0.5F);
            return p.getHeldItemMainhand().copy();
        } finally {
            p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    /** Place with the fake player's view chosen so that the placed element's front is `front` (PlayerViewHorizontal). */
    static TransparentNodeElement placeFacing(SelfTestContext ctx, int damage, BlockPos p, Direction front) {
        FakePlayer fp = player(ctx);
        TransparentNodeDescriptor d = Eln.transparentNodeItem.getDescriptor(damage);
        Direction clicked = Direction.fromIntMinecraftSide(EnumFacing.UP.getIndex()).getInverse();
        float oldYaw = fp.rotationYaw;
        try {
            for (float yaw = 0; yaw < 360; yaw += 90) {
                fp.rotationYaw = yaw;
                if (d.getFrontFromPlace(clicked, fp) == front) return ctx.placeTransparent(damage, p);
            }
        } finally {
            fp.rotationYaw = oldYaw;
        }
        throw new IllegalStateException("no view gives " + d.name + " the front " + front);
    }

    static BlockPos pos(TransparentNodeElement e) {
        return new BlockPos(e.node.coordonate.x, e.node.coordonate.y, e.node.coordonate.z);
    }

    static ElectricalLoad tload(TransparentNodeElement e, Direction side) {
        ElectricalLoad l = e.getElectricalLoad(side, LRDU.Down);
        if (l == null) throw new IllegalStateException("no load on " + side + " of " + e);
        return l;
    }

    static int ghosts(SelfTestContext ctx, BlockPos center, int r) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBox(center.add(-r, -r, -r), center.add(r, r, r)))
            if (ctx.world().getBlockState(p).getBlock() == Eln.ghostBlock) n++;
        return n;
    }

    static Object callPrefix(Object o, String prefix) {
        for (Method m : o.getClass().getMethods()) {
            if (m.getName().startsWith(prefix) && m.getParameterCount() == 0) {
                try {
                    return m.invoke(o);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalStateException("no method " + prefix + "* in " + o.getClass());
    }

    static double decay(double t0, double tau, double t) {
        return t0 * Math.exp(-t / tau);
    }

    // ------------------------------------------------------------------ cases

    /**
     * Two Small Passive Thermal Dissipators (P_nom 250 W at T_nom 30 K, tau 10 s), warmed to 100 K. A: water bucket ->
     * T halves to 50 K, the bucket comes back empty; B: 2 ice blocks, one used -> T * 0.2 = 20 K. Then free decay:
     * T = T0 exp(-t / 10 s). An Active Dissipator is only placed (its fan needs a powered network).
     */
    static final class DissipatorCase implements SelfTestCase {
        ThermalDissipatorPassiveElement a, b;
        TransparentNodeElement active;
        ItemStack afterWater, afterIce;
        long t0;

        public String name() {
            return "wp10b passive dissipator";
        }

        public int width() {
            return 8;
        }

        public void build(SelfTestContext ctx) {
            a = (ThermalDissipatorPassiveElement) ctx.placeTransparent(DISSIPATOR_PASSIVE, ctx.at(1, 1, 1));
            b = (ThermalDissipatorPassiveElement) ctx.placeTransparent(DISSIPATOR_PASSIVE, ctx.at(4, 1, 1));
            active = ctx.placeTransparent(DISSIPATOR_ACTIVE, ctx.at(7, 1, 1));
            Wp10aCases.<ThermalLoad>field(a, "thermalLoad").Tc = 100;
            Wp10aCases.<ThermalLoad>field(b, "thermalLoad").Tc = 100;
            afterWater = click(ctx, a, new ItemStack(Items.WATER_BUCKET));
            afterIce = click(ctx, b, new ItemStack(Blocks.ICE, 2));
            t0 = ctx.world().getTotalWorldTime();
        }

        public void measure(SelfTestContext ctx) {
            double t = elapsed(ctx, t0);
            ThermalLoad la = field(a, "thermalLoad"), lb = field(b, "thermalLoad");
            ctx.checkValue("wp10b dissipator time constant Rp * C [s] (= nominalTao)", la.Rp * la.C, 10);
            ctx.check("wp10b dissipator water bucket returns an empty bucket", afterWater.getItem() == Items.BUCKET, String.valueOf(afterWater));
            ctx.check("wp10b dissipator ice: one of two used", afterIce.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.ICE)
                && afterIce.getCount() == 1, String.valueOf(afterIce));
            double tol = 1.5 * TICK;
            within(ctx, f("wp10b dissipator after water [K] (100 / 2 * exp(-%.2f s / 10 s))", t), la.Tc, decay(50, 10, t),
                Math.abs(decay(50, 10, t - tol) - decay(50, 10, t + tol)) / 2 + 0.01 * decay(50, 10, t));
            within(ctx, f("wp10b dissipator after ice [K] (100 * 0.2 * exp(-%.2f s / 10 s))", t), lb.Tc, decay(20, 10, t),
                Math.abs(decay(20, 10, t - tol) - decay(20, 10, t + tol)) / 2 + 0.01 * decay(20, 10, t));
            ctx.check("wp10b active dissipator placed", active instanceof ThermalDissipatorActiveElement, String.valueOf(active));
        }
    }

    /**
     * Large Rheostat (core: 24 coal dust -> E12 from 1 ohm, index 24 = 100 ohm), control pin at 50 V (normalized 1 ->
     * R = (1 + 0.01) / 1.01 * 100 = 100 ohm), between a 50 V source (LV cable) and a ground cable.
     * I = 50 / (Rs(src) + 2 Rs(cable) + Rs(a) + R + Rs(b) + Rs(ground)); heat = I^2 R into its thermal load.
     */
    static final class LargeRheostatCase implements SelfTestCase {
        LargeRheostatElement rh;
        TransparentNodeElement e;
        mods.eln.node.six.SixNodeElement src, cable, gnd;

        public String name() {
            return "wp10b large rheostat";
        }

        public int width() {
            return 5;
        }

        public void build(SelfTestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            BlockPos p = ctx.at(2, 1, 1);
            e = placeFacing(ctx, LARGE_RHEOSTAT, p, Direction.ZN); // control pin toward z0, a/b terminals along X
            l.used.add(p);
            rh = (LargeRheostatElement) e;
            rh.getInventory().setInventorySlotContents(0, Wp9aCases.coal(24)); // ResistorContainer.coreId
            rh.getInventory().markDirty();
            rh.inventoryChange(rh.getInventory());
            mods.eln.node.six.SixNodeElement[] fed = l.feed(p, rh.front.right()); // aLoad side
            src = fed[0];
            cable = fed[1];
            gnd = l.six(GROUND, off(p, rh.front.left()));
            BlockPos cp = off(p, rh.front);
            ElectricalSourceElement s = (ElectricalSourceElement) l.six(SIGNAL_SOURCE, cp);
            s.networkUnserialize(ctx.stream(o -> {
                o.writeByte(ElectricalSourceElement.setVoltageId);
                o.writeFloat(50F);
            }));
        }

        public void measure(SelfTestContext ctx) {
            LargeRheostatDescriptor d = (LargeRheostatDescriptor) rh.getDescriptor();
            ctx.checkValue("wp10b large rheostat nominal R [ohm] (E12 index 24 = 100)", rh.getNominalRs(), 100);
            Resistor r = rh.getResistor();
            ctx.checkValue("wp10b large rheostat R at control 50 V [ohm] ((1 + 0.01) / 1.01 * nominal)", r.getR(), rh.getNominalRs());
            double rt = src.getElectricalLoad(LRDU.Up).getRs() + 2 * cable.getElectricalLoad(LRDU.Up).getRs()
                + tload(rh, rh.front.right()).getRs() + r.getR() + tload(rh, rh.front.left()).getRs() + gnd.getElectricalLoad(LRDU.Up).getRs();
            ctx.checkValue("wp10b large rheostat current [A] (50 V / R_total)", Math.abs(r.getI()), 50 / rt);
            ctx.check("wp10b large rheostat heats (I^2 R > 0 into its thermal load)", rh.getThermalLoad().Tc > 0,
                f("T %.4f K above ambient, %s", rh.getThermalLoad().Tc, d.name));
        }
    }

    /**
     * 50V Turbine, warm side set to 150 K above the cool side, nothing connected (open circuit): the source voltage is
     * the descriptor's TtoU(dT) (table scaled to nominal dT 250 K / 50 V: dT 150 K -> 50 V), evaluated at the measured
     * dT; no current -> no heat drawn.
     */
    static final class TurbineCase implements SelfTestCase {
        TurbineElement turbine;

        public String name() {
            return "wp10b turbine";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            turbine = (TurbineElement) ctx.placeTransparent(TURBINE_50V, ctx.at(2, 1, 1));
            turbine.warmLoad.Tc = 150;
            turbine.coolLoad.Tc = 0;
        }

        public void measure(SelfTestContext ctx) {
            TurbineDescriptor d = (TurbineDescriptor) turbine.getDescriptor();
            double dT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
            ctx.checkValue("wp10b turbine table TtoU(150 K) [V] (= nominal 50 V)", d.TtoU.getValue(150), 50);
            ctx.checkValue(f("wp10b turbine open-circuit voltage [V] (TtoU(dT = %.2f K))", dT), turbine.positiveLoad.getU(), d.TtoU.getValue(dT));
        }
    }

    /**
     * 50V Fuel Generator (nominal 300 W * fuelGeneratorPowerFactor, Umax = 52.5 V, tank 2 buckets, tank energy =
     * tank seconds * nominal P). Lava is no fuel: a lava bucket is refused (tank 0, bucket kept). A bucket of a
     * registered gasoline fuel (if any and bucketable) fills half the tank and comes back empty. With fuel, a click
     * starts it: open circuit -> U = Umax; tank drains at the minimum load 0.1 * P_nom with efficiency factor 1.375:
     * dLevel/dt = 1.375 * 0.1 * P_nom / tank energy.
     */
    static final class FuelGeneratorCase implements SelfTestCase {
        TransparentNodeElement gen;
        ItemStack afterLava, afterFuel;
        String fuelName;
        double level0, lavaLevel;
        long t0;

        public String name() {
            return "wp10b fuel generator";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            gen = ctx.placeTransparent(FUEL_GENERATOR_50V, ctx.at(2, 1, 1));
            afterLava = click(ctx, gen, new ItemStack(Items.LAVA_BUCKET));
            lavaLevel = field(gen, "tankLevel");
            for (String name : FuelRegistry.INSTANCE.getGasolineList()) {
                Fluid fl = FluidRegistry.getFluid(name);
                if (fl == null) continue;
                ItemStack bucket = FluidUtil.getFilledBucket(new FluidStack(fl, Fluid.BUCKET_VOLUME));
                if (bucket.isEmpty()) continue;
                fuelName = name;
                afterFuel = click(ctx, gen, bucket);
                break;
            }
            level0 = field(gen, "tankLevel");
            if (fuelName == null) {
                level0 = 0.5;
                setField(gen, "tankLevel", level0);
            }
            click(ctx, gen, ItemStack.EMPTY); // switch on
            t0 = ctx.world().getTotalWorldTime();
        }

        public void measure(SelfTestContext ctx) {
            double t = elapsed(ctx, t0);
            ctx.check("wp10b fuel generator refuses lava (not a fuel): tank empty, bucket kept", lavaLevel == 0
                && afterLava.getItem() == Items.LAVA_BUCKET, "level " + lavaLevel + ", hand " + afterLava);
            if (fuelName == null) {
                ctx.line("wp10b fuel generator: SKIP bucket fill (no bucketable gasoline fuel registered); tank set to 0.5");
            } else {
                ctx.check("wp10b fuel generator takes a bucket of " + fuelName + ": half tank, empty bucket back",
                    Math.abs(level0 - 0.5) < 1e-9 && afterFuel.getItem() == Items.BUCKET, "level " + level0 + ", hand " + afterFuel);
            }
            boolean on = (Boolean) callPrefix(gen, "getOn");
            double umax = 50 * 1.05;
            ElectricalLoad pos = field(gen, "positiveLoad");
            ctx.check("wp10b fuel generator running", on, "on " + on);
            ctx.checkValue("wp10b fuel generator open-circuit voltage [V] (Umax = 1.05 * 50)", pos.getU(), umax);
            double pNom = 300 * Eln.fuelGeneratorPowerFactor;
            double rate = 1.375 * 0.1 * pNom / (Eln.fuelGeneratorTankCapacity * pNom);
            double level = field(gen, "tankLevel");
            within(ctx, f("wp10b fuel generator fuel used in %.2f s (1.375 * 0.1 P_nom / tank energy * t)", t), level0 - level, rate * t,
                rate * 1.5 * TICK + rate * t * 0.01);
        }
    }

    /**
     * Small Solar Panel: the power source is set to Pmax * light, light = cos(panel angle - sun angle), halved by rain
     * and by thunder, attenuated by blocks on the ray to the sun (SolarPannelSlowProcess.getSolarLight, refreshed every
     * 0.1..0.2 s). Checked at measure time against the same function: P = Pmax * light (0 at night).
     */
    static final class SolarCase implements SelfTestCase {
        SolarPanelElement panel;

        public String name() {
            return "wp10b solar panel";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            panel = (SolarPanelElement) ctx.placeTransparent(SOLAR_SMALL, ctx.at(2, 1, 1));
        }

        public void measure(SelfTestContext ctx) {
            Object process = field(panel, "slowProcess");
            double light = (double) Wp9cCases.call(process, "getSolarLight");
            double pmax = field(field(panel, "descriptor"), "electricalPmax");
            PowerSourceBipole src = field(panel, "powerSource");
            within(ctx, f("wp10b solar panel power [W] (Pmax %.1f * light %.4f)", pmax, light), src.getP(), pmax * light,
                0.01 * pmax * light + 1e-9);
        }
    }

    /**
     * Wind Turbine: P = PfW(wind) with wind = |Eln wind at the rotor height| * environment factor (1 - 0.07 per block
     * around the rotor beyond 2); checked against its own getWind() at measure time; 7 ghost blocks (mast + blades).
     * Water Turbine without water (its water block, the platform stone under x7, is made air: placement needs air/water): water
     * factor -1 -> power stays 0.
     */
    static final class WindWaterCase implements SelfTestCase {
        WindTurbineElement wind;
        WaterTurbineElement water;

        public String name() {
            return "wp10b wind + water turbine";
        }

        public int width() {
            return 8;
        }

        public void build(SelfTestContext ctx) {
            wind = (WindTurbineElement) ctx.placeTransparent(WIND_TURBINE, ctx.at(2, 1, 1));
            // WaterTurbineDescriptor.checkCanPlaceWater: the water block (waterCoord rotated by the front, here
            // (1,-1,0) = the platform stone under x7) must be air or water, else placement is refused. Make it air.
            BlockPos wp = ctx.at(6, 1, 1);
            mods.eln.misc.Coordonate wc = field(Eln.transparentNodeItem.getDescriptor(WATER_TURBINE), "waterCoord");
            int[] v = {wc.x, wc.y, wc.z};
            Direction.XN.rotateFromXN(v);
            ctx.world().setBlockToAir(wp.add(v[0], v[1], v[2]));
            water = (WaterTurbineElement) placeFacing(ctx, WATER_TURBINE, wp, Direction.XN);
        }

        public void measure(SelfTestContext ctx) {
            Object process = field(wind, "slowProcess");
            double w = (double) Wp9cCases.call(process, "getWind");
            FunctionTable pfw = field(field(wind, "descriptor"), "PfW");
            PowerSource ps = field(wind, "powerSource");
            within(ctx, f("wp10b wind turbine power [W] (PfW(wind %.3f))", w), ps.getP(), pfw.getValue(w), 0.01 * pfw.getValue(w) + 1e-9);
            int g = ghosts(ctx, pos(wind), 3);
            ctx.check("wp10b wind turbine ghost blocks (mast 3 + blades 4)", g == 7, g + " ghost block(s)");
            Object wp = field(water, "slowProcess");
            double factor = (double) Wp9cCases.call(wp, "getWaterFactor");
            PowerSource wps = field(water, "powerSource");
            ctx.check("wp10b water turbine without water: factor -1, P = 0", factor == -1 && Math.abs(wps.getP()) < 1e-9,
                f("factor %.3f, P %.4f W", factor, wps.getP()));
        }
    }

    /**
     * Low Power Transmitter / Receiver Antennas 4 blocks apart, facing each other (fronts set as when placed against
     * walls: tx +X, rx -X). The tx finds the rx on its first scan; efficiency = 1 - (offset + perBlock * distance) with
     * offset = 1 - 0.9, perBlock = (0.9 - 0.7) / range 200 -> 0.896 at 4 blocks, * 0.707 when raining, * 0.707 when
     * thundering.
     */
    static final class AntennaCase implements SelfTestCase {
        ElectricalAntennaTxElement tx;
        ElectricalAntennaRxElement rx;

        public String name() {
            return "wp10b antennas";
        }

        public int width() {
            return 6;
        }

        public void build(SelfTestContext ctx) {
            tx = (ElectricalAntennaTxElement) ctx.placeTransparent(ANTENNA_TX_LOW, ctx.at(1, 1, 1));
            rx = (ElectricalAntennaRxElement) ctx.placeTransparent(ANTENNA_RX_LOW, ctx.at(5, 1, 1));
            tx.front = Direction.XP;
            rx.front = Direction.XN;
            tx.reconnect();
            rx.reconnect();
        }

        public void measure(SelfTestContext ctx) {
            Object found = field(tx, "rxElement");
            ctx.check("wp10b antenna tx found the rx 4 blocks ahead", found == rx, String.valueOf(found));
            double eff = 1 - ((1 - 0.9) + (0.9 - 0.7) / 200 * 4);
            if (ctx.world().getWorldInfo().isRaining()) eff *= 0.707;
            if (ctx.world().getWorldInfo().isThundering()) eff *= 0.707;
            double measured = field(tx, "powerEfficency");
            ctx.checkValue("wp10b antenna link efficiency (1 - 0.1 - 0.001 * 4, weather)", measured, eff);
        }
    }

    /**
     * Experimental Transporter: spawn delta (4, 0, 0) and 22 ghost blocks (platform, walls, roof, floor marker, door
     * frame) plus its closed door (2). Placed so its front is -X (no rotation of the ghost group). Broken: the ghosts go.
     */
    static final class TeleporterCase implements SelfTestCase {
        TeleporterElement tp;
        BlockPos clicked;

        public String name() {
            return "wp10b teleporter";
        }

        public int width() {
            return 8;
        }

        public void build(SelfTestContext ctx) {
            clicked = ctx.at(1, 1, 1);
            tp = (TeleporterElement) placeFacing(ctx, TELEPORTER, clicked, Direction.XN);
        }

        public void measure(SelfTestContext ctx) {
            BlockPos p = pos(tp);
            ctx.check("wp10b teleporter node at the clicked position + spawn delta (4, 0, 0)", p.equals(clicked.add(4, 0, 0)), String.valueOf(p));
            int g = ghosts(ctx, p, 4);
            ctx.check("wp10b teleporter ghost blocks (22 structure + 2 closed door)", g == 24 || g == 22, g + " ghost block(s)");
            ctx.world().setBlockToAir(p);
            int left = ghosts(ctx, p, 4);
            ctx.check("wp10b teleporter broken: ghosts removed, node gone", left == 0
                && NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p.getX(), p.getY(), p.getZ(), ctx.world())) == null,
                left + " ghost(s) left");
        }
    }

    /** Placement + idle state without power: turret (energy buffer 0), stone heat furnace and fuel heat furnace at ambient. */
    static final class IdleCase implements SelfTestCase {
        TurretElement turret;
        HeatFurnaceElement furnace;
        TransparentNodeElement fuelFurnace;

        public String name() {
            return "wp10b idle devices";
        }

        public int width() {
            return 7;
        }

        public void build(SelfTestContext ctx) {
            turret = (TurretElement) ctx.placeTransparent(TURRET, ctx.at(0, 1, 1));
            furnace = (HeatFurnaceElement) ctx.placeTransparent(HEAT_FURNACE, ctx.at(3, 1, 1));
            fuelFurnace = ctx.placeTransparent(FUEL_HEAT_FURNACE, ctx.at(6, 1, 1));
        }

        public void measure(SelfTestContext ctx) {
            ctx.check("wp10b turret idle without power: energy buffer 0", turret.energyBuffer == 0,
                f("buffer %.3f J, charge power %.1f W", turret.energyBuffer, turret.chargePower));
            within(ctx, "wp10b stone heat furnace without fuel stays at ambient [K]", furnace.thermalLoad.Tc, 0, 1e-6);
            ctx.check("wp10b fuel heat furnace placed", fuelFurnace instanceof mods.eln.transparentnode.FuelHeatFurnaceElement,
                String.valueOf(fuelFurnace));
        }
    }
}
