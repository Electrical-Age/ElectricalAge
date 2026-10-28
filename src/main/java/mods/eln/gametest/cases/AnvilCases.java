package mods.eln.gametest.cases;

import mods.eln.anvil.ElectricalAnvilElement;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/**
 * The electrical anvil (Baughn 2026-10-28): repair with material, no XP, the penalty never raised and annealed, credit
 * kept for fractions, repair only (books and enchanted copies refused), engraving, and the 800 V pulse.
 */
public final class AnvilCases {
    private AnvilCases() {
    }

    static final int ANVIL = 0 + (85 << 6);
    static final int HV_CABLE = 12 + (32 << 6);

    public static void addTo(List<TestCase> cases) {
        cases.add(new Rules());
        cases.add(new Pulse());
        cases.add(new Sag());
    }

    /**
     * A line that can't carry it: the pulse stops below 90% of 800 V, keeps its progress, and waits a second before it
     * tries again (no chattering); it starts only at 98%. The line's voltage is set by hand, tick by tick.
     */
    static final class Sag implements TestCase {
        public String id() {
            return "anvil_pauses_on_a_sag";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 1;
        }

        ElectricalAnvilElement a;

        public void build(TestContext ctx) {
            a = (ElectricalAnvilElement) ctx.placeTransparent(ANVIL, ctx.at(1, 1, 1));
            put(a, pick(ctx, 1000, 0, false), new ItemStack(Items.DIAMOND, 2));
        }

        void tick(double u, int n) {
            for (int i = 0; i < n; i++) {
                a.load.setU(u);
                a.process.process(0.05);
            }
        }

        public void measure(TestContext ctx) {
            tick(760, 5);
            ctx.check("anvil: at 760 V (95%) it doesn't start", !a.welding && a.status == ElectricalAnvilElement.WAITING && a.energy == 0, "" + a.status);
            tick(800, 20);
            double e = a.energy;
            ctx.check("anvil: at 800 V it welds (1 s: ~5 kJ)", a.welding && Math.abs(e - 5000) < 300, a.welding + " " + e);
            tick(600, 1);
            ctx.check("anvil: a sag to 600 V pauses it, keeping its progress", !a.welding && a.status == ElectricalAnvilElement.SAGGING
                && a.energy == e, a.status + " " + a.energy);
            tick(800, 10);
            ctx.check("anvil: back at 800 V it holds off for a second", !a.welding && a.energy == e, "" + a.welding);
            tick(800, 12);
            ctx.check("anvil: ... then welds on from where it was", a.welding && a.energy > e, a.welding + " " + a.energy);
            a.getInventory().setItem(ElectricalAnvilElement.TOOL, ItemStack.EMPTY);
            tick(0, 1);
        }
    }

    static ItemStack pick(TestContext ctx, int damage, int cost, boolean enchanted) {
        ItemStack s = new ItemStack(Items.DIAMOND_PICKAXE);
        if (enchanted)
            s.enchant(ctx.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
        s.setDamageValue(damage);
        s.set(DataComponents.REPAIR_COST, cost);
        return s;
    }

    static void put(ElectricalAnvilElement a, ItemStack tool, ItemStack material) {
        a.getInventory().setItem(ElectricalAnvilElement.TOOL, tool);
        a.getInventory().setItem(ElectricalAnvilElement.MATERIAL, material);
    }

    static ItemStack tool(ElectricalAnvilElement a) {
        return a.getInventory().getItem(ElectricalAnvilElement.TOOL);
    }

    /** The rules, step by step (complete() as the pulse would call it; no power needed). */
    static final class Rules implements TestCase {
        public String id() {
            return "anvil_repairs_without_xp_and_anneals";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 1;
        }

        ElectricalAnvilElement a;

        public void build(TestContext ctx) {
            a = (ElectricalAnvilElement) ctx.placeTransparent(ANVIL, ctx.at(1, 1, 1));
        }

        public void measure(TestContext ctx) {
            // a well-used enchanted pick (penalty 31): one diamond mends a quarter (390), the penalty steps down a notch
            put(a, pick(ctx, 1500, 31, true), new ItemStack(Items.DIAMOND, 5));
            var j = a.job();
            ctx.check("anvil: a damaged pick and diamonds make a repair step", j != null && !j.rename() && j.restore() == 390, "" + j);
            a.complete(j);
            ctx.check("anvil: a step restores a quarter and takes one diamond", tool(a).getDamageValue() == 1110
                && a.getInventory().getItem(ElectricalAnvilElement.MATERIAL).getCount() == 4, tool(a).getDamageValue() + " "
                + a.getInventory().getItem(ElectricalAnvilElement.MATERIAL));
            ctx.check("anvil: the penalty never rises, it steps down (31 -> 15)", tool(a).getOrDefault(DataComponents.REPAIR_COST, -1) == 15,
                "" + tool(a).get(DataComponents.REPAIR_COST));
            for (int i = 0; i < 3; i++) a.complete(a.job());
            ctx.check("anvil: fully mended by four diamonds; annealed to the floor (one enchantment: 1)", tool(a).getDamageValue() == 0
                && tool(a).getOrDefault(DataComponents.REPAIR_COST, -1) == 1 && a.getInventory().getItem(ElectricalAnvilElement.MATERIAL).getCount() == 1,
                tool(a).getDamageValue() + " " + tool(a).get(DataComponents.REPAIR_COST));
            ctx.check("anvil: a mended tool is done", a.job() == null && a.status == ElectricalAnvilElement.DONE, "" + a.status);
            // topping up: 100 of 1561 missing is 0.26 of a diamond; the rest stays as credit and pays the next top-up
            put(a, pick(ctx, 100, 0, false), new ItemStack(Items.DIAMOND, 1));
            a.credit.clear();
            a.complete(a.job());
            double c = a.credit.getOrDefault("minecraft:diamond", 0.0);
            ctx.check("anvil: topping up takes a whole diamond and keeps the change as credit (0.74)",
                tool(a).getDamageValue() == 0 && a.getInventory().getItem(ElectricalAnvilElement.MATERIAL).isEmpty() && Math.abs(c - 290 / 390.0) < 1e-6,
                "credit " + c + ", damage " + tool(a).getDamageValue());
            put(a, pick(ctx, 200, 0, false), ItemStack.EMPTY);
            var k = a.job();
            ctx.check("anvil: credit alone repairs (no diamond in the slot)", k != null && k.restore() == 200, "" + k);
            a.complete(k);
            ctx.check("anvil: ... and is used up by it", tool(a).getDamageValue() == 0
                && Math.abs(a.credit.getOrDefault("minecraft:diamond", 0.0) - 90 / 390.0) < 1e-6, "" + a.credit);
            CompoundTag t = new CompoundTag();
            a.writeToNBT(t);
            ctx.check("anvil: the credit is saved with the desk", t.getCompoundOrEmpty("credit").getDoubleOr("minecraft:diamond", 0) > 0.2, "" + t);
            // repair only: a book, an enchanted copy, the wrong material: no job
            put(a, pick(ctx, 500, 0, false), new ItemStack(Items.ENCHANTED_BOOK));
            a.credit.clear();
            ctx.check("anvil: an enchanted book is refused (no combining)", a.job() == null && a.status == ElectricalAnvilElement.NO_MATERIAL, "" + a.status);
            put(a, pick(ctx, 500, 0, false), pick(ctx, 0, 0, true));
            ctx.check("anvil: an enchanted copy is refused", a.job() == null, "");
            put(a, pick(ctx, 500, 0, false), new ItemStack(Items.IRON_INGOT, 5));
            ctx.check("anvil: the wrong material is refused", a.job() == null, "");
            // a plain copy repairs like vanilla's two-tools repair (its durability + 12%)
            put(a, pick(ctx, 1000, 7, true), pick(ctx, 1200, 0, false));
            var cp = a.job();
            ctx.check("anvil: a plain copy is material (361 + 187 = 548)", cp != null && cp.copy() && cp.restore() == 548, "" + cp);
            a.complete(cp);
            ctx.check("anvil: ... consumed, the tool mended, the penalty annealed (7 -> 3)", a.getInventory().getItem(ElectricalAnvilElement.MATERIAL).isEmpty()
                && tool(a).getDamageValue() == 452 && tool(a).getOrDefault(DataComponents.REPAIR_COST, -1) == 3, tool(a).getDamageValue() + " " + tool(a).get(DataComponents.REPAIR_COST));
            // engraving: a name job first, then it's set; the penalty doesn't change
            a.networkUnserialize(ctx.stream(o -> {
                o.writeByte(ElectricalAnvilElement.SET_NAME);
                o.writeUTF("Old Faithful");
            }));
            var nj = a.job();
            ctx.check("anvil: a new name is an engraving job (2 kJ)", nj != null && nj.rename() && nj.joules() == 2000, "" + nj);
            a.complete(nj);
            ctx.check("anvil: engraved, the penalty unchanged", "Old Faithful".equals(tool(a).getHoverName().getString())
                && tool(a).getOrDefault(DataComponents.REPAIR_COST, -1) == 3, tool(a).getHoverName().getString());
            // not the desk's: EA's battery-like items, and undamageable things
            put(a, new ItemStack(Items.STICK), ItemStack.EMPTY);
            ctx.check("anvil: a stick can't be mended", a.job() == null && a.status == ElectricalAnvilElement.CANT, "" + a.status);
            put(a, ItemStack.EMPTY, ItemStack.EMPTY);
        }
    }

    /** The 800 V pulse: on a stiff 800 V line a unit (40 kJ) takes 8 s at 5 kW; below 784 V it waits. */
    static final class Pulse implements TestCase {
        public String id() {
            return "anvil_800v_pulse";
        }

        // (room for its cable and source two blocks behind, whichever way the placer faced)
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

        public int ticks() {
            return 220;   // 11 s: one step, a second under way
        }

        ElectricalAnvilElement fed, weak;
        ItemStack fedTool;

        ElectricalAnvilElement desk(TestContext ctx, int x, double volts) {
            BlockPos p = ctx.at(x, 1, 3);
            var a = (ElectricalAnvilElement) ctx.placeTransparent(ANVIL, p);
            Direction back = a.front.back();
            BlockPos cp = Wp10aCases.off(p, back), sp = Wp10aCases.off(cp, back);
            ctx.placeSix(HV_CABLE, cp);
            SixNodeElement src = ctx.placeSix(Wp10aCases.SOURCE, sp);
            src.networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat((float) volts);
            }));
            put(a, pick(ctx, 1500, 15, false), new ItemStack(Items.DIAMOND, 4));
            return a;
        }

        public void build(TestContext ctx) {
            fed = desk(ctx, 2, 800);
            weak = desk(ctx, 6, 700);
        }

        public void measure(TestContext ctx) {
            ctx.line("anvil pulse: fed " + fed.getWaila() + " damage " + tool(fed).getDamageValue() + "; weak " + weak.getWaila());
            ctx.check("anvil: on 800 V it welds (one quarter in 11 s, at ~5 kW)", tool(fed).getDamageValue() == 1500 - 390 && fed.welding
                && fed.powerNow() > 4500 && fed.powerNow() < 5100, tool(fed).getDamageValue() + " " + fed.powerNow() + " W");
            ctx.check("anvil: ... and is part way into the next", fed.progress() > 0.2 && fed.progress() < 0.6, "" + fed.progress());
            ctx.check("anvil: on 700 V it waits for the line (no load, no progress)", !weak.welding && weak.status == ElectricalAnvilElement.WAITING
                && weak.progress() == 0 && tool(weak).getDamageValue() == 1500, weak.status + " " + weak.progress());
        }
    }
}
