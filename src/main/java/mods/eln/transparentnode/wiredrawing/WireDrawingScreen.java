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
 * the copper and rubber it has (its slots plus what it already took from them), its state, and Start (one item a press).
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
        mode = mode == WireDrawingElement.WINDING || mode == WireDrawingElement.BARE || mode == WireDrawingElement.OVERHEAD || pole ? mode : WireDrawingElement.KIT;
        int max = WireDrawingDescriptor.maxAmount(mode);
        render.clientSet(mode, Math.max(0, Math.min(d.maxMm2, mm2)), CableSpec.clampTier(tier, pole ? 3 : d.maxTier), (colour + 16) % 16,
            Math.max(1, Math.min(max, amount)));
    }

    /** What each mode uses (Baughn 2026-11-08: rows a mode doesn't use aren't shown). */
    boolean usesWire() {
        return render.mode != WireDrawingElement.BARE;
    }

    /** Insulation: not for bare wire or overhead cable (air insulates it). */
    boolean usesInsulation() {
        return render.mode != WireDrawingElement.BARE && render.mode != WireDrawingElement.OVERHEAD;
    }

    boolean usesColour() {
        // (not for a twisted jacket: the 1 kV cable's colours are its own)
        return (render.mode == WireDrawingElement.KIT || render.mode == WireDrawingElement.WINDING) && !CableSpec.twisted(render.tier);
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

    @Override
    protected void init() {
        super.init();
        WireDrawingRender r = render;
        progress("work", () -> r.progress);
        text("makel", () -> tr("Make"));
        boolean pole = r.desc.maxTier >= 4;
        // the drop-down's entries and the mode each stands for (the pole transformer only on the 230 V machine)
        String[] modes = pole ? new String[]{tr("Cable kit"), tr("Winding"), tr("Bare wire"), tr("Pole transformer"), tr("Overhead cable")}
            : new String[]{tr("Cable kit"), tr("Winding"), tr("Bare wire"), tr("Overhead cable")};
        int[] modeOf = pole ? new int[]{WireDrawingElement.KIT, WireDrawingElement.WINDING, WireDrawingElement.BARE, WireDrawingElement.POLE, WireDrawingElement.OVERHEAD}
            : new int[]{WireDrawingElement.KIT, WireDrawingElement.WINDING, WireDrawingElement.BARE, WireDrawingElement.OVERHEAD};
        locked(dropdown("mode", modes, () -> {
                for (int i = 0; i < modeOf.length; i++) if (modeOf[i] == r.mode) return i;
                return 0;
            }, i -> {
                int m = modeOf[Math.max(0, Math.min(modeOf.length - 1, i))];
                set(m, r.mm2, r.tier, r.colour, m == WireDrawingElement.KIT ? Math.max(r.amount, 20)
                    : m == WireDrawingElement.OVERHEAD ? Math.max(r.amount, 100) : Math.min(r.amount, WireDrawingDescriptor.MAX_BLOCKS));
            }, tr("Output item type")));
        java.util.function.BooleanSupplier wire = this::usesWire, ins = this::usesInsulation, colour = this::usesColour, amount = this::usesAmount;
        java.util.function.BooleanSupplier poleMode = () -> r.mode == WireDrawingElement.POLE;
        text("mm2l", shown(wire, () -> poleMode.getAsBoolean() ? tr("Low-side wire") : tr("Wire")));
        when(wire, locked(button("mm2dn", () -> "<", () -> set(r.mode, r.mm2 - 1, r.tier, r.colour, r.amount))));
        text("mm2", shown(wire, () -> CableSpec.mm2Number(CableSpec.MM2[r.mm2]) + " mm², " + String.format("%.0f A", CableSpec.AMPS[r.mm2])));
        when(wire, locked(button("mm2up", () -> ">", () -> set(r.mode, r.mm2 + 1, r.tier, r.colour, r.amount),
            tr("Max %1$ mm²", CableSpec.mm2Number(CableSpec.MM2[r.desc.maxMm2])))));
        text("insl", shown(ins, () -> poleMode.getAsBoolean() ? tr("Low side") : tr("Insulation")));
        when(ins, locked(button("insdn", () -> "<", () -> set(r.mode, r.mm2, CableSpec.stepTier(r.tier, -1, 4), r.colour, r.amount))));
        text("ins", shown(ins, () -> CableSpec.voltsLabel(r.tier)));
        when(ins, locked(button("insup", () -> ">", () -> set(r.mode, r.mm2, CableSpec.stepTier(r.tier, 1, 4), r.colour, r.amount),
            tr("Max %1$", CableSpec.voltsLabel(r.desc.maxTier)))));
        text("coll", shown(colour, () -> tr("Colour")));
        when(colour, locked(button("coldn", () -> "<", () -> set(r.mode, r.mm2, r.tier, r.colour - 1, r.amount))));
        text("col", shown(colour, () -> DyeColor.byId(r.colour).getName().replace('_', ' ')));
        when(colour, locked(button("colup", () -> ">", () -> set(r.mode, r.mm2, r.tier, r.colour + 1, r.amount), tr("Cosmetic only"))));
        text("amountl", shown(amount, () -> switch (r.mode) {
            case WireDrawingElement.KIT, WireDrawingElement.OVERHEAD -> tr("Length");
            case WireDrawingElement.WINDING -> tr("Turns");
            default -> tr("Count");
        }));
        when(amount, locked(field("amount", () -> String.valueOf(r.amount), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) set(r.mode, r.mm2, r.tier, r.colour, Math.round(f));
        }, tr("Quantity / length"))));
        text("amountu", shown(amount, () -> switch (r.mode) {
            case WireDrawingElement.KIT, WireDrawingElement.OVERHEAD -> tr("blocks");
            case WireDrawingElement.WINDING -> tr("turns");
            default -> tr("cables");
        }));
        text("cost", () -> {
            double l = WireDrawingDescriptor.length(r.mode, r.amount);
            return tr(r.mode == WireDrawingElement.POLE ? "Takes %1$ copper, %2$ rubber, a core, %3$ kJ" : "Takes %1$ copper, %2$ rubber, %3$ kJ", String.format("%.1f", l * WireDrawingDescriptor.copperPerBlock(r.mode, r.mm2)),
                String.format("%.1f", l * WireDrawingDescriptor.rubberPerBlock(r.mode, r.tier, r.mm2)),
                String.format("%.0f", l * WireDrawingDescriptor.energyPerBlock(r.mode, r.mm2) / 1000));
        });
        text("buffer", () -> {
            var cu = menu.getSlot(WireDrawingElement.COPPER).getItem();
            var ru = menu.getSlot(WireDrawingElement.RUBBER).getItem();
            double copper = r.copper + (WireDrawingElement.isCopper(cu) ? cu.getCount() : 0);
            double rubber = r.rubber + (WireDrawingElement.isRubber(ru) ? ru.getCount() : 0);
            return tr("Has %1$ copper, %2$ rubber", String.format("%.1f", Math.floor(copper * 10 + 1e-9) / 10), String.format("%.1f", Math.floor(rubber * 10 + 1e-9) / 10));
        });
        text("status", () -> WireDrawingElement.statusText(r.status), () -> switch (r.status) {
            case WireDrawingElement.NO_COPPER, WireDrawingElement.NO_RUBBER, WireDrawingElement.NO_POWER, WireDrawingElement.NO_CORE -> ERROR_TEXT;
            case WireDrawingElement.RUNNING -> OK_TEXT;
            default -> LABEL;
        });
        var run = button("run", () -> r.running ? tr("Busy") : tr("Start"), () -> {
            if (!r.running) r.clientRun(true);
        }, tr("Changing settings discards progress"));
        refreshers.add(() -> {
            run.active = !r.running;
            for (var w : lockedWhileDrawing) w.active = !r.running;
            for (var e : shownWhen) e.getKey().visible = e.getValue().getAsBoolean();
        });
    }
}
