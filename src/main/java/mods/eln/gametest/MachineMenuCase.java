package mods.eln.gametest;

import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Direction;
import mods.eln.node.NodeMenus;
import mods.eln.node.transparent.TransparentNode;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** A player puts ore into a macerator's input through its menu (P4b; AdventurAgent saw iron ore refused). */
final class MachineMenuCase implements TestCase {
    public String id() {
        return "machine_menu";
    }

    public int width() {
        return 3;
    }

    public int ticks() {
        return 5;
    }

    TransparentNode node;
    ElnNodeMenu menu;

    public void build(TestContext ctx) {
        node = ctx.placeTransparent(LabCommands.transparentId("50V Macerator"), ctx.at(1, 1, 1)).node;
        FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.level());
        menu = NodeMenus.menu(1, fp, node, Direction.YN);
        menu.setCarried(new ItemStack(Items.IRON_ORE, 8));
        menu.clicked(4, 0, ContainerInput.PICKUP, fp);
        ctx.check("macerator: iron ore taken by the input slot", menu.getSlot(4).getItem().is(Items.IRON_ORE),
            "slot 4 holds " + menu.getSlot(4).getItem() + ", carried " + menu.getCarried());
        // shift-click (Baughn: items went into the output slots): a stack the macerator has no recipe for goes nowhere
        // in the device, ore goes to the input only; nothing can be put in an output
        int dev = menu.deviceSlots;
        menu.setCarried(ItemStack.EMPTY);
        fp.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS, 16));
        menu.quickMoveStack(fp, dev);   // (the first main-inventory slot: inventory index 9)
        boolean outsEmpty = true;
        for (int i = 0; i < 4; i++) outsEmpty &= menu.getSlot(i).getItem().isEmpty();
        ctx.check("macerator: shift-clicked planks go into no device slot", outsEmpty && !menu.getSlot(4).getItem().is(Items.OAK_PLANKS),
            "outputs empty " + outsEmpty + ", input " + menu.getSlot(4).getItem());
        ctx.check("macerator: an output slot takes nothing", !menu.getSlot(0).mayPlace(new ItemStack(Items.IRON_ORE)), "");
    }

    public void measure(TestContext ctx) {
        // 26.x ores drop raw chunks; deepslate ores share the ore tag (Baughn: the macerator takes both)
        for (var item : new net.minecraft.world.item.Item[]{Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON, Items.RAW_GOLD,
            Items.RAW_COPPER, Items.COPPER_ORE, Items.DEEPSLATE_GOLD_ORE})
            ctx.check("macerator recipe for " + item, mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(item)) != null, "");
        // ore -> chunks about like Fortune II (mean 1.75), chunk -> dust with a 20% chance of two (mean 1.2)
        java.util.Random rnd = new java.util.Random(1);
        double ore = 0, chunk = 0;
        var ro = mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(Items.IRON_ORE));
        var rc = mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(Items.RAW_IRON));
        for (int i = 0; i < 4000; i++) {
            ore += ro.getOutputCopy(rnd)[0].getCount();
            chunk += rc.getOutputCopy(rnd)[0].getCount();
        }
        ctx.check("iron ore -> raw iron, mean about 1.75", ro.output[0].is(Items.RAW_IRON) && Math.abs(ore / 4000 - 1.75) < 0.05, "mean " + ore / 4000);
        ctx.check("raw iron -> iron dust, mean about 1.2", Math.abs(chunk / 4000 - 1.2) < 0.03, "mean " + chunk / 4000);
        // every chunk: one dust, two 20 % of the time (Baughn asked for a test of exactly this)
        for (var raw : new net.minecraft.world.item.Item[]{Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_COPPER,
            mods.eln.registry.ElnRegistry.RAW_LEAD.get(), mods.eln.registry.ElnRegistry.RAW_TUNGSTEN.get()}) {
            var r = mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(raw));
            int n = 20000, two = 0, other = 0;
            for (int i = 0; i < n; i++) {
                int c = r.getOutputCopy(rnd)[0].getCount();
                if (c == 2) two++;
                else if (c != 1) other++;
            }
            double share = two / (double) n;
            ctx.check("macerating " + raw + ": 1 dust, 2 dust 20% of the time", other == 0 && Math.abs(share - 0.2) < 0.012,
                String.format("2 dust %.1f%%, other counts %d", 100 * share, other));
        }
        // every other ore block as a Fortune II pickaxe gets it (Baughn): its loot table, rolled per operation
        Object[][] fortune = {{Items.DIAMOND_ORE, Items.DIAMOND, 1, 3, 1.75}, {Items.DEEPSLATE_COAL_ORE, Items.COAL, 1, 3, 1.75},
            {Items.REDSTONE_ORE, Items.REDSTONE, 4, 7, 5.5}, {Items.NETHER_QUARTZ_ORE, Items.QUARTZ, 1, 3, 1.75},
            {Items.EMERALD_ORE, Items.EMERALD, 1, 3, 1.75}, {Items.LAPIS_ORE, Items.LAPIS_LAZULI, 4, 27, 11.375}};
        for (Object[] f : fortune) {
            var item = (net.minecraft.world.item.Item) f[0];
            var r = mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(item));
            if (r == null) {
                ctx.check("macerator recipe for " + item + " (Fortune II loot)", false, "none");
                continue;
            }
            int n = 2000, lo = Integer.MAX_VALUE, hi = 0;
            double sum = 0;
            boolean only = true;
            for (int i = 0; i < n; i++) {
                int c = 0;
                for (ItemStack o : r.getOutputCopy(rnd)) {
                    if (o.is((net.minecraft.world.item.Item) f[1])) c += o.getCount();
                    else only = false;
                }
                lo = Math.min(lo, c);
                hi = Math.max(hi, c);
                sum += c;
            }
            double mean = sum / n, want = (double) f[4];
            ctx.check("macerating " + item + " as a Fortune II pickaxe: " + f[2] + ".." + f[3] + " " + f[1] + ", mean about " + want,
                only && lo >= (int) f[2] && hi <= (int) f[3] && Math.abs(mean - want) < 0.08 * want,
                String.format("%d..%d, mean %.2f%s", lo, hi, mean, only ? "" : ", other items"));
        }
        ctx.check("macerator: no recipe for ancient debris (it drops itself)",
            mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(Items.ANCIENT_DEBRIS)) == null, "");
        ctx.check("macerator: iron ore still in the input after 5 ticks",
            node.element.getInventory().getItem(4).is(Items.IRON_ORE), "input holds " + node.element.getInventory().getItem(4));
    }
}
