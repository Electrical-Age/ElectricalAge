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
        cases.add(new Burst());
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

    static SixNodeElement source(TestContext ctx, BlockPos p, double volts) {
        SixNodeElement src = ctx.placeSix(Wp10aCases.SOURCE, p);
        src.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat((float) volts);
        }));
        return src;
    }

    /**
     * A bank charged from a stiff 800 V source on its + flank (- to ground): the inrush limiter holds it to 3 A, so the
     * cable survives; a charged bank holds 0.5 C U^2 and leaks slowly; one charged past 1.3 x its rating pops.
     */
    static final class Charge extends Room {
        CapacitorBankElement bank, held, over;
        SixNodeElement cable;

        public String id() {
            return "capacitor_bank_charges_holds_and_pops";
        }

        public int ticks() {
            return 400;   // 20 s
        }

        public void build(TestContext ctx) {
            BlockPos c = ctx.at(4, 1, 3);
            bank = (CapacitorBankElement) ctx.placeTransparent(BANK, c);
            Direction f = bank.front, l = f.left(), r = f.right();
            cable = ctx.placeSix(HV_CABLE, Wp10aCases.off(c, l));
            source(ctx, Wp10aCases.off(Wp10aCases.off(c, l), l), 800);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(c, r));
            // two more, three blocks either way along its front, only grounded: one charged to 800 V, one past its rating
            BlockPos a = c, b = c;
            for (int i = 0; i < 3; i++) {
                a = Wp10aCases.off(a, f);
                b = Wp10aCases.off(b, f.getInverse());
            }
            held = (CapacitorBankElement) ctx.placeTransparent(BANK, a);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(a, r));   // (a floating bank has no reference: grounded, it keeps its charge)
            over = (CapacitorBankElement) ctx.placeTransparent(BANK, b);
            ctx.placeSix(Wp10aCases.GROUND, Wp10aCases.off(b, r));
            held.cellLoad.setU(800);
            held.positiveLoad.setU(800);
            over.cellLoad.setU(1600);   // (300 V over: the watchdog's budget, 500 V s, goes in a few seconds)
            over.positiveLoad.setU(1600);
        }

        public void measure(TestContext ctx) {
            double u = bank.voltage(), i = bank.capacitor.getCurrent();
            ctx.line("capacitor bank: charging " + bank.getWaila() + String.format(" %.2f A", i) + "; held " + held.getWaila()
                + "; over burnt " + over.isBurnt() + "; cable " + cable.getWaila());
            ctx.check("capacitor bank: an empty bank on a stiff 800 V line charges at ~3 A (the limiter), the cable unharmed",
                Math.abs(i - 3) < 0.3 && !cable.isBurnt(), String.format("%.2f A, cable burnt %s", i, cable.isBurnt()));
            ctx.check("capacitor bank: ... ~3 A x 20 s / 0.36 F = ~167 V", u > 140 && u < 190, String.format("%.0f V", u));
            double hu = held.voltage(), he = held.energy();
            ctx.check("capacitor bank: a charged bank keeps its charge for a while (800 V, 20 s later >= 785 V)", hu > 785 && hu <= 800.5,
                String.format("%.1f V", hu));
            ctx.check("capacitor bank: and holds 0.5 C U^2 (~115 kJ at 800 V)", Math.abs(he - CapacitorBankDescriptor.energyAt(800)) < 3000,
                String.format("%.0f J", he));
            double leak = 800 * 800 / (CapacitorBankDescriptor.leakTauS / CapacitorBankDescriptor.farads);
            ctx.check("capacitor bank: it leaks ~130 W at 800 V (a store for bursts, not for keeping)", leak > 100 && leak < 160,
                String.format("%.0f W", leak));
            ctx.check("capacitor bank: past 1.3 x its 1000 V rating it pops", over.isBurnt(), "");
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
            bank.cellLoad.setU(800);
            bank.positiveLoad.setU(800);
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
