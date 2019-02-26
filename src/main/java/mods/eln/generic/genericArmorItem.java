package mods.eln.generic;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;

public class genericArmorItem extends ItemArmor {

    String t1, t2;

    public genericArmorItem(ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn) {
        super(materialIn, renderIndexIn, equipmentSlotIn);
    }

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

//
//    // TODO(1.10): WTF?
//    @Override
//    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
//        if (this.armorType.getIndex() == 2) {
//            return t2;
//        } else {
//            return t1;
//        }
//    }
}
