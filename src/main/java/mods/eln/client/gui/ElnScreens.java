package mods.eln.client.gui;

import mods.eln.Eln;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Which screen opens for an EA menu (P4b). Screens register per descriptor class; a device without one gets the generic
 * screen (its slots and the player inventory), so every container works before its screen is ported.
 */
public final class ElnScreens {
    private ElnScreens() {
    }

    public interface Factory {
        ElnScreen create(ElnNodeMenu menu, Inventory inventory, Component title, Object render);
    }

    private static final Map<Class<?>, Factory> BY_DESCRIPTOR = new HashMap<>();

    public static void register(Class<?> descriptor, Factory factory) {
        BY_DESCRIPTOR.put(descriptor, factory);
    }

    /** The descriptor of the menu's device (from the open data), or null. */
    public static Object descriptor(ElnNodeMenu menu) {
        return switch (menu.kind) {
            case ElnNodeMenu.SIX -> Eln.sixNodeItem.getDescriptor(menu.descriptorId);
            case ElnNodeMenu.TRANSPARENT -> Eln.transparentNodeItem.getDescriptor(menu.descriptorId);
            default -> null;
        };
    }

    /** The client element render of the menu's device: its live values and the packets back to the server. */
    static Object render(ElnNodeMenu menu) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(menu.pos);
        if (be instanceof SixNodeEntity six) return six.elementRenderList[menu.slot];
        if (be instanceof TransparentNodeEntity t) return t.elementRender;
        return null;
    }

    /** MenuScreens constructor for the eln:node menu type. */
    public static ElnScreen create(ElnNodeMenu menu, Inventory inventory, Component title) {
        Object d = descriptor(menu);
        Object render = render(menu);
        Component name = d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor gd ? Component.literal(gd.name) : title;
        if (d != null && render != null) {
            for (Class<?> c = d.getClass(); c != null; c = c.getSuperclass()) {
                Factory f = BY_DESCRIPTOR.get(c);
                if (f != null) return f.create(menu, inventory, name, render);
            }
        }
        return new ElnScreen(menu, inventory, name);
    }
}
