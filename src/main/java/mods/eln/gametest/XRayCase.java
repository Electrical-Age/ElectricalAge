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

    mods.eln.sixnode.batterycharger.BatteryChargerElement charger;

    public void build(TestContext ctx) {
        ctx.floor(ctx.at(7, 0, 5));
        charger = (mods.eln.sixnode.batterycharger.BatteryChargerElement) ctx.placeSix(66 << 6, ctx.at(7, 1, 5));   // Weak 50V Battery Charger
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

        // every ore block in the game, and everything it drops when mined, is a filter that finds that block
        // (Baughn 2026-10-24: tungsten ore and coal didn't work; by tags, so other mods' ores work too)
        var level = ctx.level();
        var pos = ctx.at(1, 1, 1);
        var pick = new ItemStack(Items.NETHERITE_PICKAXE);
        List<String> bad = new java.util.ArrayList<>();
        int ores = 0, drops = 0;
        for (var ore : XRayFilter.allOres()) {
            ores++;
            String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(ore).toString();
            if (!XRayFilter.blockSlots(List.of(new ItemStack(ore))).containsKey(ore)) bad.add(name + " (the ore itself)");
            level.setBlock(pos, ore.defaultBlockState(), 3);
            for (ItemStack d : net.minecraft.world.level.block.Block.getDrops(ore.defaultBlockState(), level, pos, null, null, pick)) {
                drops++;
                if (!XRayFilter.blockSlots(List.of(d)).containsKey(ore)) bad.add(name + " drop " + d.getItem());
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        ctx.check("xray: every ore and every ore's drop finds that ore", bad.isEmpty() && ores >= 20,
            ores + " ores, " + drops + " drops; failing: " + bad);
        for (var b : new net.minecraft.world.level.block.Block[]{Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "tungsten_ore"))})
            ctx.check("xray: " + b + " is an ore", XRayFilter.allOres().contains(b), "");
        ctx.check("xray: coal finds coal ore, raw tungsten finds tungsten ore",
            XRayFilter.blockSlots(List.of(new ItemStack(Items.COAL))).containsKey(Blocks.DEEPSLATE_COAL_ORE)
                && XRayFilter.materials(new ItemStack(mods.eln.registry.ElnRegistry.RAW_TUNGSTEN.get())).contains("tungsten"), "");
        ctx.check("xray: a gem with no ore (amethyst) is no filter", !XRayFilter.accepts(new ItemStack(Items.AMETHYST_SHARD)), "");

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
        // with a picture for its id (the client scan's job), the running scanner draws the 48x30 pixels too
        var old = XRayScannerDescriptor.pictures;
        long sid = XRayScannerDescriptor.id(s);
        XRayScannerDescriptor.pictures = q -> q == sid ? new float[XRayScannerDescriptor.RES_W * XRayScannerDescriptor.RES_H * 3] : null;
        try {
            mods.eln.gl.GlFrame.beginRenderFrame();
            var fr = mods.eln.gl.GlFrame.open();
            final ItemStack run = s;
            d.drawItem(run, false);
            mods.eln.gl.GlFrame.close();
            int v = 0;
            for (var b : fr.batches().values()) v += b.vertices();
            ctx.check("xray: a running scanner with a picture draws it", v >= 272 + 4 * 48 * 30, v + " vertices");
        } finally {
            XRayScannerDescriptor.pictures = old;
        }
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

        // a charger fed by right-click keeps the scanner's filters, id and energy (1.12 rebuilt the stack: only energy)
        ItemStack held = d.newItemStack();
        XRayFilter.setFilters(held, List.of(new ItemStack(Items.COAL_ORE), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        d.setEnergy(held, 1234);
        d.onUpdate(held, ctx.level(), fp, 0, false);   // the server gives it its id
        long heldId = XRayScannerDescriptor.id(held);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
        charger.onBlockActivated(fp, mods.eln.misc.Direction.YP, 0.5f, 0.5f, 0.5f);
        ItemStack in = ItemStack.EMPTY;
        for (int k = 0; k < 4; k++) if (!charger.getInventory().getItem(k).isEmpty()) in = charger.getInventory().getItem(k);
        ctx.check("xray: right-click puts the scanner in the charger", !in.isEmpty() && fp.getMainHandItem().isEmpty(), in + ", hand " + fp.getMainHandItem());
        ctx.check("xray: in the charger it keeps its filters, id and energy", !in.isEmpty()
                && XRayFilter.filters(in).get(0).is(Items.COAL_ORE) && heldId != 0 && XRayScannerDescriptor.id(in) == heldId && d.getEnergy(in) == 1234,
            XRayFilter.filters(in) + ", id " + XRayScannerDescriptor.id(in) + " (was " + heldId + "), " + (in.isEmpty() ? 0 : d.getEnergy(in)) + " J");
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }
}
