package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.item.ElectricalFuseDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.signalinductor.SignalInductorElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sixnode.ElectricalFuseHolderElement;
import mods.eln.sixnode.batterycharger.BatteryChargerElement;
import mods.eln.sixnode.diode.DiodeElement;
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerElement;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalrelay.ElectricalRelayElement;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.electricalswitch.ElectricalSwitchElement;
import mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor;
import mods.eln.sixnode.powersocket.PowerSocketElement;
import mods.eln.sixnode.resistor.ResistorDescriptor;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.oredict.OreDictionary;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Selftest cases of device batch Wp9a (registry/batch/Wp9aContent): six-node power components.
 * <p>
 * Every case is a series circuit on the floor along +X: Electrical Source (50 V) -> device under test -> [load] ->
 * Ground Cable. Two-terminal devices are turned (element.front, as a wrench does, then reconnect) so their terminals
 * face -X / +X; control gates face -Z where a Signal Source sits. Expected current: I = U / R_total with
 * R_total = sum over each adjacent pair of (Rs(a) + Rs(b)) (EA's connection resistance, read from the loads at
 * measure time) + the devices' internal resistances. The "load" is a Power Resistor with 48 items in its core slot
 * (E12 series from 0.01 ohm: 0.01 * 10^(48/12) = 100 ohm). Coal dust (wp12) is used when registered, else coal (the
 * resistor only reads the stack size). "Open" means |I| < 1 mA (the 1e9 ohm pull-downs leak 50 nA).
 */
public final class Wp9aCases {
    private Wp9aCases() {
    }

    static final int GROUND = 0 + (2 << 6);
    static final int SOURCE = 0 + (3 << 6);
    static final int SIGNAL_SOURCE = 1 + (3 << 6);
    static final int LV_CABLE = 4 + (32 << 6);
    static final int CHARGER_WEAK = 0 + (66 << 6);   // Weak 50V Battery Charger (200 W)
    static final int SOCKET_50V = 1 + (67 << 6);
    static final int RELAY_LV = 0 + (94 << 6);
    static final int DIODE_10A = 0 + (96 << 6);
    static final int SIGNAL_INDUCTOR = 16 + (96 << 6); // 20 H, hidden sub-item
    static final int POWER_CAPACITOR = 32 + (96 << 6);
    static final int POWER_RESISTOR = 36 + (96 << 6);
    static final int RHEOSTAT = 37 + (96 << 6);
    static final int SWITCH_LV = 1 + (97 << 6);
    static final int BREAKER = 0 + (98 << 6);
    static final int FUSE_HOLDER = 6 + (98 << 6);
    static final int FUSE_LV = 7 + (98 << 6);         // shared item
    static final double U = 50.0;
    static final double OPEN_I = 1e-3;

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new ResistorCase());
        cases.add(new RheostatCase());
        cases.add(new DiodeCase());
        cases.add(new SwitchCase());
        cases.add(new RelayCase());
        cases.add(new BreakerCase());
        cases.add(new FuseCase());
        cases.add(new ReactiveCase());
        cases.add(new SocketChargerCase());
    }

    // ------------------------------------------------------------------ helpers

    /** Turn e so that terminal(front) points to world direction `toward`, then reconnect (what the wrench does). */
    static void orient(SixNodeElement e, UnaryOperator<LRDU> terminal, Direction toward) {
        for (LRDU f : LRDU.values()) {
            if (e.side.applyLRDU(terminal.apply(f)) == toward) {
                e.front = f;
                e.sixNode.reconnect();
                e.sixNode.setNeedPublish(true);
                return;
            }
        }
        throw new IllegalStateException("cannot orient " + e + " toward " + toward);
    }

    /** The element's load on its world side `dir`. */
    static ElectricalLoad load(SixNodeElement e, Direction dir) {
        ElectricalLoad l = e.getElectricalLoad(e.side.getLRDUGoingTo(dir));
        if (l == null) throw new IllegalStateException("no load toward " + dir + " on " + e);
        return l;
    }

    /** Connection resistance between a's +X load and b's -X load. */
    static double link(SixNodeElement a, SixNodeElement b) {
        return load(a, Direction.XP).getRs() + load(b, Direction.XN).getRs();
    }

    static ElectricalSourceElement source(SelfTestContext ctx, int damage, int x, int z, double u) {
        ElectricalSourceElement s = (ElectricalSourceElement) ctx.placeSix(damage, ctx.at(x, 1, z));
        s.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat((float) u);
        }));
        return s;
    }

    static ItemStack coal(int n) {
        List<ItemStack> dust = OreDictionary.getOres("dustCoal");
        ItemStack s = dust.isEmpty() ? new ItemStack(Items.COAL) : dust.get(0).copy();
        s.setCount(n);
        return s;
    }

    /** Power Resistor (or rheostat) with n items in the core slot, terminals along X. */
    static ResistorElement resistor(SelfTestContext ctx, int damage, int x, int z, int n) {
        ResistorElement r = (ResistorElement) ctx.placeSix(damage, ctx.at(x, 1, z));
        orient(r, LRDU::right, Direction.XN);
        r.getInventory().setInventorySlotContents(0, coal(n)); // ResistorContainer.coreId = 0
        r.getInventory().markDirty();
        return r;
    }

    /** Current through a resistor element from its terminal voltages. */
    static double current(ResistorElement r) {
        return (load(r, Direction.XN).getU() - load(r, Direction.XP).getU()) / r.nominalRs;
    }

    static ItemStack lvCable() {
        return Eln.sixNodeItem.getDescriptor(LV_CABLE).newItemStack();
    }

    static EntityPlayer player(SelfTestContext ctx) {
        return FakePlayerFactory.getMinecraft(ctx.world());
    }

    static void use(SelfTestContext ctx, SixNodeElement e, ItemStack held) {
        EntityPlayer p = player(ctx);
        p.setHeldItem(EnumHand.MAIN_HAND, held);
        try {
            e.onBlockActivated(p, e.side, 0.5F, 0.5F, 0.5F);
        } finally {
            p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    static String f(String fmt, Object... args) {
        return String.format(fmt, args);
    }

    // ------------------------------------------------------------------ cases

    /** Source -> Power Resistor 100 ohm -> ground: Ohm's law, power, and a temperature rise bounded by P*t/C. */
    static final class ResistorCase implements SelfTestCase {
        ElectricalSourceElement src;
        ResistorElement r;
        SixNodeElement gnd;
        long t0;

        public String name() {
            return "wp9a power resistor";
        }

        public int width() {
            return 3;
        }

        public void build(SelfTestContext ctx) {
            src = source(ctx, SOURCE, 0, 1, U);
            r = resistor(ctx, POWER_RESISTOR, 1, 1, 48);
            gnd = ctx.placeSix(GROUND, ctx.at(2, 1, 1));
            t0 = ctx.world().getTotalWorldTime();
        }

        public void measure(SelfTestContext ctx) {
            ResistorDescriptor d = (ResistorDescriptor) Eln.sixNodeItem.getDescriptor(POWER_RESISTOR);
            ctx.checkValue("wp9a resistor R from 48 core items [ohm] (E12: 0.01*10^4)", r.nominalRs, 100.0);
            double rt = link(src, r) + r.nominalRs + link(r, gnd);
            double i = U / rt;
            ctx.line(f("wp9a resistor model: R_total = %.6f ohm, I = 50 V / R_total = %.5f A", rt, i));
            double iMeas = current(r);
            ctx.checkValue("wp9a resistor current [A]", iMeas, i);
            ctx.checkValue("wp9a resistor voltage drop [V]", load(r, Direction.XN).getU() - load(r, Direction.XP).getU(), i * r.nominalRs);
            double p = iMeas * iMeas * r.nominalRs;
            ctx.checkValue("wp9a resistor power [W] (I^2 R)", p, i * i * r.nominalRs);
            double t = (ctx.world().getTotalWorldTime() - t0) * 0.05;
            double c = d.thermalMaximalPowerDissipated * d.thermalNominalHeatTime / d.thermalWarmLimit;
            double tc = r.getThermalLoad(LRDU.Up).Tc;
            double bound = p * t / c;
            ctx.check("wp9a resistor heating", tc > 0 && tc <= bound * 1.01 + 1e-6,
                f("T = %.4f C above ambient, expected 0 < T <= P*t/C = %.2f W * %.2f s / %.1f J/K = %.4f C", tc, p, t, c, bound));
        }
    }

    /** Rheostat 100 ohm nominal, control gate at 25 V (Signal Source): R = Rnom * (0.5 + 0.01) / 1.01. */
    static final class RheostatCase implements SelfTestCase {
        ElectricalSourceElement src;
        ResistorElement r;
        SixNodeElement gnd;

        public String name() {
            return "wp9a rheostat";
        }

        public int width() {
            return 3;
        }

        public void build(SelfTestContext ctx) {
            src = source(ctx, SOURCE, 0, 1, U);
            source(ctx, SIGNAL_SOURCE, 1, 0, 25.0);
            r = resistor(ctx, RHEOSTAT, 1, 1, 48);
            orient(r, f -> f, Direction.ZN); // control gate (front) toward the signal source; terminals stay on X
            gnd = ctx.placeSix(GROUND, ctx.at(2, 1, 1));
        }

        public void measure(SelfTestContext ctx) {
            double rEff = r.nominalRs * (0.5 + 0.01) / 1.01;
            double rt = link(src, r) + rEff + link(r, gnd);
            double i = U / rt;
            double drop = load(r, Direction.XN).getU() - load(r, Direction.XP).getU();
            ctx.line(f("wp9a rheostat model: control 25 V -> 0.5, R = 100 * 0.51 / 1.01 = %.4f ohm, I = %.5f A", rEff, i));
            ctx.checkValue("wp9a rheostat control [normalized]", r.control.getNormalized(), 0.5);
            ctx.checkValue("wp9a rheostat voltage drop [V]", drop, i * rEff);
            ctx.checkValue("wp9a rheostat resistance U/I [ohm]", drop / (U - drop) * (rt - rEff), rEff);
        }
    }

    /** 10 A diode, forward (z=0 lane) and reverse (z=2 lane), each in series with 100 ohm. */
    static final class DiodeCase implements SelfTestCase {
        ElectricalSourceElement srcF, srcR;
        DiodeElement dF, dR;
        ResistorElement rF, rR;
        SixNodeElement gF, gR;

        public String name() {
            return "wp9a diode";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            srcF = source(ctx, SOURCE, 0, 0, U);
            dF = (DiodeElement) ctx.placeSix(DIODE_10A, ctx.at(1, 1, 0));
            orient(dF, f -> f, Direction.XN); // anode (front) toward the source
            rF = resistor(ctx, POWER_RESISTOR, 2, 0, 48);
            gF = ctx.placeSix(GROUND, ctx.at(3, 1, 0));

            srcR = source(ctx, SOURCE, 0, 2, U);
            dR = (DiodeElement) ctx.placeSix(DIODE_10A, ctx.at(1, 1, 2));
            orient(dR, f -> f, Direction.XP); // anode toward the load: reverse biased
            rR = resistor(ctx, POWER_RESISTOR, 2, 2, 48);
            gR = ctx.placeSix(GROUND, ctx.at(3, 1, 2));
        }

        public void measure(SelfTestContext ctx) {
            double rOn = dF.resistorSwitch.getR();
            ctx.checkValue("wp9a diode on-resistance [ohm] (stdU/stdI = 1 V / 10 A)", rOn, 0.1);
            double rt = link(srcF, dF) + rOn + link(dF, rF) + rF.nominalRs + link(rF, gF);
            double i = U / rt;
            ctx.line(f("wp9a diode forward: anode %.3f V, cathode %.3f V; model I = 50 / %.5f ohm = %.5f A",
                dF.anodeLoad.getU(), dF.catodeLoad.getU(), rt, i));
            ctx.checkValue("wp9a diode forward current [A]", current(rF), i);
            ctx.checkValue("wp9a diode forward drop [V] (I * 0.1)", dF.anodeLoad.getU() - dF.catodeLoad.getU(), i * rOn);
            double iRev = current(rR);
            ctx.line(f("wp9a diode reverse: anode %.3f V, cathode %.3f V", dR.anodeLoad.getU(), dR.catodeLoad.getU()));
            ctx.check("wp9a diode reverse blocks", Math.abs(iRev) < OPEN_I && dR.catodeLoad.getU() - dR.anodeLoad.getU() > 0.98 * U,
                f("I = %.3g A (expected < 1 mA), U(cathode) - U(anode) = %.3f V (expected ~50 V)",
                    iRev, dR.catodeLoad.getU() - dR.anodeLoad.getU()));
        }
    }

    /** Low Voltage Switch: z=0 lane switched on by a right click (empty hand), z=2 lane left open. */
    static final class SwitchCase implements SelfTestCase {
        ElectricalSourceElement srcA;
        ElectricalSwitchElement on, off;
        ResistorElement rA, rB;
        SixNodeElement gA;

        public String name() {
            return "wp9a switch";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            srcA = source(ctx, SOURCE, 0, 0, U);
            on = (ElectricalSwitchElement) ctx.placeSix(SWITCH_LV, ctx.at(1, 1, 0));
            orient(on, f -> f, Direction.XN);
            rA = resistor(ctx, POWER_RESISTOR, 2, 0, 48);
            gA = ctx.placeSix(GROUND, ctx.at(3, 1, 0));
            use(ctx, on, ItemStack.EMPTY); // 1.12: empty hand is ItemStack.EMPTY

            source(ctx, SOURCE, 0, 2, U);
            off = (ElectricalSwitchElement) ctx.placeSix(SWITCH_LV, ctx.at(1, 1, 2));
            orient(off, f -> f, Direction.XN);
            rB = resistor(ctx, POWER_RESISTOR, 2, 2, 48);
            ctx.placeSix(GROUND, ctx.at(3, 1, 2));
        }

        public void measure(SelfTestContext ctx) {
            double rSw = on.switchResistor.getR();
            // registration passes rs = 2 * LV cable Rs; the descriptor halves it (electricalRs = rs / 2, as in 1.7.10)
            ctx.checkValue("wp9a switch closed resistance [ohm] (registered 2 * LV cable Rs, halved = LV cable Rs)", rSw, ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs);
            double rt = link(srcA, on) + rSw + link(on, rA) + rA.nominalRs + link(rA, gA);
            ctx.checkValue("wp9a switch closed current [A] (50 V / R_total)", current(rA), U / rt);
            double iOff = current(rB);
            ctx.check("wp9a switch open", Math.abs(iOff) < OPEN_I, f("I = %.3g A, expected < 1 mA", iOff));
        }
    }

    /** Low Voltage Relay, gate (front, -Z) driven to 50 V by a Signal Source: closes (hysteresis > 0.7 * SVU). */
    static final class RelayCase implements SelfTestCase {
        ElectricalSourceElement src;
        ElectricalRelayElement relay;
        ResistorElement r;
        SixNodeElement gnd;

        public String name() {
            return "wp9a relay";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            src = source(ctx, SOURCE, 0, 1, U);
            source(ctx, SIGNAL_SOURCE, 1, 0, Eln.SVU);
            relay = (ElectricalRelayElement) ctx.placeSix(RELAY_LV, ctx.at(1, 1, 1));
            orient(relay, f -> f, Direction.ZN);
            r = resistor(ctx, POWER_RESISTOR, 2, 1, 48);
            gnd = ctx.placeSix(GROUND, ctx.at(3, 1, 1));
        }

        public void measure(SelfTestContext ctx) {
            ctx.check("wp9a relay closed by gate", relay.getSwitchState(), f("gate %.2f V (closes above %.1f V)", relay.gate.getU(), 0.7 * Eln.SVU));
            double rSw = relay.switchResistor.getR();
            ctx.checkValue("wp9a relay contact resistance [ohm] (LV cable Rs)", rSw, ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs);
            double rt = link(src, relay) + rSw + link(relay, r) + r.nominalRs + link(r, gnd);
            ctx.checkValue("wp9a relay current [A] (50 V / R_total)", current(r), U / rt);
        }
    }

    /** Breaker with an LV cable: z=0 vmax 100 V + switched on (GUI packets) conducts; z=2 vmax 40 V trips at 50 V. */
    static final class BreakerCase implements SelfTestCase {
        ElectricalSourceElement srcA;
        ElectricalBreakerElement bA, bB;
        ResistorElement rA, rB;
        SixNodeElement gA;

        public String name() {
            return "wp9a breaker";
        }

        public int width() {
            return 4;
        }

        ElectricalBreakerElement breaker(SelfTestContext ctx, int z, float vMax) {
            ElectricalBreakerElement b = (ElectricalBreakerElement) ctx.placeSix(BREAKER, ctx.at(1, 1, z));
            orient(b, f -> f, Direction.XN);
            b.getInventory().setInventorySlotContents(0, lvCable()); // ElectricalBreakerContainer.cableSlotId
            b.getInventory().markDirty();
            b.networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalBreakerElement.setVoltageMaxId);
                out.writeFloat(vMax);
            }));
            b.networkUnserialize(ctx.stream(out -> out.writeByte(ElectricalBreakerElement.toogleSwitchId)));
            return b;
        }

        public void build(SelfTestContext ctx) {
            srcA = source(ctx, SOURCE, 0, 0, U);
            bA = breaker(ctx, 0, 100F);
            rA = resistor(ctx, POWER_RESISTOR, 2, 0, 48);
            gA = ctx.placeSix(GROUND, ctx.at(3, 1, 0));

            source(ctx, SOURCE, 0, 2, U);
            bB = breaker(ctx, 2, 40F);
            rB = resistor(ctx, POWER_RESISTOR, 2, 2, 48);
            ctx.placeSix(GROUND, ctx.at(3, 1, 2));
        }

        public void measure(SelfTestContext ctx) {
            ctx.check("wp9a breaker (vmax 100 V) closed", bA.getSwitchState(), "");
            double rSw = bA.switchResistor.getR();
            ctx.checkValue("wp9a breaker contact resistance [ohm] (LV cable Rs)", rSw, ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs);
            double rt = link(srcA, bA) + rSw + link(bA, rA) + rA.nominalRs + link(rA, gA);
            ctx.checkValue("wp9a breaker current [A] (50 V / R_total)", current(rA), U / rt);
            double iB = current(rB);
            ctx.check("wp9a breaker (vmax 40 V) tripped at 50 V", !bB.getSwitchState() && Math.abs(iB) < OPEN_I,
                f("switch %s, I = %.3g A (expected open, < 1 mA)", bB.getSwitchState() ? "closed" : "open", iB));
        }
    }

    /** Fuse holder: z=0 an LV lead fuse put in by right click (stack consumed) conducts; z=2 fuse put in and taken
     *  out again with an empty hand (the 1.12 empty-hand path) is open. */
    static final class FuseCase implements SelfTestCase {
        ElectricalSourceElement srcA;
        ElectricalFuseHolderElement hA, hB;
        ResistorElement rA, rB;
        SixNodeElement gA;
        int leftInHand = -1;

        public String name() {
            return "wp9a fuse holder";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            srcA = source(ctx, SOURCE, 0, 0, U);
            hA = (ElectricalFuseHolderElement) ctx.placeSix(FUSE_HOLDER, ctx.at(1, 1, 0));
            orient(hA, f -> f, Direction.XN);
            rA = resistor(ctx, POWER_RESISTOR, 2, 0, 48);
            gA = ctx.placeSix(GROUND, ctx.at(3, 1, 0));
            ItemStack fuse = Eln.sharedItem.getDescriptor(FUSE_LV).newItemStack();
            use(ctx, hA, fuse);
            leftInHand = fuse.getCount();

            source(ctx, SOURCE, 0, 2, U);
            hB = (ElectricalFuseHolderElement) ctx.placeSix(FUSE_HOLDER, ctx.at(1, 1, 2));
            orient(hB, f -> f, Direction.XN);
            rB = resistor(ctx, POWER_RESISTOR, 2, 2, 48);
            ctx.placeSix(GROUND, ctx.at(3, 1, 2));
            use(ctx, hB, Eln.sharedItem.getDescriptor(FUSE_LV).newItemStack());
            use(ctx, hB, ItemStack.EMPTY);
        }

        public void measure(SelfTestContext ctx) {
            ElectricalFuseDescriptor fd = hA.getInstalledFuse();
            ElectricalCableDescriptor cable = fd == null ? null : fd.getCableDescriptor();
            ctx.check("wp9a fuse inserted by right click", cable == ElnDeviceRegistry.lowVoltageCableDescriptor && leftInHand == 0,
                "installed " + (fd == null ? "none" : fd.name) + ", fuses left in hand " + leftInHand + " (expected 0)");
            if (cable != null) {
                double rt = link(srcA, hA) + cable.electricalRs + link(hA, rA) + rA.nominalRs + link(rA, gA);
                ctx.checkValue("wp9a fuse current [A] (50 V / R_total, fuse R = LV cable Rs)", current(rA), U / rt);
            }
            double iB = current(rB);
            ctx.check("wp9a fuse taken out with empty hand", hB.getInstalledFuse() == null && Math.abs(iB) < OPEN_I,
                "installed " + (hB.getInstalledFuse() == null ? "none" : hB.getInstalledFuse().name) + f(", I = %.3g A (expected open)", iB));
        }
    }

    /** z=0: Power Capacitor (empty slots: C = 1 uF per getCValue) charged from 50 V through the connections:
     *  steady state U = 50 V. z=2: Signal 20 H inductor in series with 100 ohm: steady state I = U / R_total (tau = L/R
     *  = 0.2 s; needs a run of >= ~1 s, the default 40 ticks is 2 s). */
    static final class ReactiveCase implements SelfTestCase {
        ElectricalSourceElement srcI;
        SixNodeElement cap, gC, gI;
        SignalInductorElement ind;
        ResistorElement rI;

        public String name() {
            return "wp9a capacitor + signal inductor";
        }

        public int width() {
            return 4;
        }

        public void build(SelfTestContext ctx) {
            source(ctx, SOURCE, 0, 0, U);
            cap = ctx.placeSix(POWER_CAPACITOR, ctx.at(1, 1, 0));
            orient(cap, LRDU::right, Direction.XN); // positive load = front.right()
            gC = ctx.placeSix(GROUND, ctx.at(2, 1, 0));

            srcI = source(ctx, SOURCE, 0, 2, U);
            ind = (SignalInductorElement) ctx.placeSix(SIGNAL_INDUCTOR, ctx.at(1, 1, 2));
            orient(ind, f -> f, Direction.XN);
            rI = resistor(ctx, POWER_RESISTOR, 2, 2, 48);
            gI = ctx.placeSix(GROUND, ctx.at(3, 1, 2));
        }

        public void measure(SelfTestContext ctx) {
            PowerCapacitorSixDescriptor cd = (PowerCapacitorSixDescriptor) Eln.sixNodeItem.getDescriptor(POWER_CAPACITOR);
            ctx.line(f("wp9a capacitor: C = %.3g F (no redstone/dielectric: getCValue(0, 0))", cd.getCValue(cap.getInventory())));
            ctx.checkValue("wp9a capacitor charged voltage [V]", load(cap, Direction.XN).getU() - load(cap, Direction.XP).getU(), U);
            ctx.checkValue("wp9a signal inductor L [H]", ind.inductor.getL(), 20.0);
            double rt = link(srcI, ind) + link(ind, rI) + rI.nominalRs + link(rI, gI);
            double i = U / rt;
            ctx.checkValue("wp9a signal inductor steady current [A] (50 V / R_total)", current(rI), i);
            ctx.checkValue("wp9a signal inductor current (component) [A]", Math.abs(ind.inductor.getCurrent()), i);
        }
    }

    /** Wall lane (blocks z=3, y=0, on the side of the platform): Electrical Source + 50V Power Socket with an LV
     *  cable in its slot, nothing plugged in: the socket node sits at the source voltage and it is registered on the
     *  default channel. z=2: Weak 50V Battery Charger switched on through its
     *  GUI packet, no batteries: draws at most its nominal 200 W and fills
     *  its energy buffer (up to 2 s of nominal power). */
    static final class SocketChargerCase implements SelfTestCase {
        ElectricalSourceElement srcC;
        PowerSocketElement socket;
        BatteryChargerElement charger;
        final List<BlockPos> wall = new ArrayList<>();

        /** The socket only goes on walls (placeDirection XP/XN/ZP/ZN, checked by placeBlockAt): place like a player
         *  clicking the +Z face (MC side 3) of the platform block at p - (0,0,1); element on the ZN face of p.
         *  ctx.placeSix only does floors, so these blocks are removed by this case (removeWall), not by SelfTest. */
        SixNodeElement placeOnWall(SelfTestContext ctx, int damage, BlockPos p) {
            SixNodeDescriptor d = Eln.sixNodeItem.getDescriptor(damage);
            boolean ok = Eln.sixNodeItem.placeBlockAt(d.newItemStack(), player(ctx), ctx.world(), p.getX(), p.getY(), p.getZ(), 3, 0.5F, 0.5F, 0F, damage);
            if (!ok) throw new IllegalStateException("placeBlockAt (wall) failed for " + d.name + " at " + p);
            wall.add(p);
            NodeBase node = NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.getX(), p.getY(), p.getZ(), ctx.world()));
            if (!(node instanceof SixNode)) throw new IllegalStateException("no SixNode at " + p);
            SixNodeElement e = ((SixNode) node).getElement(Direction.ZN);
            if (e == null) throw new IllegalStateException("no element on ZN at " + p);
            return e;
        }

        void removeWall(SelfTestContext ctx) {
            boolean clean = true;
            for (BlockPos p : wall) {
                ctx.world().setBlockToAir(p); // drops (socket + its cable) are item entities, killed by the cleanup
                if (NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.getX(), p.getY(), p.getZ(), ctx.world())) != null)
                    clean = false;
            }
            ctx.check("wp9a power socket wall lane removed", clean && !wall.isEmpty(), wall.size() + " block(s)");
            wall.clear();
        }

        public String name() {
            return "wp9a power socket + battery charger";
        }

        public int width() {
            return 2;
        }

        public void build(SelfTestContext ctx) {
            // wall lane: both on the +Z side face of the platform's last row (z=2), in blocks (0..1, 0, 3)
            ElectricalSourceElement srcS = (ElectricalSourceElement) placeOnWall(ctx, SOURCE, ctx.at(0, 0, 3));
            srcS.networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat((float) U);
            }));
            socket = (PowerSocketElement) placeOnWall(ctx, SOCKET_50V, ctx.at(1, 0, 3));
            orient(socket, f -> f, Direction.XN);
            socket.getInventory().setInventorySlotContents(0, lvCable()); // PowerSocketContainer.cableSlotId
            socket.getInventory().markDirty();

            srcC = source(ctx, SOURCE, 0, 2, U);
            charger = (BatteryChargerElement) ctx.placeSix(CHARGER_WEAK, ctx.at(1, 1, 2));
            orient(charger, f -> f, Direction.XN);
            charger.networkUnserialize(ctx.stream(out -> out.writeByte(BatteryChargerElement.toogleCharge)));
        }

        public void measure(SelfTestContext ctx) {
            try {
                ctx.checkValue("wp9a power socket voltage [V] (no plug: no current)", socket.powerLoad.getU(), U);
                List<PowerSocketElement> ch = PowerSocketElement.channelMap.get(socket.channel);
                ctx.check("wp9a power socket on channel", ch != null && ch.contains(socket), "channel '" + socket.channel + "'");
            } finally {
                removeWall(ctx);
            }
            // current from the drop over the source/charger connection (Rs(source) + Rs(charger))
            double rLink = link(srcC, charger);
            double uC = charger.powerLoad.getU();
            double i = (U - uC) / rLink;
            double p = uC * i;
            double pNom = charger.descriptor.nominalPower;
            double e = charger.getEnergyBuffer();
            // slow process (every 1 s): while the buffer is below 2 s * nominal power it draws up to the nominal power
            ctx.check("wp9a battery charger on, drawing at most nominal power", charger.isPowerOn() && p >= -1e-6 && p <= pNom * 1.01,
                f("on %s, P = %.2f W at %.3f V (expected 0 <= P <= nominal %.0f W)", charger.isPowerOn(), p, uC, pNom));
            ctx.check("wp9a battery charger took energy", e > 0.01 * pNom && e <= 2 * pNom * 1.01,
                f("buffer %.1f J (expected 0 < E <= 2 s * %.0f W, no batteries to give it to)", e, pNom));
        }
    }
}
