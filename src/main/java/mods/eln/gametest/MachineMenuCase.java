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
    }

    public void measure(TestContext ctx) {
        // 26.x ores drop raw chunks; deepslate ores share the ore tag (Baughn: the macerator takes both)
        for (var item : new net.minecraft.world.item.Item[]{Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON, Items.RAW_GOLD,
            Items.RAW_COPPER, Items.COPPER_ORE, Items.DEEPSLATE_GOLD_ORE})
            ctx.check("macerator recipe for " + item, mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(item)) != null, "");
        ctx.check("macerator: iron ore still in the input after 5 ticks",
            node.element.getInventory().getItem(4).is(Items.IRON_ORE), "input holds " + node.element.getInventory().getItem(4));
    }
}
