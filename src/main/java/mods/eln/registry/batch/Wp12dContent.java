package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.item.electricalitem.ElectricalArmor;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Map;

/**
 * Device batch Wp12d (26.x port, split from 1.12 Wp12Content): copper tools and armour (1.12: vanilla ToolMaterial.IRON
 * / ArmorMaterial.IRON with EA textures) and the E-Coal armour (energy instead of armour points, see ElectricalArmor).
 * 1.12 registry names kept (eln:copper_sword ... eln:e_coal_boots). Armour textures: equipment assets eln:copper and
 * eln:ecoal (client, P3).
 */
public class Wp12dContent implements ElnContent {
    public static DeferredItem<Item> swordCopper, hoeCopper, shovelCopper, pickaxeCopper, axeCopper;
    public static DeferredItem<Item> helmetCopper, plateCopper, legsCopper, bootsCopper;
    public static DeferredItem<ElectricalArmor> helmetECoal, plateECoal, legsECoal, bootsECoal;

    static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("eln", name));
    }

    static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
        Map<ArmorType, Integer> m = new EnumMap<>(ArmorType.class);
        m.put(ArmorType.BOOTS, boots);
        m.put(ArmorType.LEGGINGS, legs);
        m.put(ArmorType.CHESTPLATE, chest);
        m.put(ArmorType.HELMET, helmet);
        return m;
    }

    /** 1.12 copper armour = ArmorMaterial.IRON (durability 15, 2/5/6/2, enchantability 9) with EA's textures. */
    static final ArmorMaterial COPPER = new ArmorMaterial(15, defense(2, 5, 6, 2), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
        ItemTags.REPAIRS_IRON_ARMOR, asset("copper"));

    /**
     * 1.12 E-Coal: durability 10, enchantability 0 (ElectricalArmor.getItemEnchantability), not repairable; no armour
     * points (1.12 ISpecialArmor ignored them: protection is the energy ratio).
     */
    static final ArmorMaterial ECOAL = new ArmorMaterial(10, defense(0, 0, 0, 0), 1, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("eln", "repairs_e_coal_armor")), asset("ecoal"));

    @Override
    public void preInit() {
        registerTool();
        registerArmor();
    }

    static DeferredItem<Item> item(String name, java.util.function.UnaryOperator<Item.Properties> props) {
        DeferredItem<Item> i = ElnRegistry.ITEMS.registerItem(name, Item::new, props);
        ElnRegistry.ALL_ITEMS.add(i);
        return i;
    }

    public static void registerTool() {
        // 1.12 ToolMaterial.IRON: as the 26.x iron tools
        swordCopper = item("copper_sword", p -> p.sword(ToolMaterial.IRON, 3.0F, -2.4F));
        hoeCopper = item("copper_hoe", p -> p.hoe(ToolMaterial.IRON, -2.0F, -1.0F));
        shovelCopper = item("copper_shovel", p -> p.shovel(ToolMaterial.IRON, 1.5F, -3.0F));
        pickaxeCopper = item("copper_pickaxe", p -> p.pickaxe(ToolMaterial.IRON, 1.0F, -2.8F));
        axeCopper = item("copper_axe", p -> p.axe(ToolMaterial.IRON, 6.0F, -3.1F));
    }

    static DeferredItem<ElectricalArmor> eCoal(String name, ArmorType type, int armor, int armorMarge) {
        double energyPerDamage = 500;
        DeferredItem<ElectricalArmor> i = ElnRegistry.ITEMS.registerItem(name, p -> new ElectricalArmor(p,
            (armor + armorMarge) * energyPerDamage, 250.0, // energyStorage, chargePower
            armor / 20.0, armor * energyPerDamage, // ratioMax, ratioMaxEnergy
            energyPerDamage), p -> p.durability(type.getDurability(ECOAL.durability()))
            // humanoidArmor without enchantable (1.12 enchantability 0) and without a repair ingredient (not repairable)
            .attributes(ECOAL.createAttributes(type))
            .component(net.minecraft.core.component.DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable
                .builder(type.getSlot()).setEquipSound(ECOAL.equipSound()).setAsset(ECOAL.assetId()).build()));
        ElnRegistry.ALL_ITEMS.add(i);
        return i;
    }

    public static void registerArmor() {
        helmetCopper = item("copper_helmet", p -> p.humanoidArmor(COPPER, ArmorType.HELMET));
        plateCopper = item("copper_chestplate", p -> p.humanoidArmor(COPPER, ArmorType.CHESTPLATE));
        legsCopper = item("copper_leggings", p -> p.humanoidArmor(COPPER, ArmorType.LEGGINGS));
        bootsCopper = item("copper_boots", p -> p.humanoidArmor(COPPER, ArmorType.BOOTS));
        // 1.7.10 E-Coal armour points {2, 6, 5, 2} (helmet, chest, legs, boots) became the energy ratios
        helmetECoal = eCoal("e_coal_helmet", ArmorType.HELMET, 2, 1);
        plateECoal = eCoal("e_coal_chestplate", ArmorType.CHESTPLATE, 6, 2);
        legsECoal = eCoal("e_coal_leggings", ArmorType.LEGGINGS, 5, 2);
        bootsECoal = eCoal("e_coal_boots", ArmorType.BOOTS, 2, 1);
    }
}
