package mods.eln.item.headlamp;

import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.item.electricalitem.LampItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * The headlamp (Baughn 2026-10-30): a small flashlight on a leather band, worn on the head, in the helmet slot (where it
 * protects as a leather cap) or in Curios' head slot. Switched with EA's headlamp key (K, ClientKeys -> PlayerKeys); worn
 * and on, it lights the way the wearer looks, as the flashlight does in hand, from its own charge (an EA item battery:
 * chargers fill it). The small flashlight's light: 10, 6 blocks, 20 W, 6 kJ.
 */
public class HeadlampItem extends Item implements IItemEnergyBattery {
    public static final int LIGHT = 10, RANGE = 6;
    public static final double POWER = 20, STORAGE = 6000, CHARGE = 100;

    public HeadlampItem(Properties p) {
        super(p);
    }

    static CompoundTag tag(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    static void edit(ItemStack s, Consumer<CompoundTag> f) {
        CompoundTag t = tag(s);
        f.accept(t);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static boolean isOn(ItemStack s) {
        return tag(s).getBooleanOr("on", false);
    }

    public static void setOn(ItemStack s, boolean on) {
        edit(s, t -> t.putBoolean("on", on));
    }

    /** The light it gives now: LIGHT when on and charged, else 0. */
    public int light(ItemStack s) {
        return isOn(s) && getEnergy(s) > 0 ? LIGHT : 0;
    }

    /** Worn and on: drain its charge for dt seconds (off when empty) and light the wearer's look. Server. */
    public static void wornTick(ItemStack s, Entity wearer, double dt) {
        if (!(s.getItem() instanceof HeadlampItem lamp) || !isOn(s) || !(wearer.level() instanceof ServerLevel level)) return;
        double e = lamp.getEnergy(s) - POWER * dt;
        if (e <= 0) {
            lamp.setEnergy(s, 0);
            setOn(s, false);
            return;
        }
        lamp.setEnergy(s, e);
        // (always the trail of light along the look: no client draws a beam for a worn lamp, so LampItem.clientBeam,
        // which hands the beam to Stasis Delvers' drawing for lamps in hand, doesn't apply)
        LampItem.placeBeam(level, wearer, LIGHT, RANGE);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) wornTick(stack, owner, 0.05);
    }

    /** The headlamp the player wears: the helmet slot, else Curios' head slot; EMPTY if none. */
    public static ItemStack worn(Player p) {
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof HeadlampItem) return head;
        return mods.eln.item.belt.CuriosCompat.headlamp(p);
    }

    /** The headlamp key (server): switch the worn headlamp, and say so. */
    public static void toggle(Player p) {
        ItemStack s = worn(p);
        if (s.isEmpty()) {
            p.sendOverlayMessage(Component.translatable("eln.headlamp.none"));
            return;
        }
        boolean on = !isOn(s);
        if (on && ((HeadlampItem) s.getItem()).getEnergy(s) <= 0) {
            p.sendOverlayMessage(Component.translatable("eln.headlamp.empty"));
            return;
        }
        setOn(s, on);
        p.sendOverlayMessage(Component.translatable(on ? "eln.headlamp.on" : "eln.headlamp.off"));
    }

    @Override
    public void appendHoverText(ItemStack s, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> out,
                                net.minecraft.world.item.TooltipFlag flag) {
        out.accept(Component.translatable("eln.headlamp.tip.wear").withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.translatable("eln.headlamp.tip.key", Component.keybind("key.eln.headlamp")).withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.translatable("eln.headlamp.tip.power", (int) POWER));
        out.accept(Component.translatable("eln.headlamp.tip.energy", (int) Math.round(getEnergy(s)), (int) Math.round(100 * getEnergy(s) / STORAGE)));
        out.accept(Component.translatable(isOn(s) ? "eln.headlamp.state.on" : "eln.headlamp.state.off"));
    }

    // ---- an EA item battery (chargers fill it)

    @Override
    public double getEnergy(ItemStack stack) {
        return tag(stack).getDoubleOr("energy", 0.0);
    }

    @Override
    public void setEnergy(ItemStack stack, double value) {
        edit(stack, t -> t.putDouble("energy", Math.max(0, Math.min(STORAGE, value))));
    }

    @Override
    public double getEnergyMax(ItemStack stack) {
        return STORAGE;
    }

    @Override
    public double getChargePower(ItemStack stack) {
        return CHARGE;
    }

    @Override
    public double getDischagePower(ItemStack stack) {
        return 0;
    }

    @Override
    public int getPriority(ItemStack stack) {
        return 0;
    }

    @Override
    public void electricalItemUpdate(ItemStack stack, double time) {
    }
}
