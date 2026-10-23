package mods.eln.gametest;

import io.netty.buffer.Unpooled;
import mods.eln.Eln;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeMenus;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Every device with a screen opens it (P4b): a right click on the placed block (the real block use path, as a player)
 * leaves the player in an eln:node menu, and its open data rebuilds the same device slots on the client side.
 */
final class MenusOpenCase implements TestCase {
    public String id() {
        return "menus_open";
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

    /** A click toggles these; their screen opens with the wrench key held (GuiPacketCase). */
    static final java.util.Set<String> CLICK_TOGGLES = java.util.Set.of("Wireless Button", "Wireless Switch");

    public void measure(TestContext ctx) {
        FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.level());
        NodeMenus.openedHook = (pl, m) -> opened = m;
        try {
            run(ctx, fp);
        } finally {
            NodeMenus.openedHook = null;
        }
    }

    static ElnNodeMenu opened;

    void run(TestContext ctx, FakePlayer fp) {
        BlockPos p = ctx.at(4, 1, 1);
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        int ok = 0, all = 0;
        for (var e : Eln.sixNodeItem.all().entrySet()) {
            SixNodeDescriptor d = e.getValue();
            if (seen.put(d, true) != null) continue;
            NodeBase node;
            try {
                node = ctx.placeSix(e.getKey(), p, Direction.YN).sixNode;
            } catch (RuntimeException ex) {
                GoldenSlotsCase.clear(ctx);
                continue; // wall/ceiling-only devices: their screens are covered by the transparent pass's logic
            }
            if (node.hasGui(Direction.YN) && !CLICK_TOGGLES.contains(d.name)) {
                all++;
                if (opens(ctx, fp, d.name, node, p, net.minecraft.core.Direction.UP, new Vec3(0.5, 0.05, 0.5))) ok++;
            }
            GoldenSlotsCase.clear(ctx);
        }
        for (var e : Eln.transparentNodeItem.all().entrySet()) {
            TransparentNodeDescriptor d = e.getValue();
            if (seen.put(d, true) != null || PublishRenderCase.NOT_HERE.contains(d.name)) continue;
            TransparentNode node;
            try {
                ctx.floor(p.below());
                node = ctx.placeTransparent(e.getKey(), p).node;
            } catch (RuntimeException ex) {
                GoldenSlotsCase.clear(ctx);
                continue;
            }
            if (node.hasGui(Direction.XN)) {
                all++;
                BlockPos at = node.coordonate.pos();
                if (opens(ctx, fp, d.name, node, at, net.minecraft.core.Direction.WEST, new Vec3(0.0, 0.5, 0.5))) ok++;
            }
            GoldenSlotsCase.clear(ctx);
        }
        ctx.check("menus_open: devices", ok == all, ok + " of " + all);
    }

    static boolean opens(TestContext ctx, FakePlayer fp, String name, NodeBase node, BlockPos at,
                         net.minecraft.core.Direction face, Vec3 local) {
        try {
            opened = null;
            Vec3 hitAt = new Vec3(at.getX() + local.x, at.getY() + local.y, at.getZ() + local.z);
            fp.setPos(hitAt.x - 1.5, at.getY(), hitAt.z);
            var state = ctx.level().getBlockState(at);
            var result = state.useWithoutItem(ctx.level(), fp, new BlockHitResult(hitAt, face, at, false));
            ElnNodeMenu menu = opened;
            if (menu == null) {
                ctx.check("menu " + name + " opens", false, "use gave " + result);
                return false;
            }
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), ctx.level().registryAccess());
            menu.writeOpenData(buf);
            ElnNodeMenu client = ElnNodeMenu.fromNetwork(1, fp.getInventory(), buf);
            boolean same = client.slots.size() == menu.slots.size()
                && NodeMenus.descriptor(client.kind, client.descriptorId) != null;
            ctx.check("menu " + name + " rebuilt on the client", same, client.slots.size() + " slots vs " + menu.slots.size());
            return same;
        } catch (RuntimeException ex) {
            ctx.check("menu " + name + " opens", false, ex.toString());
            return false;
        }
    }
}
