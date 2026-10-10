package mods.eln.generic;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.EntityEquipmentSlot;

public class genericArmorItem extends ItemArmor {

    String t1, t2;

    public enum ArmourType {
        Helmet(0),
        Chestplate(1),
        Leggings(2),
        Boots(3);

        private int _Value;

        private ArmourType(int Value) {
            this._Value = Value;
        }

        public int getValue() {
            return _Value;
        }
    }

    /** 1.7.10 armour type index (0 helmet .. 3 boots) -> 1.12 equipment slot. */
    private static final EntityEquipmentSlot[] SLOTS = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};

    public genericArmorItem(ArmorMaterial par2EnumArmorMaterial, int par3, ArmourType Type, String t1, String t2) {
        super(par2EnumArmorMaterial, par3, SLOTS[Type.getValue()]);
        this.t1 = t1;
        this.t2 = t2;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String layer) {
        if (this.armorType == EntityEquipmentSlot.LEGS) {
            return t2;
        } else {
            return t1;
        }
    }
}
