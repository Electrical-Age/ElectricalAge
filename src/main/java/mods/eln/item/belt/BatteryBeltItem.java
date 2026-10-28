package mods.eln.item.belt;

import mods.eln.item.electricalinterface.IItemEnergyBattery;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The battery belt (Baughn 2026-10-27; first in Stasis Delvers, moved here: it is EA's): a Curios belt with four
 * pockets for EA portable batteries and condensators, and whatever another mod tags {@link #EXTRAS} (Stasis: its rift
 * capacitors). As an EA item battery it stands for what it holds: an EA charger charges everything in it, and drawing
 * takes only from what can discharge (so a capacitor is never drained). Worn, it joins the player's item energy
 * (ItemEnergyInventoryProcess: EA tools draw from it), and its charge shows on the HUD (client BeltHud). Right-click in
 * hand opens its pockets (BeltMenu), sneak-right-click puts it on (CuriosCompat).
 */
public class BatteryBeltItem extends Item implements IItemEnergyBattery {
    public static final int SLOTS = 4;

    public BatteryBeltItem(Properties properties) {
        super(properties);
    }

    /** Items another mod lets into the pockets (Stasis: rift capacitors). */
    public static final net.minecraft.tags.TagKey<Item> EXTRAS = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
        net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "battery_belt_items"));
    /** Another mod's HUD bar sits just above the belt's (Stasis: the suit gauge, while it shows): the belt's bar is
     *  then slimmer, under it (Baughn: the two stacked). Client. */
    public static volatile java.util.function.BooleanSupplier hudAbove = () -> false;

    /** The belt the player wears (Curios belt slot), or EMPTY. */
    public static ItemStack worn(Player p) {
        ItemStack t = TEST_WORN.get(p.getUUID());
        return t != null ? t : CuriosCompat.belt(p);
    }

    /** GameTests: a belt "worn" by a test player (Curios isn't loaded in the test server). */
    public static final java.util.Map<java.util.UUID, ItemStack> TEST_WORN = new java.util.concurrent.ConcurrentHashMap<>();

    /** What may go in a pocket: an EA portable battery or condensator, or an item tagged EXTRAS. */
    public static boolean accepts(ItemStack s) {
        if (s.isEmpty() || s.getItem() instanceof BatteryBeltItem) return false;
        return mods.eln.misc.Utils.getItemObject(s) instanceof mods.eln.item.electricalitem.BatteryItem || s.is(EXTRAS);
    }

    /** The item battery behind a stack: the item itself, or an EA item's descriptor (EA's portable batteries are
     *  ElnItems whose descriptor is the battery); null if none. */
    public static IItemEnergyBattery battery(ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.getItem() instanceof IItemEnergyBattery b) return b;
        return mods.eln.misc.Utils.getItemObject(s) instanceof IItemEnergyBattery b ? b : null;
    }

    /** The pockets' contents (copies). */
    public static NonNullList<ItemStack> contents(ItemStack belt) {
        NonNullList<ItemStack> out = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        belt.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(out);
        return out;
    }

    public static void setContents(ItemStack belt, List<ItemStack> items) {
        belt.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /** A pocket that can give power (a capacitor, which can't discharge, can only be charged). */
    public static boolean isBattery(ItemStack s) {
        IItemEnergyBattery b = battery(s);
        return b != null && b.getDischagePower(s) > 0;
    }

    /** The batteries' charge as a share of what they hold (for the HUD); NaN when it holds none. */
    public static double batteryShare(ItemStack belt) {
        double e = 0, m = 0;
        for (ItemStack s : contents(belt))
            if (isBattery(s)) {
                IItemEnergyBattery b = battery(s);
                e += b.getEnergy(s);
                m += b.getEnergyMax(s);
            }
        return m <= 0 ? Double.NaN : e / m;
    }

    // ---- an EA item battery standing for what it holds: chargers fill the pockets in order; drawing takes from batteries

    @Override
    public double getEnergy(ItemStack stack) {
        double e = 0;
        for (ItemStack s : contents(stack)) if (battery(s) instanceof IItemEnergyBattery b) e += b.getEnergy(s);
        return e;
    }

    @Override
    public double getEnergyMax(ItemStack stack) {
        double e = 0;
        for (ItemStack s : contents(stack)) if (battery(s) instanceof IItemEnergyBattery b) e += b.getEnergyMax(s);
        return e;
    }

    @Override
    public void setEnergy(ItemStack stack, double value) {
        var all = contents(stack);
        double delta = value - getEnergy(stack);
        for (ItemStack s : all) {
            if (Math.abs(delta) < 1e-9) break;
            if (!(battery(s) instanceof IItemEnergyBattery b)) continue;
            double e = b.getEnergy(s);
            if (delta > 0) {
                double put = Math.min(delta, b.getEnergyMax(s) - e);
                if (put > 0) {
                    b.setEnergy(s, e + put);
                    delta -= put;
                }
            } else if (isBattery(s)) {   // (only what can discharge: a capacitor's charge isn't power)
                double take = Math.min(-delta, e);
                if (take > 0) {
                    b.setEnergy(s, e - take);
                    delta += take;
                }
            }
        }
        setContents(stack, all);
    }

    @Override
    public double getChargePower(ItemStack stack) {
        double p = 0;
        for (ItemStack s : contents(stack)) if (battery(s) instanceof IItemEnergyBattery b) p += b.getChargePower(s);
        return p;
    }

    @Override
    public double getDischagePower(ItemStack stack) {
        double p = 0;
        for (ItemStack s : contents(stack)) if (isBattery(s)) p += battery(s).getDischagePower(s);
        return p;
    }

    /** As a Portable Battery's (2): a source for tools, not a sink (ItemEnergyInventoryProcess moves energy from the
     *  highest priority to the lowest). */
    @Override
    public int getPriority(ItemStack stack) {
        return 2;
    }

    @Override
    public void electricalItemUpdate(ItemStack stack, double time) {
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;   // (sneaking: Curios puts it on)
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) BeltMenu.open(sp, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        int n = 0;
        for (ItemStack s : contents(stack)) if (!s.isEmpty()) n++;
        double share = batteryShare(stack);
        out.accept(Component.literal(String.format("%d of %d pockets used%s", n, SLOTS,
            Double.isNaN(share) ? "" : String.format("; batteries %.0f%%", share * 100))).withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("Worn as a belt, its batteries power your tools (and a Stasis suit)").withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("Right-click to open, sneak-right-click to put on; an EA charger charges what's inside").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !Double.isNaN(batteryShare(stack));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double s = batteryShare(stack);
        return (int) Math.round(13 * (Double.isNaN(s) ? 0 : Math.max(0, Math.min(1, s))));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x50a0d0;
    }
}
