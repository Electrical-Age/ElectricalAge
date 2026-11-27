package mods.eln.gridnode.electricalpole;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Equipment for a utility pole other than its transformer (Baughn 2026-11-08), fitted by right-clicking the pole:
 * a streetlight (on a beam out of the side the player stands on), or a stasis anchor (keeps the pole running in
 * stopped land; its recipe comes from Stasis Delvers, which owns the rift crystal).
 */
public class PoleGearItem extends Item {
    public enum Kind {STREETLIGHT, ANCHOR}

    public final Kind kind;

    public PoleGearItem(Kind kind, Properties p) {
        super(p.stacksTo(16));
        this.kind = kind;
    }

    public static boolean is(ItemStack s, Kind kind) {
        return !s.isEmpty() && s.getItem() instanceof PoleGearItem g && g.kind == kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        String line = switch (kind) {
            case STREETLIGHT -> String.format("Pole lamp (%.0f W), lights at dusk", ElectricalPoleElement.LAMP_WATTS);
            case ANCHOR -> "Keeps a utility pole running in stopped time";
        };
        out.accept(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
        mods.eln.client.ItemTooltips.addDetails(out,
            mods.eln.client.ItemTooltips.hintLine(mods.eln.i18n.I18N.tr(kind == Kind.STREETLIGHT
                ? "Right-click a utility pole from the side it should face"
                : "Right-click a utility pole to fit")));
    }
}
