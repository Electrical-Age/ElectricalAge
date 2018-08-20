package mods.eln.generic;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.ISpecialArmor;

public class SharedItem extends GenericItemUsingDamage<GenericItemUsingDamageDescriptor> implements ISpecialArmor {

    public SharedItem() {
        super();
    }

    @Override
    public ArmorProperties getProperties(EntityLivingBase player,
                                         ItemStack armor, DamageSource source, double damage, int slot) {
        return new ArmorProperties(10, 1.0, 10000);
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        return 4;
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack,
                            DamageSource source, int damage, int slot) {
    }
}
