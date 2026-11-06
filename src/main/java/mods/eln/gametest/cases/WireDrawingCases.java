package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CableSpec;
import mods.eln.node.voxel.KitItem;
import mods.eln.node.voxel.WindingItem;
import mods.eln.transparentnode.wiredrawing.WireDrawingElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The wire-drawing machines (Baughn 2026-11-06, Wires stage 4): kits and windings to a spec, the machine's limits, the
 * copper and rubber taken (whole items into buffers that keep the fractions), pauses, and saves.
 */
public final class WireDrawingCases {
    private WireDrawingCases() {
    }

    static final int MACHINE_48 = 0 + (88 << 6), MACHINE_230 = 4 + (88 << 6);

    public static void addTo(List<TestCase> cases) {
        cases.add(new Rules());
        cases.add(new Draws());
    }

    static ItemStack rubber(int n) {
        return new ItemStack(mods.eln.Eln.itemsByName.get("Rubber"), n);
    }

    /** Limits, products, pauses and the save, ticked by hand (no supply). */
    static final class Rules implements TestCase {
        public String id() {
            return "wire_drawing_rules";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 1;
        }

        WireDrawingElement lv, mv;

        public void build(TestContext ctx) {
            lv = (WireDrawingElement) ctx.placeTransparent(MACHINE_48, ctx.at(1, 1, 1));
            mv = (WireDrawingElement) ctx.placeTransparent(MACHINE_230, ctx.at(3, 1, 1));
        }

        public void measure(TestContext ctx) {
            ctx.check("wire drawing: the 48 V machine starts on brown 2.5 mm², 48 V", lv.mm2 == 4 && lv.tier == 1 && lv.colour == CableSpec.BROWN, lv.mm2 + " " + lv.tier + " " + lv.colour);
            ctx.check("wire drawing: the 230 V machine starts on black 1.5 mm², 230 V", mv.mm2 == 3 && mv.tier == 2 && mv.colour == CableSpec.BLACK, mv.mm2 + " " + mv.tier + " " + mv.colour);
            lv.configure(WireDrawingElement.KIT, 13, 4, 3, 100);
            ctx.check("wire drawing: the 48 V machine makes at most 6 mm², 230 V insulation, 64 blocks", lv.mm2 == 6 && lv.tier == 2 && lv.amount == 64,
                lv.mm2 + " " + lv.tier + " " + lv.amount);
            mv.configure(WireDrawingElement.WINDING, 0, 4, CableSpec.RED, 5000);
            ctx.check("wire drawing: a winding has at most 1000 turns", mv.amount == WindingItem.MAX_TURNS, "" + mv.amount);
            mv.configure(WireDrawingElement.WINDING, 0, 4, CableSpec.RED, 688);
            ItemStack w = mv.product();
            ctx.check("wire drawing: the 230 V machine winds 688 turns of 0.5 mm² 11 kV wire", WindingItem.is(w) && WindingItem.turns(w) == 688
                && WindingItem.tier(w) == 4 && WindingItem.spec(w).equals(new CableSpec(0, CableSpec.RED)), w + " " + w.getComponents());
            ctx.checkValue("wire drawing: ... from 86 blocks of wire's copper (ingots)", mv.copperNeeded(), 688 / 8.0 * 0.5 * 2 / 3 * 0.5 / 2.5);
            mv.configure(WireDrawingElement.KIT, 4, 1, CableSpec.BROWN, 64);
            ItemStack k = mv.product();
            ctx.check("wire drawing: a brown 2.5 mm² 48 V kit is the preset kit (it merges with crafted ones)", CableKitItem.isPreset(k) && KitItem.length(k) == 1024, k.toString());
            ctx.checkValue("wire drawing: ... and costs two thirds of the hand recipe's copper (21.3 ingots for 64 blocks; 32 by hand)", mv.copperNeeded(), 64 * 0.5 * 2 / 3);
            ctx.checkValue("wire drawing: ... and two thirds of its rubber (42.7 for 64 blocks; 64 by hand)", mv.rubberNeeded(), 64 * 2.0 / 3);
            // pauses
            mv.running = true;
            mv.tick(0.05);
            ctx.check("wire drawing: no copper, it waits for copper", mv.status == WireDrawingElement.NO_COPPER, "" + mv.status);
            mv.getInventory().setItem(WireDrawingElement.COPPER, new ItemStack(Items.COPPER_INGOT, 2));
            mv.tick(0.05);
            ctx.check("wire drawing: no rubber, it waits for rubber", mv.status == WireDrawingElement.NO_RUBBER, "" + mv.status);
            mv.getInventory().setItem(WireDrawingElement.RUBBER, rubber(2));
            mv.tick(0.05);
            ctx.check("wire drawing: unpowered, it says so", mv.status == WireDrawingElement.NO_POWER, "" + mv.status);
            ctx.check("wire drawing: it took one copper ingot and one rubber into its buffers", mv.copper == 1 && mv.rubber == 1
                && mv.getInventory().getItem(WireDrawingElement.COPPER).getCount() == 1 && mv.getInventory().getItem(WireDrawingElement.RUBBER).getCount() == 1,
                mv.copper + " " + mv.rubber);
            mv.getInventory().setItem(WireDrawingElement.OUT, new ItemStack(Items.STICK));
            mv.tick(0.05);
            ctx.check("wire drawing: the output slot full, it waits", mv.status == WireDrawingElement.OUTPUT_FULL, "" + mv.status);
            // the save
            mv.done = 12.5;
            mv.copper = 0.25;
            var nbt = new net.minecraft.nbt.CompoundTag();
            mv.writeToNBT(nbt);
            mv.done = 0;
            mv.configure(WireDrawingElement.WINDING, 0, 2, 0, 3);
            mv.readFromNBT(nbt);
            ctx.check("wire drawing: a save keeps what it makes, its progress and its buffers", mv.mode == WireDrawingElement.KIT && mv.amount == 64
                && mv.done == 12.5 && mv.copper == 0.25 && mv.running, mv.mode + " " + mv.amount + " " + mv.done + " " + mv.copper);
            mv.configure(WireDrawingElement.KIT, 4, 1, CableSpec.RED, 64);
            ctx.check("wire drawing: changing what to make scraps the progress", mv.done == 0, "" + mv.done);
        }
    }

    /**
     * The 48 V machine fed at 48 V on its back: 4 blocks of red 6 mm² 48 V cable (0.8 copper and 1.03 rubber a block,
     * 1.55 kJ a block: 6.2 kJ, about 16 s at 400 W). It takes 4 ingots and 5 rubber, keeps 0.8 ingot and 0.87 rubber,
     * and the kit comes out with the spec.
     */
    static final class Draws implements TestCase {
        public String id() {
            return "wire_drawing_makes_a_kit";
        }

        public int width() {
            return 8;
        }

        public int ticks() {
            return 440;
        }

        WireDrawingElement m;

        public void build(TestContext ctx) {
            var l = new Wp10aCases.Layout(ctx, width());
            BlockPos p = ctx.at(3, 1, 1);
            m = (WireDrawingElement) l.device(MACHINE_48, 3, 1);
            l.feed(p, m.front.back());
            m.getInventory().setItem(WireDrawingElement.COPPER, new ItemStack(Items.COPPER_INGOT, 10));
            m.getInventory().setItem(WireDrawingElement.RUBBER, rubber(10));
            m.configure(WireDrawingElement.KIT, 6, 1, CableSpec.RED, 4);
            m.running = true;
        }

        public void measure(TestContext ctx) {
            ItemStack out = m.getInventory().getItem(WireDrawingElement.OUT);
            ctx.line("wire drawing: " + m.getWaila());
            ctx.check("wire drawing: a kit of 4 blocks of red 6 mm² came out", out.getItem() instanceof CableKitItem k && k.tier == 1
                && new CableSpec(6, CableSpec.RED).equals(CableKitItem.specOf(out)) && KitItem.length(out) == 64, out + " " + out.getComponents());
            ctx.check("wire drawing: it took 4 copper ingots and 5 rubber", m.getInventory().getItem(WireDrawingElement.COPPER).getCount() == 6
                && m.getInventory().getItem(WireDrawingElement.RUBBER).getCount() == 5,
                m.getInventory().getItem(WireDrawingElement.COPPER) + " " + m.getInventory().getItem(WireDrawingElement.RUBBER));
            ctx.checkValue("wire drawing: copper left in the machine (ingots)", m.copper, 4 - 4 * 0.8);
            ctx.check("wire drawing: then it waits for the kit to be taken", m.status == WireDrawingElement.OUTPUT_FULL, "" + m.status);
        }
    }
}
