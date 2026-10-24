package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.transparentnode.autominer.AutoMinerElement;
import mods.eln.item.LampDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketElement;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.DielectricItem;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.item.HeatingCorpElement;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.ghost.GhostGroup;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.transparentnode.eggincubator.EggIncubatorContainer;
import mods.eln.transparentnode.eggincubator.EggIncubatorElement;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement;
import mods.eln.transparentnode.electricalmachine.ElectricalMachineElement;
import mods.eln.transparentnode.powercapacitor.PowerCapacitorElement;
import mods.eln.transparentnode.powerinductor.PowerInductorElement;
import mods.eln.transparentnode.transformer.TransformerContainer;
import mods.eln.transparentnode.transformer.TransformerElement;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Selftest cases of device batch Wp10a (registry/batch/Wp10aContent). Every case feeds its device from a 50 V
 * electrical source through one LV cable laid on the side the device takes power from (read from the placed
 * element's front, so the cases don't depend on the fake player's facing). Model as in SelfTest.measure: two loads
 * connect through Rs(a) + Rs(b); source, LV cable and every load set by lowVoltageCableDescriptor/applySmallRs have
 * Rs = rs = Rs(LV cable).
 * Items owned by wp12 (ferromagnetic core, dielectric, copper cable, heating corp) are looked up by their 1.7.10
 * damage ids; while wp12 isn't merged they are missing and the checks that need them print SKIP (no PASS/FAIL).
 * Not tested: autominer mining (only placement, ghosts, power nodes, idle and removal); machine processing (recipes are
 * WP15, so the machines idle).
 */
public final class Wp10aCases {
    private Wp10aCases() {
    }

    static final int SOURCE = 0 + (3 << 6);        // Electrical Source
    static final int LV_CABLE = 4 + (32 << 6);     // Low Voltage Cable
    static final int GROUND = 0 + (2 << 6);        // Ground Cable
    static final int LAMP_SOCKET = 4 + (64 << 6);  // Robust Lamp Socket
    static final int LAMP_BULB = 0 + (4 << 6);     // Small 50V Incandescent Light Bulb
    static final int POWER_INDUCTOR = 16 + (1 << 6);
    static final int POWER_CAPACITOR = 20 + (1 << 6);
    static final int TRANSFORMER = 0 + (2 << 6);   // DC-DC Converter
    static final int FURNACE = 0 + (32 << 6);
    static final int EGG_INCUBATOR = 0 + (41 << 6);
    static final int AUTO_MINER = 0 + (42 << 6);
    static final int[] MACHINES = {0 + (33 << 6), 0 + (35 << 6), 0 + (36 << 6), 0 + (37 << 6),
        4 + (33 << 6), 4 + (35 << 6), 4 + (36 << 6), 4 + (37 << 6)}; // 50V then 200V macerator/compressor/magnetizer/plate machine
    // wp12-owned shared items (1.7.10 ids)
    static final int HEATING_CORP_SMALL_50V = 0 + (1 << 6);
    static final int CORE_CHEAP = 0 + (7 << 6);    // cableMultiplicator 10
    static final int CORE_OPTIMAL = 2 + (7 << 6);  // cableMultiplicator 1
    static final int COPPER_CABLE = 0 + (65 << 6);
    static final int DIELECTRIC = 52 + (120 << 6);
    static final double U = 50.0;

    public static void addTo(List<TestCase> cases) {
        cases.add(new EggIncubatorCase());
        cases.add(new FurnaceCase());
        cases.add(new FurnaceCharCase());
        cases.add(new SmelterCase());
        cases.add(new MachinesCase());
        cases.add(new MaceratorProcessCase());
        cases.add(new MaceratorReverseCase());
        cases.add(new MaceratorFieldSpeedCase());
        cases.add(new SingleUseChargeCase());
        cases.add(new TransformerCase());
        cases.add(new CapacitorCase());
        cases.add(new InductorCase());
        cases.add(new AutoMinerCase());
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

    /** wp12 shared item, or EMPTY when it isn't registered (wp12 not merged). */
    static ItemStack shared(int damage, Class<?> cls, int count) {
        GenericItemUsingDamageDescriptor d = Eln.sharedItem.getDescriptor(damage);
        if (d == null || !cls.isInstance(d)) return ItemStack.EMPTY;
        return d.newItemStack(count);
    }

    static ItemStack lvCables(int count) {
        return Eln.sixNodeItem.getDescriptor(LV_CABLE).newItemStack(count);
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

    static double loadU(SixNodeElement e) {
        return e.getElectricalLoad(LRDU.Up).getU();
    }

    /**
     * Lays six-node devices on a case platform next to transparent nodes, keeping them from touching anything but
     * the block they hang off (an unwanted contact would add a parallel connection to the model).
     */
    public static final class Layout {
        final TestContext ctx;
        final int width;
        final List<BlockPos> used = new ArrayList<>();

        public Layout(TestContext ctx, int width) {
            this.ctx = ctx;
            this.width = width;
        }

        boolean onPlatform(BlockPos p) {
            BlockPos o = ctx.at(0, 1, 0);
            int dx = p.getX() - o.getX(), dz = p.getZ() - o.getZ();
            return p.getY() == o.getY() && dx >= 0 && dx < width && dz >= 0 && dz <= 2;
        }

        TransparentNodeElement device(int damage, int dx) {
            return device(damage, dx, 1);
        }

        public TransparentNodeElement device(int damage, int dx, int dz) {
            BlockPos p = ctx.at(dx, 1, dz);
            used.add(p);
            return ctx.placeTransparent(damage, p);
        }

        /** Free spot next to `from` (prefer straight on along `dir`) touching nothing used except `from`. */
        BlockPos next(BlockPos from, Direction dir) {
            List<Direction> order = new ArrayList<>();
            order.add(dir);
            for (Direction d : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP})
                if (d != dir) order.add(d);
            for (Direction d : order) {
                BlockPos c = off(from, d);
                if (!onPlatform(c) || used.contains(c)) continue;
                boolean touches = false;
                for (Direction n : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) {
                    BlockPos nb = off(c, n);
                    if (!nb.equals(from) && used.contains(nb)) touches = true;
                }
                if (!touches) return c;
            }
            throw new IllegalStateException("no free spot next to " + from);
        }

        SixNodeElement six(int damage, BlockPos p) {
            used.add(p);
            return ctx.placeSix(damage, p);
        }

        /** LV cable on `side` of the device at p, and the 50 V source next to it. Returns {source, cable}. */
        public SixNodeElement[] feed(BlockPos device, Direction side) {
            BlockPos cp = off(device, side);
            if (!onPlatform(cp) || used.contains(cp)) throw new IllegalStateException("feed side " + side + " blocked at " + cp);
            SixNodeElement cable = six(LV_CABLE, cp);
            ElectricalSourceElement src = (ElectricalSourceElement) six(SOURCE, next(cp, side));
            src.networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat((float) U);
            }));
            return new SixNodeElement[]{src, cable};
        }
    }

    // ---- cases ----

    /** Egg incubator: with an egg its resistor is Rp = Unom^2/Pnom = 50 ohm (50 V, 50 W). */
    static final class EggIncubatorCase implements TestCase {
        EggIncubatorElement inc;
        SixNodeElement cable;

        public String name() {
            return "wp10a egg incubator";
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            inc = (EggIncubatorElement) l.device(EGG_INCUBATOR, 3);
            cable = l.feed(ctx.at(3, 1, 1), inc.front.left())[1];
            inc.getInventory().setItem(EggIncubatorContainer.EggSlotId, new ItemStack(Items.EGG));
            inc.getInventory().setChanged();
        }

        public void measure(TestContext ctx) {
            double rp = U * U / 50.0, r = rs();
            // src(rs)-cable(rs): 2rs; cable(rs)-incubator powerLoad(rs, LV cable): 2rs; Rp to ground
            double i = U / (4 * r + rp);
            ctx.line(String.format("wp10a egg incubator model: Rp = 50 ohm, I = U/(4Rs+Rp) = %.5f A, P = %.4f W", i, i * i * rp));
            ctx.checkValue("wp10a egg incubator R [ohm]", inc.powerResistor.getR(), rp);
            ctx.checkValue("wp10a egg incubator voltage [V]", inc.powerLoad.getU(), i * rp);
            ctx.checkValue("wp10a egg incubator power [W]", inc.powerResistor.getP(), i * i * rp);
            ctx.checkValue("wp10a egg incubator feed cable voltage [V]", loadU(cable), U - i * 2 * r);
        }
    }

    /**
     * Electrical furnace, fed on its back: cobblestone (vanilla smelting recipe) in the input slot makes the
     * auto-shutdown logic switch it on. With the Small 50V heating corp (wp12): regulator "none" keeps
     * R = Unom^2/Pnom = 2500/150 ohm and the thermal load warms up.
     */
    static final class FurnaceCase implements TestCase {
        ElectricalFurnaceElement furnace;
        boolean heating;

        public String name() {
            return "wp10a electrical furnace";
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            furnace = (ElectricalFurnaceElement) l.device(FURNACE, 3);
            l.feed(ctx.at(3, 1, 1), furnace.front.getInverse());
            furnace.getInventory().setItem(ElectricalFurnaceElement.inSlotId, new ItemStack(Blocks.COBBLESTONE, 4));
            ItemStack corp = shared(HEATING_CORP_SMALL_50V, HeatingCorpElement.class, 1);
            heating = !corp.isEmpty();
            if (heating) furnace.getInventory().setItem(ElectricalFurnaceElement.heatingCorpSlotId, corp);
            furnace.getInventory().setChanged();
        }

        public void measure(TestContext ctx) {
            boolean on = field(furnace, "powerOn");
            ctx.check("wp10a furnace switched on by a smeltable input (auto shutdown)", on, "powerOn " + on);
            if (!heating) {
                ctx.line("SKIP wp10a furnace heating: Small 50V Copper Heating Corp not registered (wp12)");
                return;
            }
            double rh = U * U / 150.0, r = rs();
            double i = U / (4 * r + rh); // src-cable 2rs, cable-furnace load (LV cable Rs) 2rs
            Resistor heat = field(furnace, "heatingCorpResistor");
            ThermalLoad t = field(furnace, "thermalLoad");
            ctx.line(String.format("wp10a furnace model: Rheat = %.4f ohm, P = I^2 R = %.3f W", rh, i * i * rh));
            ctx.checkValue("wp10a furnace heating resistor [ohm]", heat.getR(), rh);
            ctx.checkValue("wp10a furnace heating power [W]", heat.getP(), i * i * rh);
            ctx.check("wp10a furnace warms up", t.Tc > 0.1, String.format("T = %.3f C above ambient", t.Tc));
        }
    }

    /**
     * Baughn: above 280 °C an electrical oven chars food to a charcoal nugget instead of cooking it; below, beef cooks;
     * ore smelts as ever at any heat. Four nuggets make a charcoal, and they burn (a quarter of a charcoal).
     */
    static final class FurnaceCharCase implements TestCase {
        String got = "";
        boolean ok;

        public String name() {
            return "wp10a electrical oven chars food above 280 C";
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            ElectricalFurnaceElement f = (ElectricalFurnaceElement) l.device(FURNACE, 3);
            mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess p = field(f, "slowRefreshProcess");
            ThermalLoad t = field(f, "thermalLoad");
            var nugget = mods.eln.registry.ElnRegistry.CHARCOAL_NUGGET.get();
            f.getInventory().setItem(ElectricalFurnaceElement.inSlotId, new ItemStack(Items.BEEF));
            t.Tc = 250;
            ItemStack cool = p.getSmeltResult();
            t.Tc = 270;   // (+ the room: 290 °C)
            ItemStack hot = p.getSmeltResult();
            f.getInventory().setItem(ElectricalFurnaceElement.inSlotId, new ItemStack(Items.RAW_IRON));
            ItemStack ore = p.getSmeltResult();
            t.Tc = 0;
            var fuel = new ItemStack(nugget).get(net.minecraft.core.component.DataComponents.COOKING_FUEL);
            int burn = fuel != null && fuel.burnTime() instanceof net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant k ? k.value() : 0;
            got = "250 C: " + cool + ", 270 C: " + hot + ", raw iron at 270 C: " + ore + ", nugget burns " + burn;
            ok = cool.is(Items.COOKED_BEEF) && hot.is(nugget) && ore.is(Items.IRON_INGOT) && burn == 400;
            f.getInventory().setItem(ElectricalFurnaceElement.inSlotId, ItemStack.EMPTY);
        }

        public void measure(TestContext ctx) {
            ctx.check("wp10a oven: beef cooks at 250 C, chars at 270 C over the room; ore smelts; a nugget burns 400 ticks", ok, got);
        }
    }

    /**
     * The smelter (Baughn 2026-10-23): held at 400 °C it smelts at the electrical oven's rate for that heat (PfT 240 W,
     * 1000 J an item: ~4.2 s), at 240 °C at 80 W (12.5 s); without 50 V for its controls it doesn't smelt; heat put into a
     * thermal cable behind it reaches it; hoppers feed the top and empty the bottom only.
     */
    static final class SmelterCase implements TestCase {
        static final int SMELTER = 24 + (82 << 6);
        mods.eln.transparentnode.smelter.SmelterElement hot, warm, unpowered, cabled;
        mods.eln.sim.IProcess hold;
        SixNodeElement cable;
        String hoppers;
        boolean hoppersOk;
        net.minecraft.world.level.block.entity.ChestBlockEntity chest;

        public String name() {
            return "wp10a smelter";
        }

        public int width() {
            return 15;
        }

        public int ticks() {
            return 300;
        }

        mods.eln.transparentnode.smelter.SmelterElement place(TestContext ctx, Layout l, int x, boolean fed) {
            var m = (mods.eln.transparentnode.smelter.SmelterElement) l.device(SMELTER, x, 1);
            if (fed) l.feed(ctx.at(x, 1, 1), m.front.left());
            m.getInventory().setItem(mods.eln.transparentnode.smelter.SmelterElement.inSlotId, new ItemStack(Items.RAW_IRON, 16));
            m.getInventory().setChanged();
            return m;
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            hot = place(ctx, l, 1, true);
            warm = place(ctx, l, 5, true);
            unpowered = place(ctx, l, 9, false);
            cabled = place(ctx, l, 13, true);
            cable = l.six(Wp9bCases.THERMAL_CABLE, off(ctx.at(13, 1, 1), cabled.front.getInverse()));
            ThermalLoad th = field(hot, "thermalLoad"), tw = field(warm, "thermalLoad"), tu = field(unpowered, "thermalLoad");
            mods.eln.Eln.simulator.addSlowProcess(hold = t -> {
                th.Tc = 400;   // (EA temperatures are above the room's, as the oven's PfT table)
                tw.Tc = 240;
                tu.Tc = 400;
                cable.getThermalLoad(LRDU.Up).moveEnergyTo(3000 * t);   // (3 kW into the cable behind the fourth)
            });
            var inv = (mods.eln.transparentnode.smelter.SmelterInventory) hot.getInventory();
            int up = net.minecraft.core.Direction.UP.get3DDataValue(), down = net.minecraft.core.Direction.DOWN.get3DDataValue();
            boolean topIn = inv.canInsertItem(0, new ItemStack(Items.RAW_IRON), up), topJunk = inv.canInsertItem(0, new ItemStack(Items.STICK), up);
            boolean botOut = inv.canExtractItem(1, new ItemStack(Items.IRON_INGOT), down), topOut = inv.canExtractItem(1, new ItemStack(Items.IRON_INGOT), up);
            hoppers = "top takes raw iron " + topIn + ", sticks " + topJunk + "; bottom gives out " + botOut + ", top " + topOut;
            hoppersOk = topIn && !topJunk && botOut && !topOut;
            // a chest on the unpowered one (which doesn't smelt): its funnel pulls the raw iron, not the sticks (Baughn)
            net.minecraft.core.BlockPos cp = ctx.at(9, 2, 1);
            ctx.world().setBlock(cp, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
            chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) ctx.world().getBlockEntity(cp);
            chest.setItem(0, new ItemStack(Items.STICK, 4));
            chest.setItem(1, new ItemStack(Items.RAW_IRON, 5));
        }

        static int out(mods.eln.transparentnode.smelter.SmelterElement m) {
            return m.getInventory().getItem(mods.eln.transparentnode.smelter.SmelterElement.outSlotId).getCount();
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(hold);
            ThermalLoad tc = field(cabled, "thermalLoad");
            ctx.line(String.format("smelter: 400 C made %d, 240 C %d, unpowered %d; cable-fed at %.0f C made %d", out(hot), out(warm), out(unpowered),
                tc.Tc + mods.eln.sim.PhysicalConstant.Tamb, out(cabled)));
            ctx.check("smelter at 400 C over the room: 240 W, ~4.2 s an ingot (3 in 15 s)", out(hot) == 3, "" + out(hot));
            ctx.check("smelter at 240 C over the room: 80 W, 12.5 s an ingot (1 in 15 s)", out(warm) == 1, "" + out(warm));
            ctx.check("smelter without 50 V: nothing", out(unpowered) == 0, "" + out(unpowered));
            ctx.check("smelter: heat from the thermal cable behind it warms it", tc.Tc > 50, String.format("%.1f C above ambient", tc.Tc));
            ctx.check("smelter: hoppers feed the top (only what smelts) and empty the bottom", hoppersOk, hoppers);
            int pulled = unpowered.getInventory().getItem(mods.eln.transparentnode.smelter.SmelterElement.inSlotId).getCount() - 16;
            ctx.check("smelter: its funnel pulls what smelts from a chest on top, and leaves the rest",
                pulled == 5 && chest.getItem(1).isEmpty() && chest.getItem(0).getCount() == 4,
                "pulled " + pulled + ", chest raw iron " + chest.getItem(1).getCount() + ", sticks " + chest.getItem(0).getCount());
        }
    }

    /**
     * Macerator/compressor/magnetizer/plate machine, 50 V and 200 V: placement. The 50 V macerator is fed: with no
     * recipe (WP15) it idles, so its resistor is open and its node sits at the source voltage.
     */
    static final class MachinesCase implements TestCase {
        final TransparentNodeElement[] machines = new TransparentNodeElement[MACHINES.length];

        public String name() {
            return "wp10a electrical machines";
        }

        public int width() {
            return 16;
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            // fed macerator at x=2; the others at least diagonal apart, never touching each other or the feed
            int[][] at = {{2, 1}, {6, 1}, {8, 1}, {10, 1}, {12, 1}, {14, 1}, {7, 0}, {11, 2}};
            for (int k = 0; k < MACHINES.length; k++) machines[k] = l.device(MACHINES[k], at[k][0], at[k][1]);
            l.feed(ctx.at(2, 1, 1), machines[0].front.left());
        }

        public void measure(TestContext ctx) {
            int ok = 0;
            StringBuilder s = new StringBuilder();
            for (int k = 0; k < machines.length; k++) {
                boolean m = machines[k] instanceof ElectricalMachineElement
                    && machines[k].getDescriptor() == Eln.transparentNodeItem.getDescriptor(MACHINES[k]);
                if (m) ok++;
                else s.append(" ").append(MACHINES[k]);
            }
            ctx.check("wp10a machines placed", ok == MACHINES.length, ok + "/" + MACHINES.length + (s.length() > 0 ? ", wrong:" + s : ""));
            ElectricalLoad load = field(machines[0], "electricalLoad");
            Resistor res = field(machines[0], "electricalResistor");
            ctx.checkValue("wp10a idle macerator node voltage [V] (empty input: resistor open)", load.getU(), U);
            ctx.check("wp10a idle macerator draws no power", Math.abs(res.getP()) < 0.01, String.format("P = %.6f W", res.getP()));
        }
    }

    /**
     * 50V macerator (200 W) fed at 50 V with a vanilla copper ingot (26.x port, P5 step 4: the machine recipes run):
     * the 1.12 recipe is EA Copper Ingot (now vanilla's) -> 1 Copper Dust for 0.5 * 4000 J, and its input matches c:ingots/copper as
     * 1.12 matched oreDictionary ingotCopper. Expected after 260 ticks (13 s; 2000 J at up to 200 W takes >= 10 s):
     * input slot empty, one Copper Dust in the output slot.
     */
    static final class MaceratorProcessCase implements TestCase {
        ElectricalMachineElement macerator;

        public String name() {
            return "wp10a macerator processes copper";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            macerator = (ElectricalMachineElement) l.device(MACHINES[0], 2, 1);
            l.feed(ctx.at(2, 1, 1), macerator.front.left());
            macerator.getInventory().setItem(macerator.inSlotId, new ItemStack(Items.COPPER_INGOT));
            macerator.getInventory().setChanged();
        }

        public void measure(TestContext ctx) {
            ItemStack in = macerator.getInventory().getItem(macerator.inSlotId);
            ItemStack out = macerator.getInventory().getItem(macerator.outSlotId);
            ItemStack dust = Eln.findItemStack("Copper Dust");
            ctx.check("macerator: vanilla copper ingot (c:ingots/copper) -> 1 EA Copper Dust", in.isEmpty()
                && out.getItem() == dust.getItem() && out.getCount() == 1, "in " + in + ", out " + out);
        }
    }

    /**
     * A single-use battery on a charger (Baughn: they could be charged): fed 100 V on its positive side, 0 V on its
     * negative: it stores nothing (its charge doesn't rise) and heats until it burns (catches fire).
     */
    static final class SingleUseChargeCase implements TestCase {
        mods.eln.transparentnode.battery.BatteryElement battery;
        double startCharge;

        public String name() {
            return "wp10a single-use battery can't be charged";
        }

        public int width() {
            return 8;
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            battery = (mods.eln.transparentnode.battery.BatteryElement) l.device(5 + (16 << 6), 3, 1);
            startCharge = battery.batteryProcess.getCharge();
            SixNodeElement[] plus = l.feed(ctx.at(3, 1, 1), battery.front.left());
            ((ElectricalSourceElement) plus[0]).networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat(100f);
            }));
            SixNodeElement[] minus = l.feed(ctx.at(3, 1, 1), battery.front.right());
            ((ElectricalSourceElement) minus[0]).networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat(0f);
            }));
        }

        public void measure(TestContext ctx) {
            double q = battery.batteryProcess.getCharge();
            ctx.check("single-use battery: charging stores nothing", q <= startCharge + 1e-6, String.format("charge %.4f -> %.4f", startCharge, q));
            ctx.check("single-use battery: charged, it overheats and burns", battery.isBurnt() || battery.thermalLoad.Tc > battery.descriptor.thermalWarmLimit * 0.8,
                String.format("burnt %s, T %.1f (limit %.0f)", battery.isBurnt(), battery.thermalLoad.Tc, battery.descriptor.thermalWarmLimit));
        }
    }

    /**
     * The macerator fed -50 V (Baughn: a generator on one side and a ground cable on the other burned one as "overvoltage"):
     * a resistor to ground on one pin, so either polarity runs it: it processes the copper and isn't burnt; and a
     * voltage watchdog tripped below its minimum names the cause reverse voltage.
     */
    static final class MaceratorReverseCase implements TestCase {
        ElectricalMachineElement macerator;

        public String name() {
            return "wp10a macerator runs on reverse voltage";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            macerator = (ElectricalMachineElement) l.device(MACHINES[0], 2, 1);
            SixNodeElement[] f = l.feed(ctx.at(2, 1, 1), macerator.front.left());
            ((ElectricalSourceElement) f[0]).networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat((float) -U);
            }));
            macerator.getInventory().setItem(macerator.inSlotId, new ItemStack(Items.COPPER_INGOT));
            macerator.getInventory().setChanged();
        }

        public void measure(TestContext ctx) {
            ItemStack in = macerator.getInventory().getItem(macerator.inSlotId);
            ItemStack out = macerator.getInventory().getItem(macerator.outSlotId);
            ctx.check("reverse-fed macerator not burnt", !macerator.isBurnt(), "burnt " + macerator.isBurnt());
            ctx.check("reverse-fed macerator processes copper", in.isEmpty() && out.getCount() == 1, "in " + in + ", out " + out);
            var wd = new mods.eln.sim.process.destruct.VoltageStateWatchDog().setUNominal(50);
            ctx.check("a watchdog under its minimum names reverse voltage",
                mods.eln.sim.process.destruct.FailureCause.REVERSE_VOLTAGE == wd.causeFor(-30)
                    && mods.eln.sim.process.destruct.FailureCause.OVERVOLTAGE == wd.causeFor(80), "");
        }
    }

    /**
     * Stasis Delvers' acceleration fields (NodeBase.timeScale, λ-Nero): a macerator where time runs 3x draws 3x the
     * power and grinds 3x as fast, for the same energy per item; one beside it at 1x for comparison.
     */
    static final class MaceratorFieldSpeedCase implements TestCase {
        ElectricalMachineElement fast, normal;
        java.util.function.ToDoubleFunction<mods.eln.node.NodeBase> before;

        public String name() {
            return "wp10a macerator in a 3x acceleration field";
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 600;
        }

        ElectricalMachineElement place(TestContext ctx, Layout l, int x) {
            ElectricalMachineElement m = (ElectricalMachineElement) l.device(MACHINES[0], x, 1);
            SixNodeElement[] f = l.feed(ctx.at(x, 1, 1), m.front.left());
            ((ElectricalSourceElement) f[0]).networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat((float) U);
            }));
            m.getInventory().setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT, 16));
            m.getInventory().setChanged();
            return m;
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            normal = place(ctx, l, 2);
            fast = place(ctx, l, 6);
            before = mods.eln.node.NodeBase.timeScale;
            var prev = before;
            mods.eln.node.NodeBase.timeScale = n -> n == fast.node ? 3.0 : prev == null ? 1.0 : prev.applyAsDouble(n);
        }

        static double pw(ElectricalMachineElement m) {
            return Batch1Cases.<mods.eln.sim.mna.component.Resistor>field(m, "electricalResistor").getP();
        }

        public void measure(TestContext ctx) {
            mods.eln.node.NodeBase.timeScale = before;
            int a = fast.getInventory().getItem(fast.outSlotId).getCount(), b = normal.getInventory().getItem(normal.outSlotId).getCount();
            ctx.line("macerator: 3x made " + a + " dust, 1x made " + b + "; power " + pw(fast) + " W vs " + pw(normal) + " W");
            ctx.check("3x: about three times the dust (each count rounds down)", b > 0 && a >= 3 * b && a <= 3 * b + 3, a + " vs " + b);
            ctx.check("3x: about three times the power", Math.abs(pw(fast) / pw(normal) - 3) < 0.3,
                pw(fast) + " W vs " + pw(normal) + " W");
        }
    }

    /**
     * DC-DC converter: primary (left) 2 LV cables, secondary (right) 1 LV cable, cheap core (wp12); secondary open:
     * U2 = U1 * n2/n1 = 25 V. Without a core both windings are open: only the primary side is checked.
     */
    static final class TransformerCase implements TestCase {
        TransformerElement tr;
        SixNodeElement primary, secondary;
        boolean core;

        public String name() {
            return "wp10a transformer";
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            BlockPos p = ctx.at(3, 1, 1);
            tr = (TransformerElement) l.device(TRANSFORMER, 3);
            primary = l.feed(p, tr.front.left())[1];
            secondary = l.six(LV_CABLE, off(p, tr.front.right()));
            tr.getInventory().setItem(TransformerContainer.primaryCableSlotId, lvCables(2));
            tr.getInventory().setItem(TransformerContainer.secondaryCableSlotId, lvCables(1));
            ItemStack c = shared(CORE_CHEAP, FerromagneticCoreDescriptor.class, 1);
            core = !c.isEmpty();
            if (core) tr.getInventory().setItem(TransformerContainer.ferromagneticSlotId, c);
            tr.getInventory().setChanged();
        }

        public void measure(TestContext ctx) {
            ctx.checkValue("wp10a transformer primary cable voltage [V] (secondary open, no current)", loadU(primary), U);
            if (!core) {
                ctx.line("SKIP wp10a transformer ratio: Cheap Ferromagnetic Core not registered (wp12)");
                return;
            }
            ctx.checkValue("wp10a transformer secondary open-circuit voltage [V] (U1 * 1/2)", loadU(secondary), U / 2);
        }
    }

    /**
     * Power capacitor between the fed cable (left, +) and a ground cable (right, -). 1 redstone, 10 dielectrics
     * (wp12, 50 V each): Unom = 500 V, C = E6(-2)[0] / (Unom/50)^2 = 0.01/100 = 1e-4 F (small C keeps the inrush
     * current low); discharge resistor 300 s / C = 3 MOhm, so U ~ 50 V and E = C U^2 / 2 = 0.125 J.
     */
    static final class CapacitorCase implements TestCase {
        PowerCapacitorElement cap;
        SixNodeElement plus, ground;
        boolean diel;

        public String name() {
            return "wp10a power capacitor";
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            BlockPos p = ctx.at(3, 1, 1);
            cap = (PowerCapacitorElement) l.device(POWER_CAPACITOR, 3);
            plus = l.feed(p, cap.front.left())[1];
            ground = l.six(GROUND, off(p, cap.front.right()));
            ItemStack d = shared(DIELECTRIC, DielectricItem.class, 10);
            diel = !d.isEmpty();
            cap.getInventory().setItem(0, new ItemStack(Items.REDSTONE, 1)); // PowerCapacitorContainer.redId
            if (diel) cap.getInventory().setItem(1, d);                    // dielectricId
            cap.getInventory().setChanged();
        }

        public void measure(TestContext ctx) {
            if (!diel) {
                ctx.line("SKIP wp10a power capacitor: Dielectric not registered (wp12); placement only");
                ctx.check("wp10a power capacitor placed", cap != null, "");
                return;
            }
            Capacitor c = field(cap, "capacitor");
            double cExp = 0.01 / 100.0;
            ctx.checkValue("wp10a power capacitor C [F]", c.getC(), cExp);
            ctx.checkValue("wp10a power capacitor voltage [V]", Math.abs(c.getU()), U);
            ctx.checkValue("wp10a power capacitor energy [J]", c.getE(), 0.5 * cExp * U * U);
            ctx.checkValue("wp10a power capacitor + cable voltage [V]", loadU(plus), U);
        }
    }

    /**
     * Power inductor in series between the fed cable and a cable + robust lamp socket (50V bulb). 1 copper cable
     * (wp12): L = E12(-1)[0] = 0.1 H; optimal core (wp12, factor 1): Rs of both windings = rs. DC steady state
     * (L/R = 0.8 ms): I = U / (Rs chain + Rlamp), chain = src-c1 2rs, c1-L 2rs, L-c2 2rs, c2-lamp 2rs.
     */
    static final class InductorCase implements TestCase {
        PowerInductorElement ind;
        LampSocketElement lamp;
        boolean items;

        public String name() {
            return "wp10a power inductor";
        }

        public void build(TestContext ctx) {
            Layout l = new Layout(ctx, width());
            BlockPos p = ctx.at(3, 1, 1);
            ind = (PowerInductorElement) l.device(POWER_INDUCTOR, 3);
            l.feed(p, ind.front.left());
            BlockPos c2 = off(p, ind.front.right());
            l.six(LV_CABLE, c2);
            lamp = (LampSocketElement) l.six(LAMP_SOCKET, l.next(c2, ind.front.right()));
            lamp.getInventory().setItem(0, Eln.sharedItem.getDescriptor(LAMP_BULB).newItemStack()); // no cable: built in
            lamp.getInventory().setChanged();
            lamp.networkUnserialize(ctx.stream(out -> out.writeByte(3))); // LampSocketElement.tooglePowerSupplyType
            ItemStack copper = shared(COPPER_CABLE, mods.eln.item.CopperCableDescriptor.class, 1);
            ItemStack core = shared(CORE_OPTIMAL, FerromagneticCoreDescriptor.class, 1);
            items = !copper.isEmpty() && !core.isEmpty();
            if (items) {
                ind.getInventory().setItem(0, copper); // PowerInductorContainer.cableId
                ind.getInventory().setItem(1, core);   // coreId
                ind.getInventory().setChanged();
            }
        }

        public void measure(TestContext ctx) {
            if (!items) {
                ctx.line("SKIP wp10a power inductor: Copper Cable / Optimal Ferromagnetic Core not registered (wp12); placement only");
                ctx.check("wp10a power inductor placed", ind != null, "");
                return;
            }
            Inductor l = field(ind, "inductor");
            double rLamp = ((LampDescriptor) Eln.sharedItem.getDescriptor(LAMP_BULB)).getR();
            double i = U / (8 * rs() + rLamp);
            ctx.line(String.format("wp10a inductor model: L = 0.1 H, Rlamp = %.3f ohm, I = U/(8Rs+Rlamp) = %.5f A", rLamp, i));
            ctx.checkValue("wp10a power inductor L [H]", l.getL(), 0.1);
            ctx.checkValue("wp10a power inductor current [A]", Math.abs(l.getCurrent()), i);
            ctx.checkValue("wp10a lamp current through the inductor [A]", Math.abs(lamp.lampResistor.getCurrent()), i);
        }
    }


    /**
     * Auto miner (id 42): a multiblock (19 ghost blocks around the core, two power-input nodes on ghost positions).
     * Placed in the air (y = 3; it needs no floor) so the ghosts don't hit the platform. Unpowered, no drill/pipes:
     * job "none", power resistor open (P = 0). Then broken here: ghosts and power nodes must go with it.
     * Not covered: mining itself (needs an 800 V supply on the power nodes, drill, pipes and a chest).
     */
    static final class AutoMinerCase implements TestCase {
        AutoMinerElement miner;
        BlockPos pos;

        public String name() {
            return "wp10a auto miner";
        }

        /** The miner and its ghosts stand above the clicked block (GameTest boxes are closed by barriers). */
        public int height() {
            return 7;
        }

        public int width() {
            return 9;
        }

        /** Room for the ghost group whatever the front (1.12 placed it on an open row). */
        public int depth() {
            return 7;
        }

        public void build(TestContext ctx) {
            // placeBlockAt shifts the node by the spawn delta (2, 1, 0) rotated by the front: the node is not at the
            // clicked position (1.7.10 behaviour), so take its coordinate from the node
            miner = (AutoMinerElement) ctx.placeTransparent(AUTO_MINER, ctx.at(4, 3, 3));
            mods.eln.misc.Coordonate c = miner.node.coordonate;
            pos = new BlockPos(c.x, c.y, c.z);
        }

        int ghosts(TestContext ctx) {
            int n = 0;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3)))
                if (ctx.world().getBlockState(p).getBlock() == Eln.ghostBlock) n++;
            return n;
        }

        public void measure(TestContext ctx) {
            ctx.check("wp10a auto miner placed", miner != null && miner.getDescriptor() == Eln.transparentNodeItem.getDescriptor(AUTO_MINER),
                String.valueOf(miner));
            if (miner == null) return;
            GhostGroup group = field(miner.getDescriptor(), "ghostGroup");
            int expected = group.size();
            int found = ghosts(ctx);
            ctx.check("wp10a auto miner ghost blocks", expected == 19 && found == expected, found + " of " + expected + " (19 by its GhostGroup)");
            List<NodeBase> power = new ArrayList<>(Wp10aCases.<List<NodeBase>>field(miner, "powerNodeList")); // cleared on break
            int registered = 0;
            for (NodeBase n : power) if (NodeManager.instance.getNodeFromCoordonate(n.coordonate) == n) registered++;
            ctx.check("wp10a auto miner power nodes registered", power.size() == 2 && registered == 2, registered + "/" + power.size());
            Object job = field(field(miner, "slowProcess"), "job");
            Resistor res = field(miner, "powerResistor");
            ctx.check("wp10a auto miner idle (no power, no drill): job none, P = 0",
                "none".equals(String.valueOf(job)) && Math.abs(res.getP()) < 1e-6, "job " + job + String.format(", P = %.6f W", res.getP()));

            // break it (as the cleanup does): the ghosts and the power nodes go too
            ctx.world().removeBlock(pos, false);
            int left = ghosts(ctx);
            int nodesLeft = 0;
            for (NodeBase n : power) if (NodeManager.instance.getNodeFromCoordonate(n.coordonate) != null) nodesLeft++;
            ctx.check("wp10a auto miner removed with its ghosts and power nodes", left == 0 && nodesLeft == 0
                && NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos.getX(), pos.getY(), pos.getZ(), ctx.world())) == null,
                left + " ghost(s), " + nodesLeft + " power node(s) left");
        }
    }

}
