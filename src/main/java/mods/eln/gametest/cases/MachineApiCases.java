package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.api.MachineState;
import mods.eln.api.impl.MachineCore;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Recipe;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.transparentnode.electricalmachine.CompressorDescriptor;
import mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor;
import mods.eln.transparentnode.electricalmachine.ElectricalMachineElement;
import mods.eln.transparentnode.electricalmachine.MagnetizerDescriptor;
import mods.eln.transparentnode.electricalmachine.PlateMachineDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The electrical machines on the Machine API (Machines.forElement): each of the eight (50 V and 200 V macerator,
 * compressor, magnetizer, plate machine) on a real supply at its own voltage does its work at the speed and for the
 * energy of the resistor it had before (that model is run alongside from the volts the machine sees), and slower on a
 * sagging line; a machine reloaded mid-item keeps its progress; one saved by the version before loads and runs; one
 * fed too high burns out and a repair brings it back.
 */
public final class MachineApiCases {
    private MachineApiCases() {
    }

    static final int MV_CABLE = 8 + (32 << 6);   // Medium Voltage Cable

    public static void addTo(List<TestCase> cases) {
        for (int k = 0; k < Wp10aCases.MACHINES.length; k++) cases.add(new ParityCase(k, 1));
        cases.add(new ParityCase(0, 0.7));
        cases.add(new ParityCase(5, 0.7));
        cases.add(new MidSaveCase());
        cases.add(new OldSaveCase());
        cases.add(new BurnCase());
        cases.add(new SpecsVerifyCase());
    }

    /**
     * Every EA device on the Machine API passes ElnTestKit.verifySpec with the spec it really builds: the eight
     * electrical machines (from their descriptors), the exporters, the Linear Servo Drive. In game, so the specs come
     * from the registered descriptors; the bench runs a simulator of its own (off the world's field buses).
     */
    static final class SpecsVerifyCase implements TestCase {
        public String id() {
            return "api_ea_specs_verify";
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext c) {
        }

        public void measure(TestContext c) {
            java.util.Map<String, mods.eln.api.MachineSpec> specs = new java.util.LinkedHashMap<>();
            for (var e : Eln.transparentNodeItem.all().entrySet()) {
                var d = e.getValue();
                if (d instanceof ElectricalMachineDescriptor m) specs.put(d.name, ElectricalMachineElement.spec(m, 1));
                else if (d instanceof mods.eln.transparentnode.energyconverter.EnergyConverterElnToOtherDescriptor x)
                    specs.put(d.name, x.machineSpec());
                else if (d instanceof mods.eln.robotics.servo.LinearServoDriveDescriptor) specs.put(d.name,
                    mods.eln.robotics.servo.LinearServoDriveDescriptor.spec());
            }
            c.check("specs: the eight machines, the exporters and the drive", specs.size() >= 12, specs.keySet().toString());
            for (var e : specs.entrySet()) {
                String detail;
                boolean ok;
                try {
                    var r = mods.eln.api.testkit.ElnTestKit.verifySpec(e.getValue());
                    ok = true;
                    detail = r.checks().size() + " checks, " + r.checks().stream().filter(k -> k.result() == mods.eln.api.testkit.SpecReport.Result.SKIP)
                        .map(k -> "skip " + k.name() + " (" + k.detail() + ")").toList();
                } catch (mods.eln.api.testkit.ElnTestKit.SpecFailure f) {
                    ok = false;
                    detail = f.getMessage();
                }
                c.check("verifySpec: " + e.getKey() + " (" + e.getValue() + ")", ok, detail);
            }
        }
    }

    /** A stack this machine works on: copper (macerator, plate machine), sand (compressor), iron (magnetizer). */
    static ItemStack input(ElectricalMachineElement m, int count) {
        var d = m.getDescriptor();
        if (d instanceof CompressorDescriptor) return new ItemStack(Items.SAND, count);
        if (d instanceof MagnetizerDescriptor) return new ItemStack(Items.IRON_INGOT, count);
        if (d instanceof PlateMachineDescriptor) return new ItemStack(Items.COPPER_INGOT, count);
        return new ItemStack(Items.COPPER_INGOT, count);
    }

    static Recipe recipe(ElectricalMachineElement m) {
        return ((ElectricalMachineDescriptor) m.getDescriptor()).recipe.getRecipe(input(m, 64));
    }

    static ElectricalStackMachineProcess process(ElectricalMachineElement m) {
        return Wp10aCases.field(m, "slowRefreshProcess");
    }

    /** Places machine id at x = 2 fed on its left at ratio x its nominal volts (LV cable at 50 V, MV at 200 V). */
    static ElectricalMachineElement place(TestContext ctx, Wp10aCases.Layout l, int id, double ratio) {
        ElectricalMachineElement m = (ElectricalMachineElement) l.device(id, 2, 1);
        double un = m.machine.spec().nominalVolts();
        l.feed(ctx.at(2, 1, 1), m.front.left(), un > 100 ? MV_CABLE : Wp10aCases.LV_CABLE, un * ratio);
        return m;
    }

    static ElectricalMachineElement element(TestContext ctx, BlockPos p) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, ctx.level()));
        return (ElectricalMachineElement) ((TransparentNode) n).element;
    }

    /**
     * One machine at ratio x nominal volts over a window of 200 ticks (after 40 to start): the work done (items x
     * recipe energy plus progress) and the line energy match the old resistor (nominal ohms, Unom^2 / Pnom, its
     * counter reset at each item) run on the same volts, within 3%; at nominal it works at its rated power, on a
     * 0.7 x line at about half.
     */
    static final class ParityCase implements TestCase {
        static final int W0 = 40, W1 = 240;
        final int k;
        final double ratio;
        ElectricalMachineElement m;
        IProcess watch;
        int tick, in0, in1;
        double e, p0, p1, line0, line1, oldCounter, oldLine, voltsSum;
        int oldOps;

        ParityCase(int k, double ratio) {
            this.k = k;
            this.ratio = ratio;
        }

        public String name() {
            return "machine api parity " + k + (ratio < 1 ? " sagging" : "");
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return W1 + 10;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            m = place(ctx, l, Wp10aCases.MACHINES[k], ratio);
            m.getInventory().setItem(m.inSlotId, input(m, 64));
            m.getInventory().setChanged();
            e = recipe(m).energy;
            double un = m.machine.spec().nominalVolts(), pn = m.machine.spec().ratedWatts();
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                if (tick == W0) {
                    in0 = m.getInventory().getItem(m.inSlotId).getCount();
                    p0 = process(m).progressJoules();
                    line0 = m.machine.joulesDelivered();
                    oldCounter = p0;
                } else if (tick > W0 && tick <= W1) {
                    double v = m.machine.volts(), j = pn * (v / un) * (v / un) * dt;   // the old resistor, Unom^2 / Pnom
                    voltsSum += v;
                    oldLine += j;
                    oldCounter += j;
                    if (oldCounter > e) {
                        oldOps++;
                        oldCounter = 0;
                    }
                    if (tick == W1) {
                        in1 = m.getInventory().getItem(m.inSlotId).getCount();
                        p1 = process(m).progressJoules();
                        line1 = m.machine.joulesDelivered();
                    }
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            int per = recipe(m).input.getCount(), ops = (in0 - in1) / per;
            double work = ops * e + p1 - p0, oldWork = oldOps * e + oldCounter - p0, line = line1 - line0;
            double rated = m.machine.spec().ratedWatts() * (W1 - W0) * MachineCore.TICK_SECONDS;
            ctx.line(String.format("%s at %.1f V (mean): %d ops, work %.0f J (old resistor %d ops, %.0f J), line %.0f J (old %.0f J), rated %.0f J",
                m.getDescriptor().name, voltsSum / (W1 - W0), ops, work, oldOps, oldWork, line, oldLine, rated));
            ctx.check(name() + ": works as fast as the old resistor (3%)", Math.abs(work / oldWork - 1) < 0.03, work + " vs " + oldWork + " J");
            ctx.check(name() + ": draws what the old resistor drew (3%)", Math.abs(line / oldLine - 1) < 0.03, line + " vs " + oldLine + " J");
            ctx.check(name() + ": the same items", Math.abs(ops - oldOps) <= 1, ops + " vs " + oldOps);
            if (ratio == 1) ctx.check(name() + ": at its rated power on a nominal line", work > 0.95 * rated && work < 1.01 * rated, work + " of " + rated + " J");
            else ctx.check(name() + ": slower on a sagging line (about ratio^2)", work < 0.55 * rated && work > 0.4 * rated, work + " of " + rated + " J");
            ctx.check(name() + ": running", m.machine.state() == MachineState.RUNNING, "" + m.machine.state());
        }
    }

    /**
     * A 50 V macerator saved and loaded mid-item (at tick 120 of 2000 J): its progress and inventory come back, and
     * after a short restart (the machine waits 0.5 s for a steady line) it finishes the item and goes on with the next.
     */
    static final class MidSaveCase implements TestCase {
        BlockPos pos;
        ElectricalMachineElement m;
        double progress = -1, restored = -1;
        int count, restoredCount, outBefore;

        public String name() {
            return "machine api save mid item";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pos = ctx.at(2, 1, 1);
            m = place(ctx, l, Wp10aCases.MACHINES[0], 1);
            m.getInventory().setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT, 3));
            m.getInventory().setChanged();
            ctx.after(120, () -> {
                progress = process(m).progressJoules();
                count = m.getInventory().getItem(m.inSlotId).getCount();
                outBefore = m.getInventory().getItem(m.outSlotId).getCount();
                try {
                    Reload.node(ctx.level(), pos);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                m = element(ctx, pos);
                restored = process(m).progressJoules();
                restoredCount = m.getInventory().getItem(m.inSlotId).getCount();
            });
        }

        public void measure(TestContext ctx) {
            ctx.check("mid-item save: progress was partway", progress > 500 && progress < 1500, progress + " J of 2000");
            ctx.check("mid-item save: progress loaded", restored == progress, restored + " vs " + progress);
            ctx.check("mid-item save: inventory loaded", restoredCount == count && count == 3 && outBefore == 0, restoredCount + " vs " + count);
            // 280 ticks after the load at 10 J a tick, less the restart: the first item and most of the second
            double work = (3 - m.getInventory().getItem(m.inSlotId).getCount()) * 2000.0 + process(m).progressJoules();
            double expect = progress + (280 - 15) * 10;
            ctx.check("mid-item save: it goes on from where it was", Math.abs(work - expect) < 0.05 * expect, work + " J done, expected about " + expect);
            ctx.check("mid-item save: the item is out", m.getInventory().getItem(m.outSlotId).getCount() >= 1, "" + m.getInventory().getItem(m.outSlotId));
        }
    }

    /**
     * A 50 V macerator as the version before the Machine API saved it (written out here as its writeToNBT did:
     * inventory, electricalLoadUc, powerOn, others; no api.machine keys, no progress) loads with its items, boosters
     * and front, and runs at its boosted speed (two boosters: 1.375^2 x 200 W).
     */
    static final class OldSaveCase implements TestCase {
        BlockPos pos;
        ElectricalMachineElement m;
        mods.eln.misc.Direction front;
        String loadedWith = "";

        public String name() {
            return "machine api loads an old save";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pos = ctx.at(2, 1, 1);
            m = place(ctx, l, Wp10aCases.MACHINES[0], 1);
            front = m.front;
            SimpleContainer inv = new SimpleContainer(m.getInventory().getContainerSize());
            inv.setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT, 5));
            inv.setItem(m.inSlotId + 1, Eln.findItemStack("Machine Booster", 2));
            CompoundTag old = new CompoundTag();
            mods.eln.misc.Utils.writeToNBT(old, "inv", inv);
            old.putFloat("electricalLoadUc", 49.9f);
            old.putByte("others", (byte) (front.getInt() + 8));
            old.putBoolean("powerOn", false);
            try {
                Reload.node(ctx.level(), pos, t -> {
                    t.put("element", old);
                    return t;
                });
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            m = element(ctx, pos);
            for (String k : old.keySet()) loadedWith += k + " ";
        }

        public void measure(TestContext ctx) {
            ctx.line("old save keys: " + loadedWith);
            ctx.check("old save: front kept", m.front == front, m.front + " vs " + front);
            ctx.check("old save: boosters kept", m.getInventory().getItem(m.inSlotId + 1).getCount() == 2, "" + m.getInventory().getItem(m.inSlotId + 1));
            double rated = 200 * 1.375 * 1.375;
            ctx.check("old save: rated for its boosters", Math.abs(m.machine.spec().ratedWatts() - rated) < 1e-6, m.machine.spec().ratedWatts() + " W");
            // 200 ticks, about 15 to start, at 378 W x (1/1.1)^2 into the work: about 2.9 kJ, one item and most of the next
            double work = (5 - m.getInventory().getItem(m.inSlotId).getCount()) * 2000.0 + process(m).progressJoules();
            double expect = (200 - 15) * rated * 0.05 / 1.21;
            ctx.check("old save: it runs at its boosted speed", Math.abs(work - expect) < 0.06 * expect, work + " J, expected about " + expect);
        }
    }

    /**
     * A 50 V macerator fed 80 V burns out (over 1.3 x nominal, as its watchdog had it); repaired and fed 50 V it
     * runs again.
     */
    static final class BurnCase implements TestCase {
        ElectricalMachineElement m;
        ElectricalSourceElement src;
        boolean burnt, burntAfterLoad;

        public String name() {
            return "machine api burns and is repaired";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            m = (ElectricalMachineElement) l.device(Wp10aCases.MACHINES[0], 2, 1);
            src = (ElectricalSourceElement) l.feed(ctx.at(2, 1, 1), m.front.left(), MV_CABLE, 80)[0]; // (an LV cable would burn at 80 V too)
            m.getInventory().setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT, 3));
            m.getInventory().setChanged();
            ctx.after(40, () -> {
                burnt = m.isBurnt();
                try {   // saved and loaded while burnt: still burnt, and a repair is all it needs
                    Reload.node(ctx.level(), ctx.at(2, 1, 1));
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                m = element(ctx, ctx.at(2, 1, 1));
                burntAfterLoad = m.isBurnt();
                src.networkUnserialize(ctx.stream(out -> {
                    out.writeByte(ElectricalSourceElement.setVoltageId);
                    out.writeFloat(50f);
                }));
                m.repair();
            });
        }

        public void measure(TestContext ctx) {
            ctx.check("80 V on a 50 V machine burns it", burnt, "");
            ctx.check("burnt, it loads burnt", burntAfterLoad, "");
            ctx.check("the burn was an overvoltage", m.machine.lastTrip().map(t -> t.burns() && t.state() == MachineState.OVERVOLTAGE_TRIPPED).orElse(false),
                "" + m.machine.lastTrip());
            ctx.check("repaired, it runs again", !m.isBurnt() && m.machine.state() == MachineState.RUNNING
                && process(m).progressJoules() + (3 - m.getInventory().getItem(m.inSlotId).getCount()) * 2000 > 500,
                m.machine.state() + ", progress " + process(m).progressJoules());
        }
    }
}
