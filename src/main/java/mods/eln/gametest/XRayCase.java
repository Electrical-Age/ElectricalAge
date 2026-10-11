package mods.eln.gametest;

import mods.eln.generic.ElnItem;
import mods.eln.item.xray.XRayFilter;
import mods.eln.item.xray.XRayScannerDescriptor;
import mods.eln.item.xray.XRayScannerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * X-ray scanner (recreated): filters by tags (ore block or raw chunk -> every ore block of that material), the
 * on/off state machine and its energy, the filter menu writing the scanner stack, and the item draw without a picture.
 */
final class XRayCase implements TestCase {
    public String id() {
        return "xray_scanner";
    }

    public int width() {
        return 9;
    }

    public int height() {
        return 7;
    }

    public int depth() {
        return 7;
    }

    public int ticks() {
        return 1;
    }

    public void build(TestContext ctx) {
    }

    public void measure(TestContext ctx) {
        ctx.check("xray: raw iron is iron", XRayFilter.materials(new ItemStack(Items.RAW_IRON)).contains("iron"),
            String.valueOf(XRayFilter.materials(new ItemStack(Items.RAW_IRON))));
        ctx.check("xray: deepslate gold ore is gold", XRayFilter.materials(new ItemStack(Items.DEEPSLATE_GOLD_ORE)).contains("gold"), "");
        ctx.check("xray: stone and an iron ingot are no filters",
            !XRayFilter.accepts(new ItemStack(Items.STONE)) && !XRayFilter.accepts(new ItemStack(Items.IRON_INGOT)), "");
        var slots = XRayFilter.blockSlots(List.of(ItemStack.EMPTY, new ItemStack(Items.RAW_IRON), ItemStack.EMPTY, ItemStack.EMPTY));
        ctx.check("xray: raw iron in slot 1 finds iron ore and deepslate iron ore",
            Integer.valueOf(1).equals(slots.get(Blocks.IRON_ORE)) && Integer.valueOf(1).equals(slots.get(Blocks.DEEPSLATE_IRON_ORE))
                && !slots.containsKey(Blocks.GOLD_ORE) && !slots.containsKey(Blocks.STONE), slots.size() + " blocks");
        var leadOre = mods.eln.Eln.itemsByName.get("Lead Ore");
        if (leadOre != null) {
            var lead = XRayFilter.blockSlots(List.of(new ItemStack(leadOre)));
            ctx.check("xray: EA lead ore finds itself", lead.values().contains(0) && !lead.isEmpty(), lead.keySet().toString());
        }

        ItemStack s = new ItemStack(mods.eln.Eln.itemsByName.get("X-Ray Scanner"));
        ctx.check("xray: the item exists", s.getItem() instanceof ElnItem i && i.descriptor instanceof XRayScannerDescriptor, s.toString());
        if (!(s.getItem() instanceof ElnItem i && i.descriptor instanceof XRayScannerDescriptor d)) return;
        s = d.newItemStack();
        var fp = FakePlayerFactory.getMinecraft(ctx.level());
        d.setEnergy(s, 5000);
        d.onItemRightClick(s, ctx.level(), fp);
        ctx.check("xray: use boots it", d.getState(s) == XRayScannerDescriptor.State.Boot, String.valueOf(d.getState(s)));
        for (int t = 0; t < 80; t++) d.onUpdate(s, ctx.level(), fp, 0, true);
        ctx.check("xray: running after 4 s", d.getState(s) == XRayScannerDescriptor.State.Run, String.valueOf(d.getState(s)));
        d.electricalItemUpdate(s, 1.0);
        ctx.checkValue("xray: draws 300 W", d.getEnergy(s), 4700);
        ctx.check("xray: has an id", XRayScannerDescriptor.id(s) != 0, "");
        ctx.check("xray: draws without a picture (server)", PublishRenderCase.draw(ctx, "X-Ray Scanner (item, running)",
            () -> d.drawItem(d.newItemStack(), false)), "");
        d.electricalItemUpdate(s, 100.0);
        ctx.check("xray: flat battery switches it off", d.getState(s) == XRayScannerDescriptor.State.Idle && d.getEnergy(s) == 0, "");

        // the filter menu writes the scanner stack
        fp.getInventory().setItem(0, s);
        XRayScannerMenu menu = new XRayScannerMenu(1, fp.getInventory(), 0);
        ctx.check("xray: a filter slot refuses stone", !menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)), "");
        menu.getSlot(2).set(new ItemStack(Items.RAW_GOLD));
        var f = XRayFilter.filters(fp.getInventory().getItem(0));
        ctx.check("xray: raw gold stored in filter 2", f.get(2).is(Items.RAW_GOLD) && f.get(0).isEmpty(), f.toString());
        ctx.check("xray: the scanner can't be taken out while open", !menu.getSlot(XRayScannerMenu.FILTERS + 27).mayPickup(fp), "");
        fp.getInventory().setItem(0, ItemStack.EMPTY);
    }
}
