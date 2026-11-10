package mods.eln.item.electricalitem;

import mods.eln.generic.ItemNbt;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * E-Coal armour (1.12 ElectricalArmor, a Forge ISpecialArmor): protection comes from stored energy, not from armour
 * points. Each piece absorbs a share ratio = min(1, energy / ratioMaxEnergy) * ratioMax of the damage (at most
 * energy / energyPerDamage), and pays energyPerDamage per (whole) damage point it absorbed.
 * 26.x: ISpecialArmor is gone; the pieces have no armour points (their material's defence is 0) and the absorption is
 * done in {@link #onIncomingDamage} (NeoForge LivingIncomingDamageEvent), the Forge 1.12 ArmorProperties maths:
 * damage scaled by 25, ratios summed and capped at 1, armour-bypassing damage ignored.
 */
public class ElectricalArmor extends Item implements IItemEnergyBattery {
    private final double energyStorage;
    final double chargePower;
    private final double ratioMax, ratioMaxEnergy, energyPerDamage;

    public ElectricalArmor(Properties properties, double energyStorage, double chargePower, double ratioMax, double ratioMaxEnergy,
                           double energyPerDamage) {
        super(properties);
        this.energyStorage = energyStorage;
        this.chargePower = chargePower;
        this.ratioMax = ratioMax;
        this.ratioMaxEnergy = ratioMaxEnergy;
        this.energyPerDamage = energyPerDamage;
    }

    /** The absorbed share of incoming damage at the stack's energy (1.12 ArmorProperties.AbsorbRatio). */
    public double absorbRatio(ItemStack armor) {
        return Math.min(1.0, getEnergy(armor) / ratioMaxEnergy) * ratioMax;
    }

    /** 1.12 ArmorProperties.AbsorbMax (in 1/25 damage units). */
    int absorbMax(ItemStack armor) {
        return (int) (getEnergy(armor) / energyPerDamage * 25.0);
    }

    /** 1.12 damageArmor: energy pays for the damage points absorbed. */
    void damageArmor(ItemStack stack, int damage) {
        setEnergy(stack, Math.max(0.0, getEnergy(stack) - damage * energyPerDamage));
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        double total = 0;
        double[] ratios = new double[ARMOR.length];
        for (int i = 0; i < ARMOR.length; i++) {
            ItemStack s = entity.getItemBySlot(ARMOR[i]);
            if (s.getItem() instanceof ElectricalArmor a) total += ratios[i] = a.absorbRatio(s);
        }
        if (total <= 0) return;
        double damage = event.getAmount() * 25.0;
        double scale = total > 1 ? 1 / total : 1;
        double left = damage;
        for (int i = 0; i < ARMOR.length; i++) {
            if (ratios[i] <= 0) continue;
            ItemStack s = entity.getItemBySlot(ARMOR[i]);
            ElectricalArmor a = (ElectricalArmor) s.getItem();
            double absorb = Math.min(damage * ratios[i] * scale, a.absorbMax(s));
            if (absorb > 0) {
                a.damageArmor(s, (int) Math.max(1, absorb / 25.0));
                left -= absorb;
            }
        }
        event.setAmount((float) Math.max(0, left / 25.0));
    }

    private CompoundTag nbt(ItemStack stack) {
        CompoundTag t = ItemNbt.get(stack);
        if (t == null) {
            t = new CompoundTag();
            t.putDouble("energy", 0.0);
            t.putBoolean("powerOn", false);
            t.putInt("rand", (int) (Math.random() * 0xFFFFFFF));
        }
        return t;
    }

    @Override
    public double getEnergy(ItemStack stack) {
        return nbt(stack).getDoubleOr("energy", 0.0);
    }

    @Override
    public void setEnergy(ItemStack stack, double value) {
        CompoundTag t = nbt(stack);
        t.putDouble("energy", value);
        ItemNbt.set(stack, t);
    }

    @Override
    public double getEnergyMax(ItemStack stack) {
        return energyStorage;
    }

    @Override
    public double getChargePower(ItemStack stack) {
        return chargePower;
    }

    @Override
    public double getDischagePower(ItemStack stack) {
        return 0.0;
    }

    @Override
    public int getPriority(ItemStack stack) {
        return 0;
    }

    @Override
    public void electricalItemUpdate(ItemStack stack, double time) {
    }

    /** Stored energy and its share (nero 2026-11-10: the armour showed no charge when hovered in a charger). */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> out, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, out, flag);
        double e = getEnergy(stack);
        out.accept(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Stored energy: %1$J (%2$%)",
            mods.eln.misc.Utils.plotValue(e), (int) (e / energyStorage * 100))).withStyle(net.minecraft.ChatFormatting.GRAY));
        // (nero: show its armour stats; it has no armour points, the stored energy absorbs a share of the damage)
        out.accept(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Absorbs %1$% of damage now (up to %2$% charged)",
            Math.round(absorbRatio(stack) * 100), Math.round(ratioMax * 100))).withStyle(net.minecraft.ChatFormatting.BLUE));
        out.accept(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Each damage point absorbed costs %1$J",
            mods.eln.misc.Utils.plotValue(energyPerDamage))).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    // R5: energy as the vanilla item bar (see ElnItem)
    @Override
    public boolean isBarVisible(net.minecraft.world.item.ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(net.minecraft.world.item.ItemStack stack) {
        return mods.eln.generic.ElnItem.energyBarWidth(this, stack);
    }

    @Override
    public int getBarColor(net.minecraft.world.item.ItemStack stack) {
        return mods.eln.generic.ElnItem.energyBarColor(this, stack);
    }
}
