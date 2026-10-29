package mods.eln.gametest.cases;

import mods.eln.anvil.ElectricalAnvilElement;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.transparentnode.capacitorbank.CapacitorBankDescriptor;
import mods.eln.transparentnode.capacitorbank.CapacitorBankElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The capacitor bank (Baughn 2026-10-28): it charges to its line and holds ~0.5 C U^2; it pops over 1.3 x its rating;
 * charged, it carries one of the electrical anvil's 20 kJ bursts from 800 V without a plant. Its charge is saved with
 * its loads (the harness's save/load round trip covers every node a test builds).
 */
public final class CapacitorBankCases {
    private CapacitorBankCases() {
    }

    static final int BANK = 4 + (85 << 6);
    static final int HV_CABLE = 12 + (32 << 6);

    public static void addTo(List<TestCase> cases) {
        cases.add(new Charge());
        cases.add(new Hold());
        cases.add(new OverVoltage());
        cases.add(new Hammered());
        cases.add(new Burst());
        cases.add(new PowerCapacitorLeak());
    }

    /**
     * The six-node Power Capacitor leaks as the bank (Baughn 2026-10-28; 1.12: RC 33 h): 13 redstone, 16 Dielectrics
     * (39 mF), charged to 800 V and left 20 s: U = 800 e^(-20/1800) = 791.2 V, so over an hour it falls to e^-2 (13.5%)
     * of its voltage, 1.8% of its energy.
     */
    static final class PowerCapacitorLeak implements TestCase {
        mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixElement cap;

        public String id() {
            return "power_capacitor_leaks_like_the_bank";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 400;   // 20 s
        }

        public void build(TestContext ctx) {
            cap = (mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixElement) ctx.placeSix(Wp9aCases.POWER_CAPACITOR, ctx.at(1, 1, 1));
            Batch1Cases.orient(cap, mods.eln.misc.LRDU::left, Direction.XN);   // (its - terminal, the front's left, west)
            ctx.placeSix(Wp10aCases.GROUND, ctx.at(0, 1, 1));
            cap.getInventory().setItem(0, new ItemStack(Items.REDSTONE, 13));
            cap.getInventory().setItem(1, mods.eln.Eln.findItemStack("Dielectric", 16));
            cap.getInventory().setChanged();
            cap.positiveLoad.setU(800);
            cap.negativeLoad.setU(0);
        }

        public void measure(TestContext ctx) {
            double c = cap.capacitor.getC(), u = cap.capacitor.getU(), want = 800 * Math.exp(-20.0 / 1800);
            ctx.line(String.format("power capacitor: C %.4f F, R %.0f ohm, %.2f V after 20 s (want %.2f)", c, cap.dischargeResistor.getR(), u, want));
            ctx.check("power capacitor: 13 redstone, 16 Dielectrics is 39 mF", Math.abs(c - 10.0 / 256) < 1e-6, String.format("%.5f F", c));
            ctx.check("power capacitor: its leak is RC 30 min (R = 1800 s / C)", Math.abs(cap.dischargeResistor.getR() * c - 1800) < 1,
                String.format("RC %.1f s", cap.dischargeResistor.getR() * c));
            ctx.check("power capacitor: 800 V falls to 800 e^(-20/1800) = 791.2 V in 20 s (e^-2 in an hour)", Math.abs(u - want) < 1.0,
                String.format("%.2f V", u));
        }
    }

    /** A 9x7x7 platform (an existing structure), floor everywhere: room round a device in the middle. */
    abstract static class Room implements TestCase {
        public int width() {
            return 9;
        }

        public int height() {
            return 7;
        }

        public int depth() {
            return 7;
        }

        public int floorDepth() {
            return 7;
        }
    }

    static SixNodeElement source(TestContext ctx, BlockPos p, double volts) {   // (an Electrical Source at volts)
        SixNodeElement src = ctx.placeSix(Wp10aCases.SOURCE, p);
        src.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat((float) volts);
        }));
        return src;
    }

    /**
     * Inrush (Baughn 2026-10-28: "inrush current is supposed to be a problem"): an empty bank straight on a stiff 800 V
     * source draws hundreds of amps and burns its HV cable; the same bank behind a 1 kohm Power Resistor (60 Coal Dust)
     * charges gently, RC = 360 s: 800 (1 - e^(-20/360)) = 43 V in 20 s, at ~0.8 A, nothing harmed.
     */
    static final class Charge extends Room {
        CapacitorBankElement stiff, soft;
        SixNodeElement stiffCable, softCable;
        mods.eln.sixnode.resistor.ResistorElement resistor;

        public String id() {
            return "capacitor_bank_inrush";
        }

        public int ticks() {
            return 400;   // 20 s
        }

        public void build(TestContext ctx) {
            BlockPos c = ctx.at(4, 1, 3);
            stiff = (CapacitorBankElement) ctx.placeTransparent(BANK, c);
            Direction f = stiff.front, l = f.left(), r = f.right();
            stiffCable = ctx.placeSix(HV_CABLE, Wp10aCases.off(c, l));
            source(ctx, Wp10aCases.off(Wp10aCases.off(c, l), l), 800);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(c, r));
            // three blocks along its front: cable, resistor, source (the resistor helper lays its terminals east-west,
            // along a bank's flanks for this placer)
            BlockPos a = c;
            for (int i = 0; i < 3; i++) a = Wp10aCases.off(a, f);
            soft = (CapacitorBankElement) ctx.placeTransparent(BANK, a);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(a, r));
            BlockPos w = Wp10aCases.off(a, l);
            softCable = ctx.placeSix(HV_CABLE, w);
            w = Wp10aCases.off(w, l);
            resistor = Batch1Cases.resistor(ctx, w.getX() - ctx.at(0, 0, 0).getX(), w.getZ() - ctx.at(0, 0, 0).getZ(), 60);   // 1 kohm
            w = Wp10aCases.off(w, l);
            source(ctx, w, 800);
        }

        public void measure(TestContext ctx) {
            ctx.line("capacitor bank inrush: stiff " + stiff.getWaila() + " cable burnt " + stiffCable.isBurnt() + "; soft " + soft.getWaila()
                + String.format(" %.2f A", soft.capacitor.getCurrent()) + " cable " + softCable.getWaila() + " resistor burnt " + resistor.isBurnt());
            ctx.check("capacitor bank: empty, straight on a stiff 800 V source, it burns its HV cable", stiffCable.isBurnt(), "");
            double u = soft.voltage(), want = 800 * (1 - Math.exp(-20 / (1000 * CapacitorBankDescriptor.farads)));
            ctx.check("capacitor bank: behind 1 kohm it charges gently (RC 360 s: ~43 V in 20 s)", Math.abs(u - want) < 6,
                String.format("%.1f V, want %.1f", u, want));
            ctx.check("capacitor bank: ... nothing harmed", !softCable.isBurnt() && !resistor.isBurnt() && !soft.isBurnt(), "");
        }
    }

    /** A charged bank holds 0.5 C U^2 and leaks slowly. */
    static final class Hold extends Room {
        CapacitorBankElement held;

        public String id() {
            return "capacitor_bank_holds_and_pops";
        }

        public int ticks() {
            return 400;   // 20 s
        }

        public void build(TestContext ctx) {
            BlockPos c = ctx.at(4, 1, 3);
            held = (CapacitorBankElement) ctx.placeTransparent(BANK, c);
            Direction f = held.front, r = f.right();
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(c, r));   // (a floating bank has no reference: grounded, it keeps its charge)
            held.positiveLoad.setU(800);
            held.cellLoad.setU(800);
        }

        public void measure(TestContext ctx) {
            double hu = held.voltage(), he = held.energy();
            ctx.check("capacitor bank: a charged bank keeps its charge for a while (800 V, 20 s later >= 785 V)", hu > 785 && hu <= 800.5,
                String.format("%.1f V", hu));
            ctx.check("capacitor bank: and holds 0.5 C U^2 (~115 kJ at 800 V)", Math.abs(he - CapacitorBankDescriptor.energyAt(800)) < 3000,
                String.format("%.0f J", he));
            double leak = 800 * 800 / (CapacitorBankDescriptor.leakTauS / CapacitorBankDescriptor.farads);
            ctx.check("capacitor bank: it leaks ~130 W at 800 V (a store for bursts, not for keeping)", leak > 100 && leak < 160,
                String.format("%.0f W", leak));
        }
    }

    /**
     * Past 1.3 x its rating, or past its warm limit, a bank explodes by the energy it held (Baughn 2026-10-28):
     * bankStrength 1 .. 3, sqrt(E / 40 kJ). One charged to 1600 V (460 kJ: strength 3) goes in a few seconds.
     */
    static final class OverVoltage extends Room {
        BlockPos at;

        public String id() {
            return "capacitor_bank_explodes_over_voltage";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            at = ctx.at(4, 1, 3);
            var b = (CapacitorBankElement) ctx.placeTransparent(BANK, at);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(at, b.front.right()));
            b.positiveLoad.setU(1600);   // (300 V over: the watchdog's budget, 500 V s, goes in a few seconds)
            b.cellLoad.setU(1600);
        }

        public void measure(TestContext ctx) {
            float s115 = mods.eln.sim.process.destruct.Failure.bankStrength(115e3), s300 = mods.eln.sim.process.destruct.Failure.bankStrength(300e3);
            ctx.check("capacitor bank: explosion strength by stored energy (115 kJ 1.7, 300 kJ 2.7, at most a creeper's 3)",
                Math.abs(s115 - 1.70) < 0.01 && Math.abs(s300 - 2.74) < 0.01 && mods.eln.sim.process.destruct.Failure.bankStrength(1e7) == 3f
                    && mods.eln.sim.process.destruct.Failure.bankStrength(0) == 1f, s115 + " " + s300);
            ctx.check("capacitor bank: charged to 1600 V (1.6 x its rating), it explodes", ctx.level().getBlockState(at).isAir(),
                "" + ctx.level().getBlockState(at));
        }
    }

    /**
     * A stiff line hammering it: a source right on its + plate (no cable to burn first), switched 800 V / 0 V every 2 s.
     * Each charge and discharge puts most of the bank's ~115 kJ through its 0.1 ohm ESR, ~57 K a time on 2000 J/K: past
     * its warm limit (75 K over ambient) on the second, and it explodes.
     */
    static final class Hammered extends Room {
        BlockPos at;
        CapacitorBankElement bank;
        SixNodeElement src;
        mods.eln.sim.IProcess toggle;
        double clock, maxT;

        public String id() {
            return "capacitor_bank_overheats_and_explodes";
        }

        public int ticks() {
            return 300;   // 15 s
        }

        public void build(TestContext ctx) {
            at = ctx.at(4, 1, 3);
            bank = (CapacitorBankElement) ctx.placeTransparent(BANK, at);
            src = source(ctx, Wp10aCases.off(at, bank.front.left()), 800);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(at, bank.front.right()));
            mods.eln.Eln.simulator.addSlowProcess(toggle = t -> {
                clock += t;
                maxT = Math.max(maxT, bank.thermal.Tc);
                double v = ((int) (clock / 2)) % 2 == 0 ? 800 : 0;
                src.networkUnserialize(ctx.stream(out -> {
                    out.writeByte(ElectricalSourceElement.setVoltageId);
                    out.writeFloat((float) v);
                }));
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(toggle);
            ctx.line(String.format("capacitor bank hammered: hottest %.0f K over ambient", maxT));
            ctx.check("capacitor bank: hammered by a stiff line it overheats (past 75 K over ambient) and explodes",
                ctx.level().getBlockState(at).isAir() && maxT >= CapacitorBankElement.WARM, String.format("max %.0f K, %s", maxT, ctx.level().getBlockState(at)));
        }
    }

    /** A charged bank and the electrical anvil, no plant: the bank carries one 20 kJ burst, then the anvil rests. */
    static final class Burst extends Room {
        CapacitorBankElement bank;
        ElectricalAnvilElement anvil;

        public String id() {
            return "capacitor_bank_carries_an_anvil_burst";
        }

        public int ticks() {
            return 200;   // 10 s: a burst at ~5 kW takes ~4 s
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(4, 1, 3);
            bank = (CapacitorBankElement) ctx.placeTransparent(BANK, p);
            Direction f = bank.front, l = f.left(), r = f.right();
            BlockPos c = Wp10aCases.off(p, l);
            ctx.placeSix(HV_CABLE, c);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(p, r));
            // the anvil beside the cable, its back (800 V plate) on it: placed the same way, its back is -f
            anvil = (ElectricalAnvilElement) ctx.placeTransparent(AnvilCases.ANVIL, Wp10aCases.off(c, f));
            AnvilCases.put(anvil, AnvilCases.pick(ctx, 1500, 0, false), new ItemStack(Items.DIAMOND, 4));
            // charged to 800 V, as a plant would leave it (the capacitor's history is its loads' voltages)
            bank.positiveLoad.setU(800);
            bank.cellLoad.setU(800);
            bank.negativeLoad.setU(0);
        }

        public void measure(TestContext ctx) {
            ctx.line("capacitor bank burst: bank " + bank.getWaila() + "; anvil " + anvil.getWaila() + " energy " + anvil.energy);
            ctx.check("capacitor bank: carries a whole 20 kJ burst from 800 V", anvil.energy >= 19900
                && anvil.recovering, String.format("%.0f J, recovering %s", anvil.energy, anvil.recovering));
            ctx.check("capacitor bank: ... and is left between 720 and 784 V, so the anvil rests", bank.voltage() > 700 && bank.voltage() < 784,
                String.format("%.0f V", bank.voltage()));
        }
    }
}
