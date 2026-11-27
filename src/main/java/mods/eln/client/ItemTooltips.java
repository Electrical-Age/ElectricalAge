package mods.eln.client;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.IDescriptorItem;
import mods.eln.i18n.I18N;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Two-tier item tooltips: headline lines always show (styled {@link ChatFormatting#GRAY}), optional detail lines show
 * only while holding Shift (with a {@link ChatFormatting#DARK_GRAY} {@code "Hold Shift for details"} cue when hidden),
 * and control hints render {@link ChatFormatting#DARK_GRAY}.
 * <p>
 * Common-safe: holds no direct {@code net.minecraft.client.Minecraft} references; {@link ElnClient} wires
 * {@link #isShiftDown} on client startup.
 */
public final class ItemTooltips {
    private ItemTooltips() {
    }

    /** Sentinel line separating always-visible headline lines from Shift-only detail lines in {@code addInformation}. */
    public static final String DETAILS = "\u0000details";
    /** Prefix for control/cue lines styled {@link ChatFormatting#DARK_GRAY}. */
    public static final String HINT_PREFIX = "\u0000hint:";

    /** Wired by {@link ElnClient} to {@code Minecraft.getInstance().hasShiftDown()}; false on dedicated servers/tests. */
    public static volatile BooleanSupplier isShiftDown = () -> false;

    /** Descriptors whose addInformation threw once: logged once, then skipped. */
    private static final Set<Object> broken = new HashSet<>();

    /** Append Shift-only detail lines to a descriptor's {@code addInformation} list (adds {@link #DETAILS} once). */
    public static void detail(List<String> list, String... lines) {
        boolean any = false;
        for (String l : lines) {
            if (l == null || l.isEmpty()) continue;
            if (!any && !list.contains(DETAILS)) list.add(DETAILS);
            any = true;
            list.add(l);
        }
    }

    /** Append {@link ChatFormatting#DARK_GRAY} hint line(s) to a descriptor's {@code addInformation} list. */
    public static void hint(List<String> list, String... lines) {
        for (String l : lines) {
            if (l != null && !l.isEmpty()) list.add(HINT_PREFIX + l);
        }
    }

    /** For standalone {@code Item.appendHoverText}: emit detail components while Shift is held, else the Shift cue. */
    public static void addDetails(Consumer<Component> out, Component... details) {
        if (details == null || details.length == 0) return;
        if (isShiftDown.getAsBoolean()) {
            for (Component c : details) if (c != null) out.accept(c);
        } else {
            out.accept(shiftCue());
        }
    }

    /** For standalone {@code Item.appendHoverText}: emit detail components while Shift is held, else the Shift cue. */
    public static void addDetails(Consumer<Component> out, List<Component> details) {
        if (details == null || details.isEmpty()) return;
        if (isShiftDown.getAsBoolean()) {
            for (Component c : details) if (c != null) out.accept(c);
        } else {
            out.accept(shiftCue());
        }
    }

    /** For standalone {@code Item.appendHoverText}: emit {@link ChatFormatting#GRAY} detail strings while Shift is held. */
    public static void addDetailsText(Consumer<Component> out, String... details) {
        if (details == null || details.length == 0) return;
        List<Component> comps = new ArrayList<>();
        for (String l : details) {
            if (l == null || l.isEmpty()) continue;
            for (String part : l.split("\n")) comps.add(styled(part));
        }
        addDetails(out, comps);
    }

    /** A {@link ChatFormatting#DARK_GRAY} control/hint line for {@code Item.appendHoverText}. */
    public static Component hintLine(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component shiftCue() {
        return Component.literal(I18N.tr("Hold Shift for details")).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static Component styled(String line) {
        if (line.startsWith(HINT_PREFIX)) {
            return Component.literal(line.substring(HINT_PREFIX.length())).withStyle(ChatFormatting.DARK_GRAY);
        }
        return Component.literal(line).withStyle(ChatFormatting.GRAY);
    }

    private static void emitLines(List<Component> tooltip, List<String> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<String> head = new ArrayList<>();
        List<String> det = new ArrayList<>();
        boolean inDetails = false;
        for (String l : lines) {
            if (l == null) continue;
            if (DETAILS.equals(l)) {
                inDetails = true;
                continue;
            }
            boolean isHint = l.startsWith(HINT_PREFIX);
            String raw = isHint ? l.substring(HINT_PREFIX.length()) : l;
            for (String part : raw.split("\n")) {
                String entry = isHint ? HINT_PREFIX + part : part;
                if (inDetails) det.add(entry);
                else head.add(entry);
            }
        }
        for (String l : head) tooltip.add(styled(l));
        if (!det.isEmpty()) {
            if (isShiftDown.getAsBoolean()) {
                for (String l : det) tooltip.add(styled(l));
            } else {
                tooltip.add(shiftCue());
            }
        }
    }

    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();
        if (stack.getItem() instanceof mods.eln.node.voxel.PipeSensorItem ps) {
            emitLines(e.getToolTip(), ps.tooltip());
            return;
        }
        if (stack.getItem() instanceof mods.eln.node.voxel.ValveItem v && v.tooltip() != null) {
            emitLines(e.getToolTip(), v.tooltip());
            return;
        }
        if (stack.getItem() instanceof mods.eln.node.voxel.SafetyValveItem sv) {
            emitLines(e.getToolTip(), sv.tooltip());
            return;
        }
        if (!(stack.getItem() instanceof IDescriptorItem di)) return;
        Object d = di.getEaDescriptor();
        if (d == null || broken.contains(d)) return;
        List<String> lines = new ArrayList<>();
        boolean advanced = e.getFlags().isAdvanced();
        try {
            if (d instanceof GenericItemUsingDamageDescriptor g) g.addInformation(stack, e.getEntity(), lines, advanced);
            else if (d instanceof GenericItemBlockUsingDamageDescriptor b) b.addInformation(stack, e.getEntity(), lines, advanced);
        } catch (RuntimeException ex) {
            broken.add(d);
            Eln.LOG.warn("tooltip of {} failed", d, ex);
            return;
        }
        emitLines(e.getToolTip(), lines);
    }
}
