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
}
