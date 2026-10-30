package mods.eln.gametest.cases;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.IDescriptorItem;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.registry.ElnRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Item tooltips (client ItemTooltips, 26.x): every descriptor's 1.12 addInformation runs without a player (as for a
 * tooltip outside the world) without throwing, and the Scanner explains itself.
 */
public final class TooltipCases {
    private TooltipCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TooltipCase());
        cases.add(new ReadOnlyCase());
    }

    static final class TooltipCase implements TestCase {
        public String name() {
            return "item tooltips";
        }

        public int width() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        static List<String> lines(ItemStack stack) {
            List<String> l = new ArrayList<>();
            Object d = ((IDescriptorItem) stack.getItem()).getEaDescriptor();
            if (d instanceof GenericItemUsingDamageDescriptor g) g.addInformation(stack, null, l, false);
            else if (d instanceof GenericItemBlockUsingDamageDescriptor b) b.addInformation(stack, null, l, false);
            return l;
        }

        public void measure(TestContext ctx) {
            List<String> failed = new ArrayList<>();
            int items = 0, withText = 0;
            for (var h : ElnRegistry.ALL_ITEMS) {
                if (!(h.get() instanceof IDescriptorItem)) continue;
                items++;
                ItemStack s = new ItemStack(h.get());
                try {
                    if (!lines(s).isEmpty()) withText++;
                } catch (RuntimeException e) {
                    failed.add(s.getHoverName().getString() + ": " + e);
                }
            }
            ctx.line(String.format("tooltips: %d of %d descriptor items have text", withText, items));
            ctx.check("tooltips: addInformation never throws", failed.isEmpty(), String.join("; ", failed));
            List<String> scanner = lines(mods.eln.Eln.findItemStack("Scanner", 1));
            ctx.check("tooltips: Scanner explains itself", scanner.size() >= 3, String.join(" | ", scanner));
        }
    }

    /**
     * Drawing an item must not change it: the client reads stacks for the item bar, the name and the tooltip, and a
     * component added there makes its copy differ from the server's. A crafted Portable Battery (no tag) got its default
     * tag that way and left a ghost battery on the cursor when put away (Baughn).
     */
    static final class ReadOnlyCase implements TestCase {
        public String name() {
            return "item reads keep the stack";
        }

        public int width() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        /** What the client does to draw a stack in a slot and its tooltip; returns whether the stack is unchanged. */
        static boolean readKeeps(ItemStack s) {
            ItemStack before = s.copy();
            s.getHoverName();
            TooltipCase.lines(s);
            if (s.getItem().isBarVisible(s)) {
                s.getItem().getBarWidth(s);
                s.getItem().getBarColor(s);
            }
            return ItemStack.isSameItemSameComponents(before, s);
        }

        public void measure(TestContext ctx) {
            List<String> changed = new ArrayList<>();
            for (var h : ElnRegistry.ALL_ITEMS) {
                if (!(h.get() instanceof IDescriptorItem)) continue;
                ItemStack s = new ItemStack(h.get());
                try {
                    if (!readKeeps(s)) changed.add(s.getHoverName().getString());
                } catch (RuntimeException e) {
                    changed.add(s.getHoverName().getString() + ": " + e);
                }
            }
            ctx.check("item reads: drawing a stack never changes its components", changed.isEmpty(), String.join("; ", changed));
            ItemStack bat = new ItemStack(mods.eln.Eln.itemsByName.get("Portable Battery"));
            boolean kept = readKeeps(bat);
            ctx.check("item reads: a crafted Portable Battery stays as crafted when drawn", kept && !mods.eln.generic.ItemNbt.has(bat),
                String.valueOf(mods.eln.generic.ItemNbt.get(bat)));
        }
    }
}
