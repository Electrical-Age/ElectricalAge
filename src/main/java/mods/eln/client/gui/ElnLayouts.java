package mods.eln.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.Eln;
import mods.eln.gui.MenuSpec;
import mods.eln.gui.SlotSpec;
import mods.eln.gui.layout.Layout;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen layouts (P4b): descriptor class -> assets/eln/gui/&lt;name&gt;.json, laid out with the client font. Read
 * fresh from the resource manager each time a screen opens, so a resource pack (or a reload) changes them live.
 */
public final class ElnLayouts {
    private ElnLayouts() {
    }

    static final Map<Class<?>, String> NAMES = new HashMap<>();

    public static void register(Class<?> descriptor, String name) {
        NAMES.put(descriptor, name);
    }

    static String name(Object descriptor) {
        if (descriptor == null) return null;
        for (Class<?> c = descriptor.getClass(); c != null; c = c.getSuperclass()) {
            String n = NAMES.get(c);
            if (n != null) return n;
        }
        return null;
    }

    static JsonObject json(String name) {
        var rm = Minecraft.getInstance().getResourceManager();
        var res = rm.getResource(Identifier.fromNamespaceAndPath(Eln.MODID, "gui/" + name + ".json"));
        if (res.isEmpty()) return null;
        try (Reader r = res.get().openAsReader()) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            Eln.LOG.error("EA screen layout {} unreadable", name, e);
            return null;
        }
    }

    static final Layout.Measure FONT = s -> Minecraft.getInstance().font.width(s);

    /** The laid-out screen of a device, or null (no layout: the screen places itself). */
    public static Layout.Result layout(Object descriptor) {
        String n = name(descriptor);
        if (n == null) return null;
        JsonObject o = json(n);
        if (o == null) return null;
        Layout.Result r = Layout.layout(o, FONT);
        if (!r.problems().isEmpty()) Eln.LOG.warn("EA screen layout {}: {}", n, r.problems());
        return r;
    }

    /** Slot and inventory positions and the panel size from the layout (the client menu's spec). */
    public static MenuSpec apply(MenuSpec spec, Object descriptor) {
        Layout.Result r = layout(descriptor);
        if (r == null) return spec;
        List<SlotSpec> slots = new ArrayList<>();
        for (SlotSpec s : spec.slots()) {
            Layout.Rect rect = r.get("slot", String.valueOf(s.index()));
            if (rect == null) {
                slots.add(s);
                continue;
            }
            int inset = rect.w() > 18 ? 5 : 1;
            slots.add(new SlotSpec(s.kind(), s.index(), rect.x() + inset, rect.y() + inset, s.limit(), s.filter(),
                rect.w() > 18 ? SlotSpec.BIG : s.skin(), s.comment()));
        }
        Layout.Rect inv = r.get("inventory", "");
        return new MenuSpec(slots, spec.playerInventory(), spec.refreshRateDivider(),
            inv != null ? inv.x() + 1 : spec.inventoryX(), inv != null ? inv.y() + 1 : spec.inventoryY(), r.width(), r.height());
    }
}
