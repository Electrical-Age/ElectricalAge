package mods.eln.wiki;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

import static mods.eln.i18n.I18N.tr;

public class Data {
    public static final HashMap<String, ArrayList<ItemStack>> groupes = new HashMap<String, ArrayList<ItemStack>>();

    public static void add(String str, ItemStack stack) {
        ArrayList<ItemStack> groupe;
        if ((groupe = groupes.get(str)) == null) {
            groupes.put(str, groupe = new ArrayList<ItemStack>());
        }
        groupe.add(stack);
    }

    public static void addLight(ItemStack stack) {
        add(tr("Light"), stack);
    }

    public static void addMachine(ItemStack stack) {
        add(tr("Machine"), stack);
    }

    public static void addWiring(ItemStack stack) {
        add(tr("Wiring"), stack);
    }

    public static void addThermal(ItemStack stack) {
        add(tr("Thermal"), stack);
    }

    public static void addEnergy(ItemStack stack) {

        add(tr("Energy"), stack);
    }

    public static void addUtilities(ItemStack stack) {

        add(tr("Utilities"), stack);
    }

    public static void addSignal(ItemStack stack) {

        add(tr("Signal"), stack);
    }

    public static void addOre(ItemStack stack) {

        add(tr("Ore"), stack);
    }

    public static void addPortable(ItemStack stack) {

        add(tr("Portable"), stack);
    }

    public static void addResource(ItemStack stack) {

        add(tr("Resource"), stack);
    }

    public static void addUpgrade(ItemStack stack) {

        add(tr("Upgrade"), stack);
    }
}
