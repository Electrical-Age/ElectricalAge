package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeElement;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sixnode.AmplifierElement;
import mods.eln.sixnode.AnalogChipElement;
import mods.eln.sixnode.EmergencyLampElement;
import mods.eln.sixnode.logicgate.LogicGateElement;
import mods.eln.sixnode.SummingUnitElement;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorElement;
import mods.eln.sixnode.electricaldatalogger.DataLogs;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerContainer;
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceElement;
import mods.eln.sixnode.electricalmath.ElectricalMathElement;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.electricaltimeout.ElectricalTimeoutElement;
import mods.eln.sixnode.energymeter.EnergyMeterElement;
import mods.eln.sixnode.hub.HubElement;
import mods.eln.sixnode.modbusrtu.ModbusRtuElement;
import mods.eln.sixnode.resistor.ResistorElement;
import mods.eln.sixnode.tutorialsign.TutorialSignElement;
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor;
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxElement;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.lang.reflect.Method;
import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.f;
import static mods.eln.gametest.cases.Wp9aCases.link;
import static mods.eln.gametest.cases.Wp9aCases.orient;
import static mods.eln.gametest.cases.Batch1Cases.field;
import static mods.eln.gametest.cases.Batch1Cases.off;

/**
 * Selftest cases of device batch Wp9c (registry/batch/Wp9cContent): six-node logic and misc devices.
 * <p>
 * Signal devices are driven by core Signal Sources (voltage set through the GUI packet) and every gate output is
 * loaded by a signal cable. Gates are turned (element.front, as a wrench does) so that the output faces +X; side
 * inputs sit on whichever Z side the gate's left/right pin points to. SVU = 50 V (signal full scale); a logic
 * "1" output is SVU, "0" is 0 V; analog chips see raw volts. Time-dependent checks use the elapsed simulated time
 * between build and measure (world ticks * 0.05 s) with a +-1.5 tick tolerance, so they hold for any tick count.
 * Power circuits use the wp9a model: connection(a, b) = Rs(a) + Rs(b), 100 ohm Power Resistor as the load.
 * Not tested server-side: PAL / flip-flops / oscillator / PID / VCO / filter / sample-and-hold (same element code as
 * the tested gates and chips, different functions), wireless repeater and wireless buttons (same tx path),
 * Advanced Energy Meter (same element), 200V Emergency Lamp (same element), Modbus RTU traffic (no TCP server in
 * 1.12, only placement + its "disabled" state), tutorial sign overlay (client), analyser item use (needs a player
 * looking at a wireless device; only registration is checked).
 */
public final class Wp9cCases {
    private Wp9cCases() {
    }

    // damage = subId + (id << 6), 1.7.10 ids
    static final int SOURCE = 0 + (3 << 6);
    static final int SIGNAL_SOURCE = 1 + (3 << 6);
    static final int GROUND = 0 + (2 << 6);
    static final int SIGNAL_CABLE = 0 + (32 << 6);
    static final int LV_CABLE = 4 + (32 << 6);
    static final int POWER_RESISTOR = 36 + (96 << 6);
    static final int HUB = 8 + (2 << 6);
    static final int EMERGENCY_LAMP_50V = 15 + (64 << 6);
    static final int WIRELESS_RX = 0 + (92 << 6);
    static final int WIRELESS_TX = 8 + (92 << 6);
    static final int DATA_LOGGER = 0 + (93 << 6);
    static final int SIGNAL_TRIMMER = 0 + (95 << 6);
    static final int SIGNAL_SWITCH = 1 + (95 << 6);
    static final int SIGNAL_BUTTON = 8 + (95 << 6);
    static final int ENERGY_METER = 4 + (98 << 6);
    static final int TIMER = 0 + (109 << 6);
    static final int SIGNAL_PROCESSOR = 4 + (109 << 6);
    static final int RESIN_COLLECTOR = 0 + (116 << 6);
    static final int MODBUS_RTU = 0 + (117 << 6);
    static final int TUTORIAL_SIGN = 8 + (117 << 6);
    static final int NOT = 0 + (118 << 6);
    static final int AND = 1 + (118 << 6);
    static final int XOR = 5 + (118 << 6);
    static final int AMPLIFIER = 4 + (124 << 6);
    static final int VCA = 5 + (124 << 6);
    static final int SUMMING = 6 + (124 << 6);
    static final int WIRELESS_ANALYSER = 8 + (14 << 6); // shared item
    static final int DATA_LOGS_PRINT = 32 + (120 << 6); // shared item
    static final double SVU = Eln.SVU;
    static final double TICK = 0.05;

    public static void addTo(List<TestCase> cases) {
        cases.add(new LogicCase());
        cases.add(new AnalogCase());
        cases.add(new MathCase());
        cases.add(new TimerCase());
        cases.add(new GateSourceCase());
        cases.add(new HubCase());
        cases.add(new EnergyMeterCase());
        cases.add(new WirelessCase());
        cases.add(new DataLoggerCase());
        cases.add(new EmergencyLampCase());
        cases.add(new ResinCollectorCase());
        cases.add(new MiscCase());
    }

    // ------------------------------------------------------------------ helpers

    static ElectricalSourceElement signal(TestContext ctx, BlockPos p, double u) {
        ElectricalSourceElement s = (ElectricalSourceElement) ctx.placeSix(SIGNAL_SOURCE, p);
        s.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat((float) u);
        }));
        return s;
    }

    /** Position next to a floor element's pin `lrdu`. */
    static BlockPos beside(SixNodeElement e, BlockPos p, LRDU lrdu) {
        return off(p, e.side.applyLRDU(lrdu));
    }

    static double out(SixNodeElement e) {
        return e.getElectricalLoad(e.front).getU();
    }

    static ServerPlayer player(TestContext ctx) {
        return FakePlayerFactory.getMinecraft(ctx.world());
    }

    static double elapsed(TestContext ctx, long t0) {
        return (ctx.world().getGameTime() - t0) * TICK;
    }

    static void within(TestContext ctx, String what, double measured, double expected, double tol) {
        ctx.check(what, Math.abs(measured - expected) <= tol,
            f("measured %.6g, expected %.6g +- %.3g", measured, expected, tol));
    }

    static void setField(Object o, String name, Object value) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field fl = c.getDeclaredField(name);
                fl.setAccessible(true);
                fl.set(o, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // superclass
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("no field " + name + " in " + o.getClass());
    }

    static Object call(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m.invoke(o);
            } catch (NoSuchMethodException ignored) {
                // superclass
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("no method " + name + " in " + o.getClass());
    }

    // ------------------------------------------------------------------ cases

    /**
     * Three lanes along +X at z=1: Signal Source -> gate (output +X) -> signal cable, side input from a second source.
     * AND(50 V, 50 V) = 1 -> 50 V; XOR(50 V, 50 V) = 0 -> 0 V; NOT(0 V) = 1 -> 50 V. Inputs are digital at
     * normalized <= 0.2 (0) / >= 0.6 (1).
     */
    static final class LogicCase implements TestCase {
        SixNodeElement and, xor, not;

        public String name() {
            return "wp9c logic gates";
        }

        public int width() {
            return 11;
        }

        SixNodeElement gate(TestContext ctx, int damage, int x, double back, double left) {
            signal(ctx, ctx.at(x - 1, 1, 1), back);
            BlockPos p = ctx.at(x, 1, 1);
            SixNodeElement g = ctx.placeSix(damage, p);
            orient(g, l -> l, Direction.XP);
            if (left >= 0) signal(ctx, beside(g, p, g.front.left()), left);
            ctx.placeSix(SIGNAL_CABLE, ctx.at(x + 1, 1, 1));
            return g;
        }

        public void build(TestContext ctx) {
            and = gate(ctx, AND, 1, SVU, SVU);
            xor = gate(ctx, XOR, 5, SVU, SVU);
            not = gate(ctx, NOT, 9, 0, -1);
        }

        public void measure(TestContext ctx) {
            ctx.check("wp9c logic gate elements", and instanceof LogicGateElement && xor instanceof LogicGateElement
                && not instanceof LogicGateElement, and + ", " + xor + ", " + not);
            ctx.checkValue("wp9c AND(1, 1) output [V] (= SVU)", out(and), SVU);
            within(ctx, "wp9c XOR(1, 1) output [V] (= 0)", out(xor), 0, 0.01);
            ctx.checkValue("wp9c NOT(0) output [V] (= SVU)", out(not), SVU);
        }
    }

    /**
     * Amplifier, gain 0.5 by GUI packet, input 30 V -> 15 V. Summing unit (gains 1, 1, 1), inputs 10 V (back) +
     * 20 V (left) + unconnected (right) -> 30 V. VCA: in 20 V, control 2.5 V -> 20 * 2.5 / 5 = 10 V.
     */
    static final class AnalogCase implements TestCase {
        SixNodeElement amp, sum, vca;

        public String name() {
            return "wp9c analog chips";
        }

        public int width() {
            return 11;
        }

        SixNodeElement chip(TestContext ctx, int damage, int x, double back, double left) {
            signal(ctx, ctx.at(x - 1, 1, 1), back);
            BlockPos p = ctx.at(x, 1, 1);
            SixNodeElement c = ctx.placeSix(damage, p);
            orient(c, l -> l, Direction.XP);
            if (left >= 0) signal(ctx, beside(c, p, c.front.left()), left);
            ctx.placeSix(SIGNAL_CABLE, ctx.at(x + 1, 1, 1));
            return c;
        }

        public void build(TestContext ctx) {
            amp = chip(ctx, AMPLIFIER, 1, 30, -1);
            amp.networkUnserialize(ctx.stream(o -> {
                o.writeByte(1); // AmplifierElement.GainChangedEvent
                o.writeFloat(0.5F);
            }));
            sum = chip(ctx, SUMMING, 5, 10, 20);
            vca = chip(ctx, VCA, 9, 20, 2.5);
        }

        public void measure(TestContext ctx) {
            ctx.check("wp9c analog chip elements", amp instanceof AmplifierElement && sum instanceof SummingUnitElement
                && vca instanceof AnalogChipElement, amp + ", " + sum + ", " + vca);
            ctx.checkValue("wp9c amplifier output [V] (0.5 * 30)", out(amp), 15);
            ctx.checkValue("wp9c summing unit output [V] (10 + 20 + 0)", out(sum), 30);
            ctx.checkValue("wp9c VCA output [V] (20 * 2.5 / 5)", out(vca), 10);
        }
    }

    /**
     * Signal Processor, expression "A*0.5" (GUI packet), A (right pin) = 40 V -> normalized 0.8 -> 0.4 -> 20 V. A second
     * without chips in its slot outputs 0 V (Baughn 2026-10-30: ceil(operators / 4) cheap chips). A third runs a program
     * with named components, "half = A*0.5; half + half" -> 40 V, its parts paid for once. A fourth, saved with 8 redstone
     * in its slot (before 2026-10-30), loads with 2 cheap chips. A fifth, "1", still offers all three inputs. A sixth gives
     * A and B defaults: A's cable overrides its default, B (no cable) reads its default.
     */
    static final class MathCase implements TestCase {
        ElectricalMathElement withChips, without, program, migrated, constant, defaults;
        String slotAfterLoad = "", upgraded = "";

        public String name() {
            return "wp9c signal processor";
        }

        public int width() {
            return 23;
        }

        ElectricalMathElement math(TestContext ctx, int x, int chips, String expression) {
            BlockPos p = ctx.at(x, 1, 1);
            ElectricalMathElement m = (ElectricalMathElement) ctx.placeSix(SIGNAL_PROCESSOR, p);
            orient(m, l -> l, Direction.XP);
            m.networkUnserialize(ctx.stream(o -> {
                o.writeByte(1); // ElectricalMathElement.setExpressionId
                o.writeUTF(expression);
            }), player(ctx));
            if (chips > 0) {
                m.getInventory().setItem(0, mods.eln.Eln.findItemStack("Cheap Chip", chips));
                m.getInventory().setChanged();
            }
            signal(ctx, beside(m, p, m.front.right()), 40);
            ctx.placeSix(SIGNAL_CABLE, ctx.at(x + 1, 1, 1));
            return m;
        }

        public void build(TestContext ctx) {
            withChips = math(ctx, 1, 1, "A*0.5");
            without = math(ctx, 5, 0, "A*0.5");
            program = math(ctx, 9, 1, "half = A*0.5; half + half");
            migrated = math(ctx, 13, 0, "A*0.5");
            constant = math(ctx, 17, 0, "1");
            // A has a cable (40 V) and a default: the cable wins; B has none: its default is read (Baughn 2026-10-30)
            defaults = math(ctx, 20, 1, "A = 0.1\nB = 0.3\nA * 0.5 + B");
            // as a processor saved before 2026-10-30 holds redstone: saved and loaded, it holds cheap chips
            migrated.getInventory().setItem(0, new ItemStack(Items.REDSTONE, 8));
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            migrated.writeToNBT(t);
            // and its program from then, rs(reset, set), is rewritten to rs(set, reset), doing the same
            t.putString("expression", "rs(0, A)");
            t.remove("programVersion");
            migrated.readFromNBT(t);
            upgraded = field(migrated, "expression");
            ItemStack s = migrated.getInventory().getItem(0);
            slotAfterLoad = s.getCount() + " " + (ElectricalMathElement.isChip(s) ? "cheap chips" : s.getItem().toString());
        }

        public void measure(TestContext ctx) {
            boolean valid = field(withChips, "equationIsValid");
            int required = field(withChips, "chipsRequired");
            boolean ready = field(withChips, "chipsReady");
            ctx.check("wp9c signal processor: A*0.5 valid, a chip ready", valid && ready && required == 1,
                "valid " + valid + ", requires " + required + " chips, ready " + ready);
            ctx.checkValue("wp9c signal processor output [V] (A = 40 V -> 0.8 * 0.5 * SVU)", out(withChips), 20);
            boolean ready2 = field(without, "chipsReady");
            within(ctx, "wp9c signal processor without chips: not ready, output 0 V", ready2 ? 999 : out(without), 0, 0.01);
            boolean pvalid = field(program, "equationIsValid");
            int preq = field(program, "chipsRequired");
            ctx.check("wp9c signal processor: a program with a named part, one chip (2 operators)", pvalid && preq == 1, "valid " + pvalid + ", " + preq + " chips");
            ctx.checkValue("wp9c signal processor program output [V] (half = 0.4; half + half = 0.8 * SVU)", out(program), 40);
            // the inputs connect though "1" reads none of them (Baughn 2026-10-30)
            var f = constant.front;
            boolean all = constant.getConnectionMask(f.right()) != 0 && constant.getConnectionMask(f.inverse()) != 0 && constant.getConnectionMask(f.left()) != 0;
            ctx.check("wp9c signal processor: A, B and C connect whatever the program reads", all, "");
            ctx.check("wp9c signal processor: an old program's rs(reset, set) loads as rs(set, reset)", upgraded.equals("rs(A, 0)"), upgraded);
            ctx.checkValue("wp9c signal processor: defaults, A wired (0.8), B not (default 0.3): A * 0.5 + B [V]", out(defaults), 35);
            // its logger graphs what the program read: A from the cable, B its default, and the output
            var lv = defaults.logger.view();
            ctx.check("wp9c signal processor: the logger graphs A 0.8, B's default 0.3, out 0.7",
                lv.size() > 0 && Math.abs(lv.mean(0, 0) - 0.8) < 2e-3 && Math.abs(lv.mean(1, 0) - 0.3) < 2e-3 && Math.abs(lv.mean(3, 0) - 0.7) < 2e-3,
                lv.size() > 0 ? lv.mean(0, 0) + " " + lv.mean(1, 0) + " " + lv.mean(3, 0) : "no points");
            ctx.check("wp9c signal processor: 8 redstone load as 2 cheap chips", slotAfterLoad.equals("2 cheap chips"), slotAfterLoad);
            // daytime wraps each day (1.12 divided the total time: after day 1 it only grew)
            ctx.check("wp9c signal processor: daytime is the time of day", Math.abs(mods.eln.misc.Utils.dayFraction(36000) - 12000 / 23999.0) < 1e-9
                && mods.eln.misc.Utils.dayFraction(23999) == 1 && mods.eln.misc.Utils.dayFraction(48000) == 0, "");
        }
    }

    /**
     * Timer A: input held high by a 50 V source, timeout 0.5 s (GUI packet) -> output 50 V, counter held at 0.5 s.
     * Timer B: no input, timeout 0.5 s, started by the GUI "set" packet -> output 50 V while counter = 0.5 - t > 0,
     * then 0 V. B is judged only away from the edge (t outside 0.35..0.65 s).
     */
    static final class TimerCase implements TestCase {
        ElectricalTimeoutElement held, free;
        long t0;

        public String name() {
            return "wp9c timer";
        }

        public int width() {
            return 8;
        }

        ElectricalTimeoutElement timer(TestContext ctx, int x) {
            ElectricalTimeoutElement t = (ElectricalTimeoutElement) ctx.placeSix(TIMER, ctx.at(x, 1, 1));
            orient(t, l -> l, Direction.XN); // input = front, output = back
            t.networkUnserialize(ctx.stream(o -> {
                o.writeByte(ElectricalTimeoutElement.setTimeOutValueId);
                o.writeFloat(0.5F);
            }));
            ctx.placeSix(SIGNAL_CABLE, ctx.at(x + 1, 1, 1));
            return t;
        }

        public void build(TestContext ctx) {
            signal(ctx, ctx.at(0, 1, 1), SVU);
            held = timer(ctx, 1);
            free = timer(ctx, 5);
            free.networkUnserialize(ctx.stream(o -> o.writeByte(ElectricalTimeoutElement.setId)));
            t0 = ctx.world().getGameTime();
        }

        public void measure(TestContext ctx) {
            double t = elapsed(ctx, t0);
            double c1 = field(held, "timeOutCounter");
            ctx.checkValue("wp9c timer with input high: output [V]", held.outputGate.getU(), SVU);
            within(ctx, "wp9c timer with input high: counter held at the timeout [s]", c1, 0.5, 1e-6);
            double c2 = field(free, "timeOutCounter");
            if (t >= 0.65) {
                within(ctx, f("wp9c timer set, no input, after %.2f s > 0.5 s: output [V]", t), free.outputGate.getU(), 0, 0.01);
                within(ctx, "wp9c timer expired: counter [s]", c2, 0, 1e-9);
            } else if (t <= 0.35) {
                ctx.checkValue(f("wp9c timer set, no input, after %.2f s < 0.5 s: output [V]", t), free.outputGate.getU(), SVU);
                within(ctx, "wp9c timer counter [s] (0.5 - t)", c2, 0.5 - t, 1.5 * TICK);
            } else {
                ctx.line(f("wp9c timer: %.2f s is too close to the 0.5 s timeout to judge timer B (counter %.3f)", t, c2));
            }
        }
    }

    /**
     * Signal Trimmer set to 30 V (GUI packet) -> 30 V. Signal Switch, one right click -> 50 V. Signal Button, one right
     * click -> 50 V, back to 0 V after its 0.21 s auto reset (judged when t > 0.35 s, else expected still 50 V).
     */
    static final class GateSourceCase implements TestCase {
        ElectricalGateSourceElement trimmer, sw, button;
        long t0;

        public String name() {
            return "wp9c signal sources";
        }

        public int width() {
            return 8;
        }

        ElectricalGateSourceElement place(TestContext ctx, int damage, int x) {
            ElectricalGateSourceElement e = (ElectricalGateSourceElement) ctx.placeSix(damage, ctx.at(x, 1, 1));
            orient(e, l -> l, Direction.XP);
            ctx.placeSix(SIGNAL_CABLE, ctx.at(x + 1, 1, 1));
            return e;
        }

        public void build(TestContext ctx) {
            trimmer = place(ctx, SIGNAL_TRIMMER, 0);
            trimmer.networkUnserialize(ctx.stream(o -> {
                o.writeByte(ElectricalGateSourceElement.setVoltagerId);
                o.writeFloat(30F);
            }));
            sw = place(ctx, SIGNAL_SWITCH, 3);
            Wp9aCases.use(ctx, sw, ItemStack.EMPTY);
            button = place(ctx, SIGNAL_BUTTON, 6);
            Wp9aCases.use(ctx, button, ItemStack.EMPTY);
            t0 = ctx.world().getGameTime();
        }

        public void measure(TestContext ctx) {
            double t = elapsed(ctx, t0);
            ctx.checkValue("wp9c signal trimmer output [V] (set 30 V)", trimmer.outputGate.getU(), 30);
            ctx.checkValue("wp9c signal switch after one click [V] (= SVU)", sw.outputGate.getU(), SVU);
            if (t > 0.35)
                within(ctx, f("wp9c signal button %.2f s after a click [V] (auto reset after 0.21 s)", t), button.outputGate.getU(), 0, 0.01);
            else
                ctx.checkValue(f("wp9c signal button %.2f s after a click [V] (before the 0.21 s reset)", t), button.outputGate.getU(), SVU);
        }
    }

    /**
     * Source 50 V -> LV cable -> Hub (LV cables in the slots facing -X and +X; default grid connects left-right and
     * down-up) -> 100 ohm Power Resistor -> Ground. Hub internal resistor = Rs(cable a) + Rs(cable b).
     * I = 50 / (link(src, cable) + link(cable, hub) + R_hub + link(hub, R) + 100 + link(R, ground)).
     */
    static final class HubCase implements TestCase {
        SixNodeElement src, cable, gnd;
        HubElement hub;
        ResistorElement r;

        public String name() {
            return "wp9c hub";
        }

        public int width() {
            return 5;
        }

        public void build(TestContext ctx) {
            src = Wp9aCases.source(ctx, SOURCE, 0, 1, 50);
            cable = ctx.placeSix(LV_CABLE, ctx.at(1, 1, 1));
            hub = (HubElement) ctx.placeSix(HUB, ctx.at(2, 1, 1));
            for (Direction d : new Direction[]{Direction.XN, Direction.XP}) {
                LRDU l = hub.side.getLRDUGoingTo(d);
                hub.getInventory().setItem(l.toInt(), Eln.sixNodeItem.getDescriptor(LV_CABLE).newItemStack());
            }
            hub.getInventory().setChanged();
            r = Wp9aCases.resistor(ctx, POWER_RESISTOR, 3, 1, 48);
            gnd = ctx.placeSix(GROUND, ctx.at(4, 1, 1));
        }

        public void measure(TestContext ctx) {
            double rHub = 2 * mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs;
            double rt = link(src, cable) + link(cable, hub) + rHub + link(hub, r) + r.nominalRs + link(r, gnd);
            ctx.checkValue("wp9c hub: current through hub + 100 ohm [A] (50 V / R_total)", Wp9aCases.current(r), 50 / rt);
            LRDU zSide = hub.side.getLRDUGoingTo(Direction.ZP);
            ctx.check("wp9c hub: no cable in the +Z slot -> no connection there", hub.getConnectionMask(zSide) == 0
                && hub.getElectricalLoad(zSide) == null, "mask " + hub.getConnectionMask(zSide));
        }
    }

    /**
     * Source 50 V -> Energy Meter (LV cable in its slot, shunt closed by the GUI toggle packet, a = front toward the
     * source) -> 100 ohm -> Ground. I = 50 / R_total with the shunt at its small Rs; counted energy / counted time =
     * U(a) * I (time counter runs 72x: timeCounter / 72 = simulated seconds), +-1.5 ticks of power for the start.
     */
    static final class EnergyMeterCase implements TestCase {
        SixNodeElement src, gnd;
        EnergyMeterElement meter;
        ResistorElement r;

        public String name() {
            return "wp9c energy meter";
        }

        public int width() {
            return 4;
        }

        public void build(TestContext ctx) {
            src = Wp9aCases.source(ctx, SOURCE, 0, 1, 50);
            meter = (EnergyMeterElement) ctx.placeSix(ENERGY_METER, ctx.at(1, 1, 1));
            orient(meter, l -> l, Direction.XN);
            // no cable in a slot: built-in LV cable (Baughn 2026-10-17)
            meter.networkUnserialize(ctx.stream(o -> o.writeByte(EnergyMeterElement.clientToggleStateId)));
            r = Wp9aCases.resistor(ctx, POWER_RESISTOR, 2, 1, 48);
            gnd = ctx.placeSix(GROUND, ctx.at(3, 1, 1));
        }

        public void measure(TestContext ctx) {
            double rt = link(src, meter) + meter.shunt.getR() + link(meter, r) + r.nominalRs + link(r, gnd);
            double i = Math.abs(meter.aLoad.getCurrent());
            ctx.check("wp9c energy meter shunt closed by the toggle packet", meter.shunt.getState(), "state " + meter.shunt.getState());
            ctx.checkValue("wp9c energy meter current [A] (50 V / R_total)", i, 50 / rt);
            double energy = field(meter, "energyStack");
            double time = (double) field(meter, "timeCounter") / 72.0;
            double p = meter.aLoad.getU() * i;
            double avg = time > 0 ? energy / time : 0;
            within(ctx, f("wp9c energy meter average power [W] over %.2f s (energy %.3f J)", time, energy), avg, p,
                p * 0.01 + (time > 0 ? p * 1.5 * TICK / time : p));
        }
    }

    /**
     * Signal Source 35 V -> Wireless Transmitter (front = input) ... 3 blocks ... Wireless Receiver (front = output)
     * -> signal cable, both on a private channel (GUI packets). Receiver output = transmitter value (normalized input
     * + lightning glitch offset, 0 without lightning) * SVU = 35 V.
     */
    static final class WirelessCase implements TestCase {
        WirelessSignalTxElement tx;
        WirelessSignalRxElement rx;

        public String name() {
            return "wp9c wireless signal";
        }

        public int width() {
            return 6;
        }

        public void build(TestContext ctx) {
            String channel = "eln selftest " + System.nanoTime();
            signal(ctx, ctx.at(0, 1, 1), 35);
            tx = (WirelessSignalTxElement) ctx.placeSix(WIRELESS_TX, ctx.at(1, 1, 1));
            orient(tx, l -> l, Direction.XN);
            rx = (WirelessSignalRxElement) ctx.placeSix(WIRELESS_RX, ctx.at(4, 1, 1));
            orient(rx, l -> l, Direction.XP);
            ctx.placeSix(SIGNAL_CABLE, ctx.at(5, 1, 1));
            tx.networkUnserialize(ctx.stream(o -> {
                o.writeByte(WirelessSignalTxElement.setChannelId);
                o.writeUTF(channel);
            }));
            rx.networkUnserialize(ctx.stream(o -> {
                o.writeByte(WirelessSignalRxElement.setChannelId);
                o.writeUTF(channel);
            }));
        }

        public void measure(TestContext ctx) {
            boolean connection = field(rx, "connection");
            ctx.check("wp9c wireless receiver connected on the private channel", connection && tx.channel.equals(rx.channel),
                "connection " + connection + ", channels " + tx.channel + " / " + rx.channel);
            ctx.checkValue("wp9c wireless transmitter value (35 V / SVU)", tx.getValue(), 35 / SVU);
            ctx.checkValue("wp9c wireless receiver output [V] (= tx value * SVU)", out(rx), tx.getValue() * SVU);
        }
    }

    /**
     * Signal Source 30 V -> Data Logger A (input = back pin): it records a mean every tick, so after the test's 2 s it
     * holds about 40 ticks and its newest 0.5 s point reads 30 V / SVU. Paper in the paper slot + the GUI print
     * packet -> one Data Logger Print in the print slot. Logger B gets 600 ticks of history before the test; at the
     * end its period goes 0.5 s -> 5 s -> 0.5 s by GUI packets and the 0.5 s view must come back unchanged, the 5 s
     * view reading the same history coarser (board #45). Logger C loads a save from before 2026-10-30 (a byte log of 20
     * samples at 1 s, one scale): it shows the same samples and scale.
     */
    static final class DataLoggerCase implements TestCase {
        ElectricalDataLoggerElement logger, history, migrated;
        long t0;
        String migration = "";
        static final int SEEDED = 600;

        public String name() {
            return "wp9c data logger";
        }

        public int width() {
            return 8;
        }

        static float seed(long t) {
            return (float) (0.5 + 0.4 * Math.sin(t / 15.0));
        }

        ElectricalDataLoggerElement logger(TestContext ctx, int x, boolean input) {
            if (input) signal(ctx, ctx.at(x - 1, 1, 1), 30);
            ElectricalDataLoggerElement l = (ElectricalDataLoggerElement) ctx.placeSix(DATA_LOGGER, ctx.at(x, 1, 1));
            orient(l, LRDU::inverse, Direction.XN);
            return l;
        }

        public void build(TestContext ctx) {
            logger = logger(ctx, 1, true);
            logger.getInventory().setItem(ElectricalDataLoggerContainer.paperSlotId, new ItemStack(Items.PAPER, 2));
            logger.getInventory().setChanged();
            logger.networkUnserialize(ctx.stream(o -> o.writeByte(ElectricalDataLoggerElement.printId)), player(ctx));
            history = logger(ctx, 4, true);
            for (int t = 0; t < SEEDED; t++) history.store.tick(new float[]{seed(t)});
            migrated = logger(ctx, 6, false);
            // a save from before 2026-10-30: newest sample first, (byte) (fraction * 255.5 - 128)
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            migrated.writeToNBT(tag);
            tag.remove("store");
            byte[] old = new byte[20];
            for (int i = 0; i < 20; i++) old[i] = (byte) (i * 12 - 128);
            tag.putByteArray("logslog", old);
            tag.putFloat("logssamplingPeriod", 1f);
            tag.putFloat("logsmaxValue", 50f);
            tag.putFloat("logsminValue", 0f);
            tag.putByte("logsunitType", (byte) 0);
            migrated.readFromNBT(tag);
            DataLogs v = migrated.view();
            int ok = 0;
            for (int i = 0; i < Math.min(20, v.size()); i++) if (Math.abs(v.mean(0, i) - DataLogs.oldSample(old[i])) < 2e-3) ok++;
            migration = v.size() + " points, " + ok + " match, period " + v.samplingPeriod + " s, scale " + v.channels[0].min + ".."
                + v.channels[0].max + " unit " + v.channels[0].unit;
            t0 = ctx.world().getGameTime();
        }

        float[][][] period(TestContext ctx, float p) {
            history.networkUnserialize(ctx.stream(o -> {
                o.writeByte(ElectricalDataLoggerElement.setSamplingPeriodeId);
                o.writeFloat(p);
            }), player(ctx));
            return history.store.view(DataLogs.POINTS);
        }

        public void measure(TestContext ctx) {
            double t = elapsed(ctx, t0);
            within(ctx, f("wp9c data logger ticks recorded after %.2f s (t / 0.05)", t), logger.store.ticks(), t / 0.05, 3);
            DataLogs v = logger.view();
            ctx.checkValue("wp9c data logger newest point (30 V / SVU)", v.size() > 0 ? v.mean(0, 0) : -1, 30 / SVU);
            ItemStack paper = logger.getInventory().getItem(ElectricalDataLoggerContainer.paperSlotId);
            ItemStack print = logger.getInventory().getItem(ElectricalDataLoggerContainer.printSlotId);
            GenericItemUsingDamageDescriptor pd = print.isEmpty() ? null : Eln.sharedItem.getDescriptor(print);
            ctx.check("wp9c data logger print: one paper used, a Data Logger Print with the log",
                paper.getCount() == 1 && pd instanceof DataLogsPrintDescriptor && mods.eln.generic.ItemNbt.has(print),
                "paper " + paper.getCount() + ", print " + print + " (" + pd + ")");

            float[][][] fine = period(ctx, 0.5f), coarse = period(ctx, 5f), back = period(ctx, 0.5f);
            boolean same = fine.length == back.length;
            for (int j = 0; same && j < fine.length; j++) same = java.util.Arrays.equals(fine[j][0], back[j][0]);
            ctx.check("wp9c data logger: 0.5 s -> 5 s -> 0.5 s keeps the history", same && fine.length >= SEEDED / 10,
                fine.length + " points before, " + back.length + " after, equal " + same);
            // the oldest 5 s point covers seeded ticks only: its mean is theirs
            long total = history.store.ticks();
            int oldest = (int) (total / 100) - 1;
            long a = total - (oldest + 1) * 100L;
            double sum = 0;
            for (long k = a; k < a + 100; k++) sum += seed(k);
            within(ctx, "wp9c data logger: the 5 s view reads the same history (oldest point's mean)",
                coarse.length > oldest && oldest >= 0 ? coarse[oldest][0][0] : -1, sum / 100, 2e-3);
            ctx.check("wp9c data logger: a save from before 2026-10-30 keeps its samples and scale",
                migration.equals("20 points, 20 match, period 1.0 s, scale 0.0..50.0 unit V"), migration);
        }
    }

    /**
     * 50V Emergency Lamps (battery 6000 J, starts at half = 3000 J; consumption 5 W; charge power 10 W at 50 V
     * through R = 50^2 / 10 = 250 ohm; light 6). Lamp A: private channel, no lamp supply, no cable -> on, light 6,
     * charge = 3000 - 5 t. Lamp B: powered by cable (GUI toggle packet), 50 V source on its left pin -> off, charging,
     * charge = 3000 + U^2 / 250 * t. Tolerance: 1.5 ticks of the power.
     */
    static final class EmergencyLampCase implements TestCase {
        EmergencyLampElement a, b;
        long t0;

        public String name() {
            return "wp9c emergency lamp";
        }

        public int width() {
            return 7;
        }

        public void build(TestContext ctx) {
            a = (EmergencyLampElement) ctx.placeSix(EMERGENCY_LAMP_50V, ctx.at(1, 1, 1));
            String channel = "eln selftest " + System.nanoTime();
            a.networkUnserialize(ctx.stream(o -> {
                o.writeByte(2); // Event.SET_CHANNEL
                o.writeUTF(channel);
            }));
            Wp9aCases.source(ctx, SOURCE, 4, 1, 50);
            b = (EmergencyLampElement) ctx.placeSix(EMERGENCY_LAMP_50V, ctx.at(5, 1, 1));
            orient(b, LRDU::left, Direction.XN);
            b.networkUnserialize(ctx.stream(o -> o.writeByte(1))); // Event.TOGGLE_POWERED_BY_CABLE
            t0 = ctx.world().getGameTime();
        }

        public void measure(TestContext ctx) {
            double t = elapsed(ctx, t0);
            ctx.check("wp9c emergency lamp without supply: on, light 6", a.getOn() && a.sixNode.getLightValue() == 6,
                "on " + a.getOn() + ", light " + a.sixNode.getLightValue());
            within(ctx, f("wp9c emergency lamp discharge [J] after %.2f s (-5 W * t)", t), a.getCharge() - 3000, -5 * t, 5 * 1.5 * TICK);
            double u = b.getLoad().getU();
            double p = u * u / 250;
            ctx.check("wp9c emergency lamp on cable power: off, charging", !b.getOn() && b.getChargingResistor().getState()
                && b.getPoweredByCable(), "on " + b.getOn() + ", charging " + b.getChargingResistor().getState() + ", U " + u);
            ctx.checkValue("wp9c emergency lamp charging power [W] (U^2 / 250 ohm)", b.getChargingResistor().getP(), p);
            within(ctx, f("wp9c emergency lamp charge gained [J] after %.2f s (U^2 / 250 * t)", t), b.getCharge() - 3000, p * t,
                p * 1.5 * TICK + p * t * 0.01);
        }
    }

    /**
     * Tree Resin Collector on the -X side of a 3-log trunk (logs at x2, y1..3) with four leaves on its other sides.
     * Placement rule: only on the side of wood (canBePlacedOnSide: log yes, stone no). Production =
     * min(0.05, 3/5 / 1440 per trunk block * 3 blocks / 1 collector) = 0.00125 items/s; with fewer than 4 leaves * 1e-9.
     */
    static final class ResinCollectorCase extends Wp9bCases.Base {
        TreeResinCollectorElement collector;
        java.util.List<BlockPos> crown;

        public String name() {
            return "wp9c tree resin collector";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext ctx) {
            for (int y = 1; y <= 3; y++) setVanilla(ctx, ctx.at(2, y, 1), Blocks.OAK_LOG.defaultBlockState());
            // a crown on the far side only (Baughn: collectors round a tree with leaves on other sides should all work)
            var leafState = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
            crown = java.util.List.of(ctx.at(2, 4, 1), ctx.at(3, 3, 1), ctx.at(3, 2, 1), ctx.at(2, 3, 2));
            for (BlockPos q : crown) setVanilla(ctx, q, leafState);
            collector = (TreeResinCollectorElement) ctx.placeSix(RESIN_COLLECTOR, ctx.at(1, 1, 1), Direction.XP);
        }

        void check(TestContext ctx) {
            TreeResinCollectorDescriptor d = (TreeResinCollectorDescriptor) Eln.sixNodeItem.getDescriptor(RESIN_COLLECTOR);
            Direction clicked = Direction.fromIntMinecraftSide(net.minecraft.core.Direction.WEST.get3DDataValue()).getInverse();
            BlockPos log = ctx.at(2, 1, 1), stone = ctx.at(0, 0, 1);
            boolean onLog = d.canBePlacedOnSide(player(ctx), new Coordonate(log.getX(), log.getY(), log.getZ(), ctx.world()), clicked);
            boolean onStone = d.canBePlacedOnSide(player(ctx), new Coordonate(stone.getX(), stone.getY(), stone.getZ(), ctx.world()), clicked);
            ctx.check("wp9c resin collector placement: on the side of a log yes, of stone no", onLog && !onStone,
                "log " + onLog + ", stone " + onStone);
            double expected = 3f / 5f / (60f * 24f) * 3;
            ctx.checkValue("wp9c resin collector production [1/s] (3-log trunk, leaves on the other sides)", (double) call(collector, "getProductPerSecond"), expected);
            var waila = collector.getWaila();
            ctx.check("wp9c resin collector probe: pot and rate, no null (Baughn)", waila.containsKey("Resin in pot")
                && waila.get("Rate").endsWith("per hour") && !waila.containsValue(null), String.valueOf(waila));
            ctx.world().removeBlock(crown.get(0), false);
            ctx.check("wp9c resin collector probe off a tree", collector.getWaila().get("Rate").startsWith("none"), String.valueOf(collector.getWaila()));
            ctx.checkValue("wp9c resin collector production with too few leaves [1/s] (* 1e-9: not a tree)", (double) call(collector, "getProductPerSecond"), expected * 1e-9);
        }
    }

    /** Placement / registration only: tutorial sign, Modbus RTU (always disabled in 1.12), wireless analyser, print. */
    static final class MiscCase implements TestCase {
        SixNodeElement sign, rtu;

        public String name() {
            return "wp9c misc";
        }

        public int width() {
            return 4;
        }

        public void build(TestContext ctx) {
            sign = ctx.placeSix(TUTORIAL_SIGN, ctx.at(0, 1, 1));
            rtu = ctx.placeSix(MODBUS_RTU, ctx.at(3, 1, 1));
        }

        public void measure(TestContext ctx) {
            ctx.check("wp9c tutorial sign placed", sign instanceof TutorialSignElement, String.valueOf(sign));
            ctx.check("wp9c modbus RTU placed (disabled: no TCP server)", rtu instanceof ModbusRtuElement
                && rtu.getWaila().containsKey("X_X"), String.valueOf(rtu));
            ctx.check("wp9c wireless analyser + data logger print registered",
                Eln.sharedItem.getDescriptor(WIRELESS_ANALYSER) instanceof WirelessSignalAnalyserItemDescriptor
                    && Eln.sharedItem.getDescriptor(DATA_LOGS_PRINT) instanceof DataLogsPrintDescriptor,
                Eln.sharedItem.getDescriptor(WIRELESS_ANALYSER) + ", " + Eln.sharedItem.getDescriptor(DATA_LOGS_PRINT));
        }
    }
}
