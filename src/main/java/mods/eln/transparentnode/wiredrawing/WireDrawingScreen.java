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
 * the copper and rubber already in the machine, its state, and Start/Stop.
 */
public class WireDrawingScreen extends ElnScreen {
    final WireDrawingRender render;

    public WireDrawingScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (WireDrawingRender) render;
    }

    void set(int mode, int mm2, int tier, int colour, int amount) {
        WireDrawingDescriptor d = render.desc;
        mode = mode == WireDrawingElement.WINDING || mode == WireDrawingElement.BARE ? mode : WireDrawingElement.KIT;
        int max = WireDrawingDescriptor.maxAmount(mode);
        render.clientSet(mode, Math.max(0, Math.min(d.maxMm2, mm2)), Math.max(1, Math.min(d.maxTier, tier)), (colour + 16) % 16,
            Math.max(1, Math.min(max, amount)));
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
        button("mode", () -> switch (r.mode) {
                case WireDrawingElement.KIT -> tr("Cable kit");
                case WireDrawingElement.WINDING -> tr("Winding");
                default -> tr("Bare wire");
            },
            () -> set((r.mode + 1) % 3, r.mm2, r.tier, r.colour, r.mode == WireDrawingElement.KIT ? Math.max(r.amount, 20) : Math.min(r.amount, WireDrawingDescriptor.MAX_BLOCKS)),
            tr("A cable kit (blocks of cable), a winding for a DC-DC converter (turns), or bare Copper Cable for crafting"));
        text("mm2l", () -> tr("Wire"));
        button("mm2dn", () -> "<", () -> set(r.mode, r.mm2 - 1, r.tier, r.colour, r.amount));
        text("mm2", () -> WireDrawingDescriptor.fmt(CableSpec.MM2[r.mm2]) + " mm², " + String.format("%.0f A", CableSpec.AMPS[r.mm2]));
        button("mm2up", () -> ">", () -> set(r.mode, r.mm2 + 1, r.tier, r.colour, r.amount),
            tr("Up to %1$ mm² on this machine", WireDrawingDescriptor.fmt(CableSpec.MM2[r.desc.maxMm2])));
        text("insl", () -> tr("Insulation"));
        button("insdn", () -> "<", () -> set(r.mode, r.mm2, r.tier - 1, r.colour, r.amount));
        text("ins", () -> volts(r.tier));
        button("insup", () -> ">", () -> set(r.mode, r.mm2, r.tier + 1, r.colour, r.amount),
            tr("Up to %1$ on this machine", volts(r.desc.maxTier)));
        text("coll", () -> tr("Colour"));
        button("coldn", () -> "<", () -> set(r.mode, r.mm2, r.tier, r.colour - 1, r.amount));
        text("col", () -> DyeColor.byId(r.colour).getName().replace('_', ' '));
        button("colup", () -> ">", () -> set(r.mode, r.mm2, r.tier, r.colour + 1, r.amount), tr("Only the look"));
        text("amountl", () -> switch (r.mode) {
            case WireDrawingElement.KIT -> tr("Length");
            case WireDrawingElement.WINDING -> tr("Turns");
            default -> tr("Count");
        });
        field("amount", () -> String.valueOf(r.amount), v -> {
            float f = EaField.parse(v);
            if (!Float.isNaN(f)) set(r.mode, r.mm2, r.tier, r.colour, Math.round(f));
        }, tr("Kits: 1 to 64 blocks. Windings: 1 to 1000 turns. Bare wire: 1 to 64 Copper Cable"));
        text("amountu", () -> switch (r.mode) {
            case WireDrawingElement.KIT -> tr("blocks");
            case WireDrawingElement.WINDING -> tr("turns");
            default -> tr("cables");
        });
        text("cost", () -> {
            double l = WireDrawingDescriptor.length(r.mode, r.amount);
            return tr("Takes %1$ copper, %2$ rubber, %3$ kJ", String.format("%.1f", l * WireDrawingDescriptor.copperPerBlock(r.mode, r.mm2)),
                String.format("%.1f", l * WireDrawingDescriptor.rubberPerBlock(r.mode, r.tier, r.mm2)),
                String.format("%.0f", l * WireDrawingDescriptor.energyPerBlock(r.mode, r.mm2) / 1000));
        });
        text("buffer", () -> tr("Held: %1$ copper, %2$ rubber", String.format("%.2f", r.copper), String.format("%.2f", r.rubber)));
        text("status", () -> WireDrawingElement.statusText(r.status), () -> switch (r.status) {
            case WireDrawingElement.NO_COPPER, WireDrawingElement.NO_RUBBER, WireDrawingElement.NO_POWER -> 0xFFA02020;
            case WireDrawingElement.RUNNING -> 0xFF206020;
            default -> LABEL;
        });
        button("run", () -> r.running ? tr("Stop") : tr("Start"), () -> r.clientRun(!r.running),
            tr("Changing what to make scraps the wire drawn so far"));
    }
}
