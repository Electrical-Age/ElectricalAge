package mods.eln.node;

import mods.eln.Eln;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.gui.MenuSpec;
import mods.eln.gui.MenuSpecs;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Opening a node's GUI (P4). 1.12: player.openGui(Eln, GuiHandler.nodeBaseOpen + side, ...) with GuiHandler building
 * the Container/GuiScreen. 26.x: one menu type, {@link ElnNodeMenu}, whose device slots come from the descriptor's
 * {@link MenuSpecs} entry.
 */
public final class NodeMenus {
    private NodeMenus() {
    }

    /** The device descriptor for open data, or null (simple nodes). */
    public static Object descriptor(byte kind, int descriptorId) {
        return switch (kind) {
            case ElnNodeMenu.SIX -> Eln.sixNodeItem.getDescriptor(descriptorId);
            case ElnNodeMenu.TRANSPARENT -> Eln.transparentNodeItem.getDescriptor(descriptorId);
            default -> null;
        };
    }

    /** The menu spec for open data (both sides: no node needed). */
    public static MenuSpec spec(byte kind, int descriptorId, String simpleKey) {
        Object d = switch (kind) {
            case ElnNodeMenu.SIX -> Eln.sixNodeItem.getDescriptor(descriptorId);
            case ElnNodeMenu.TRANSPARENT -> Eln.transparentNodeItem.getDescriptor(descriptorId);
            default -> null;
        };
        return d == null ? MenuSpec.SCREEN_ONLY : MenuSpecs.of(d);
    }

    /** The server-side menu of a node's GUI at a side, or null if there is none. */
    public static @Nullable ElnNodeMenu menu(int containerId, Player player, NodeBase node, Direction side) {
        byte kind;
        int id = 0;
        String key = "";
        Object descriptor;
        Container inventory;
        if (node instanceof SixNode six) {
            SixNodeElement e = six.getElement(side);
            if (e == null) return null;
            kind = ElnNodeMenu.SIX;
            descriptor = e.sixNodeElementDescriptor;
            id = e.sixNodeElementDescriptor.parentItemDamage;
            inventory = e.getInventory();
        } else if (node instanceof TransparentNode t) {
            if (t.element == null) return null;
            kind = ElnNodeMenu.TRANSPARENT;
            descriptor = t.element.transparentNodeDescriptor;
            id = t.element.transparentNodeDescriptor.parentItemDamage;
            inventory = t.element.getInventory();
        } else if (node instanceof SimpleNode s) {
            kind = ElnNodeMenu.SIMPLE;
            descriptor = null;
            key = s.getNodeUuid();
            inventory = null;
        } else {
            return null;
        }
        MenuSpec spec = descriptor == null ? MenuSpec.SCREEN_ONLY : MenuSpecs.of(descriptor);
        if (inventory == null || inventory.getContainerSize() < spec.inventorySize())
            inventory = new SimpleContainer(Math.max(1, spec.inventorySize())); // a spec slot past the element inventory: 1.12 would crash
        return new ElnNodeMenu(containerId, player.getInventory(), inventory, spec, node, node.coordonate.pos(), side, kind, id, key);
    }

    /** GameTests: told of every menu opened (a FakePlayer's openMenu does nothing). */
    public static java.util.function.BiConsumer<Player, ElnNodeMenu> openedHook;

    public static boolean open(NodeBase node, Player player, Direction side) {
        if (!(player instanceof ServerPlayer sp)) return true; // the client waits for the server's open packet
        ElnNodeMenu probe = menu(0, player, node, side);
        if (probe == null) return false;
        if (openedHook != null) openedHook.accept(player, probe);
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> menu(id, p, node, side), Component.literal(node.getNodeUuid())),
            probe::writeOpenData);
        return true;
    }
}
