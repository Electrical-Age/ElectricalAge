package mods.eln.gametest;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;

import java.util.IdentityHashMap;
import java.util.Map;

/** R5: every device item with a 3D draw records vertices held and dropped (no exception), with its cost. */
final class ItemDrawCase implements TestCase {
    public String id() {
        return "item_draw";
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
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        int ok = 0, all = 0;
        java.util.List<GenericItemBlockUsingDamageDescriptor> ds = new java.util.ArrayList<>();
        ds.addAll(Eln.sixNodeItem.all().values());
        ds.addAll(Eln.transparentNodeItem.all().values());
        for (GenericItemBlockUsingDamageDescriptor d : ds) {
            if (!d.hasItemDraw() || seen.put(d, true) != null) continue;
            all++;
            boolean a = PublishRenderCase.draw(ctx, d.name + " (item, held)", () -> d.drawItem(false));
            boolean b = PublishRenderCase.draw(ctx, d.name + " (item, dropped)", () -> d.drawItem(true));
            if (a && b) ok++;
        }
        ctx.check("item_draw: devices", ok == all && all > 50, ok + " of " + all);
        // item descriptors with a live look: the data-log print (empty log: the sheet alone)
        var print = mods.eln.registry.batch.Wp9cContent.dataLogsPrintDescriptor;
        var stack = print.newItemStack(1);
        print.initializeStack(stack, new mods.eln.sixnode.electricaldatalogger.DataLogs(256));
        PublishRenderCase.draw(ctx, "Data logger print (item, held)", () -> print.drawItem(stack, false));
    }
}
