package mods.eln.transparentnode.wiredrawing;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.node.voxel.CableSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

import static mods.eln.i18n.I18N.tr;

/**
 * A wire-drawing machine (layout gui/wire_drawing.json): copper and rubber in, the kit or winding out; what to make
 * (kit or winding, cross-section, insulation and colour within the machine's limits, blocks or turns), what it takes,
 * the copper and rubber already in the machine, its state, and Start (one item a press).
 */
public class WireDrawingScreen extends ElnScreen {
    final WireDrawingRender render;

    public WireDrawingScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (WireDrawingRender) render;
    }

    /** The settings' widgets, greyed while it draws: a change would scrap the item in progress (Baughn 2026-11-08). */
    final java.util.List<net.minecraft.client.gui.components.AbstractWidget> lockedWhileDrawing = new java.util.ArrayList<>();

    <W extends net.minecraft.client.gui.components.AbstractWidget> W locked(W w) {
        lockedWhileDrawing.add(w);
        return w;
    }

    void set(int mode, int mm2, int tier, int colour, int amount) {
        if (render.running) return;   // (greyed; and a key into the field mustn't scrap the item either)
        WireDrawingDescriptor d = render.desc;
        boolean pole = mode == WireDrawingElement.POLE && d.maxTier >= 4;
        mode = mode == WireDrawingElement.WINDING || mode == WireDrawingElement.BARE || pole ? mode : WireDrawingElement.KIT;
        int max = WireDrawingDescriptor.maxAmount(mode);
        render.clientSet(mode, Math.max(0, Math.min(d.maxMm2, mm2)), Math.max(1, Math.min(pole ? 3 : d.maxTier, tier)), (colour + 16) % 16,
            Math.max(1, Math.min(max, amount)));
    }

    /** What each mode uses (Baughn 2026-11-08: rows a mode doesn't use aren't shown). */
    boolean usesWire() {
        return render.mode != WireDrawingElement.BARE;
    }

    boolean usesColour() {
        return render.mode == WireDrawingElement.KIT || render.mode == WireDrawingElement.WINDING;
    }

    boolean usesAmount() {
        return render.mode != WireDrawingElement.POLE;
    }

    /** Widgets shown only while their row is used. */
    final java.util.List<java.util.Map.Entry<net.minecraft.client.gui.components.AbstractWidget, java.util.function.BooleanSupplier>> shownWhen =
        new java.util.ArrayList<>();

    <W extends net.minecraft.client.gui.components.AbstractWidget> W when(java.util.function.BooleanSupplier used, W w) {
        shownWhen.add(java.util.Map.entry(w, used));
        return w;
    }

    static java.util.function.Supplier<String> shown(java.util.function.BooleanSupplier used, java.util.function.Supplier<String> s) {
        return () -> used.getAsBoolean() ? s.get() : "";
    }

    static String volts(int tier) {
        double v = CableSpec.volts(tier);
        return v >= 1000 ? (int) (v / 1000) + " kV" : (int) v + " V";
    }

    @Override
    protected void init() {
        super.init();
        WireDrawingRender r = render;
        progress("work", () -> r.progress);
        text("makel", () -> tr("Make"));
        boolean pole = r.desc.maxTier >= 4;
        String[] modes = pole ? new String[]{tr("Cable kit"), tr("Winding"), tr("Bare wire"), tr("Pole transformer")}
            : new String[]{tr("Cable kit"), tr("Winding"), tr("Bare wire")};
        locked(dropdown("mode", modes, () -> r.mode, m -> set(m, r.mm2, r.tier, r.colour,
                m == WireDrawingElement.KIT ? Math.max(r.amount, 20) : Math.min(r.amount, WireDrawingDescriptor.MAX_BLOCKS)),
            pole ? tr("A cable kit (blocks of cable), a winding for a DC-DC converter (turns), bare Copper Cable for crafting,\nor a transformer for a utility pole (it takes a ferromagnetic core)")
                : tr("A cable kit (blocks of cable), a winding for a DC-DC converter (turns), or bare Copper Cable for crafting")));
        java.util.function.BooleanSupplier wire = this::usesWire, colour = this::usesColour, amount = this::usesAmount;
        java.util.function.BooleanSupplier poleMode = () -> r.mode == WireDrawingElement.POLE;
        text("mm2l", shown(wire, () -> poleMode.getAsBoolean() ? tr("Low-side wire") : tr("Wire")));
        when(wire, locked(button("mm2dn", () -> "<", () -> set(r.mode, r.mm2 - 1, r.tier, r.colour, r.amount))));
        text("mm2", shown(wire, () -> WireDrawingDescriptor.fmt(CableSpec.MM2[r.mm2]) + " mm², " + String.format("%.0f A", CableSpec.AMPS[r.mm2])));
        when(wire, locked(button("mm2up", () -> ">", () -> set(r.mode, r.mm2 + 1, r.tier, r.colour, r.amount),
            tr("Up to %1$ mm² on this machine", WireDrawingDescriptor.fmt(CableSpec.MM2[r.desc.maxMm2])))));
        text("insl", shown(wire, () -> poleMode.getAsBoolean() ? tr("Low side") : tr("Insulation")));
        when(wire, locked(button("insdn", () -> "<", () -> set(r.mode, r.mm2, r.tier - 1, r.colour, r.amount))));
        text("ins", shown(wire, () -> volts(r.tier)));
        when(wire, locked(button("insup", () -> ">", () -> set(r.mode, r.mm2, r.tier + 1, r.colour, r.amount),
            tr("Up to %1$ on this machine (pole transformers: 48, 230 or 400 V on the low side)", volts(r.desc.maxTier)))));
        text("coll", shown(colour, () -> tr("Colour")));
        when(colour, locked(button("coldn", () -> "<", () -> set(r.mode, r.mm2, r.tier, r.colour - 1, r.amount))));
        text("col", shown(colour, () -> DyeColor.byId(r.colour).getName().replace('_', ' ')));
        when(colour, locked(button("colup", () -> ">", () -> set(r.mode, r.mm2, r.tier, r.colour + 1, r.amount), tr("Only the look"))));
        text("amountl", shown(amount, () -> switch (r.mode) {
            case WireDrawingElement.KIT -> tr("Length");
            case WireDrawingElement.WINDING -> tr("Turns");
            default -> tr("Count");
        }));
        when(amount, locked(field("amount", () -> String.valueOf(r.amount), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) set(r.mode, r.mm2, r.tier, r.colour, Math.round(f));
        }, tr("Kits: 1 to 64 blocks. Windings: 1 to 1000 turns. Bare wire: 1 to 64 Copper Cable"))));
        text("amountu", shown(amount, () -> switch (r.mode) {
            case WireDrawingElement.KIT -> tr("blocks");
            case WireDrawingElement.WINDING -> tr("turns");
            default -> tr("cables");
        }));
        text("cost", () -> {
            double l = WireDrawingDescriptor.length(r.mode, r.amount);
            return tr(r.mode == WireDrawingElement.POLE ? "Takes %1$ copper, %2$ rubber, a core, %3$ kJ" : "Takes %1$ copper, %2$ rubber, %3$ kJ", String.format("%.1f", l * WireDrawingDescriptor.copperPerBlock(r.mode, r.mm2)),
                String.format("%.1f", l * WireDrawingDescriptor.rubberPerBlock(r.mode, r.tier, r.mm2)),
                String.format("%.0f", l * WireDrawingDescriptor.energyPerBlock(r.mode, r.mm2) / 1000));
        });
        text("buffer", () -> tr("Held: %1$ copper, %2$ rubber", String.format("%.2f", r.copper), String.format("%.2f", r.rubber)));
        text("status", () -> WireDrawingElement.statusText(r.status), () -> switch (r.status) {
            case WireDrawingElement.NO_COPPER, WireDrawingElement.NO_RUBBER, WireDrawingElement.NO_POWER, WireDrawingElement.NO_CORE -> 0xFFA02020;
            case WireDrawingElement.RUNNING -> 0xFF206020;
            default -> LABEL;
        });
        // one item a Start, no Stop (Baughn 2026-11-07: twice he made a second one he didn't mean to); greyed while drawing
        var run = button("run", () -> r.running ? tr("Drawing...") : tr("Start"), () -> {
            if (!r.running) r.clientRun(true);
        }, tr("Draws one item, then stops. Changing what to make scraps the wire drawn so far"));
        refreshers.add(() -> {
            run.active = !r.running;
            for (var w : lockedWhileDrawing) w.active = !r.running;
            for (var e : shownWhen) e.getKey().visible = e.getValue().getAsBoolean();
        });
    }
}
