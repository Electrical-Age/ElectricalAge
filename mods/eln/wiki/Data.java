package mods.eln.wiki;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.item.ItemStack;

public class Data {
	public static HashMap<String, ArrayList<ItemStack>> groupes = new HashMap<String, ArrayList<ItemStack>>();
	
	public static void add(String str,ItemStack stack){
		ArrayList<ItemStack> groupe;
		if((groupe = groupes.get(str)) == null){
			groupes.put(str,groupe = new ArrayList<ItemStack>());
		}
		groupe.add(stack);
	}

	public static void addLight(ItemStack stack) {
		add("Light", stack);
	}
	public static void addMachine(ItemStack stack) {
		add("Machine", stack);
	}
	public static void addWiring(ItemStack stack) {
		add("Wiring", stack);
	}
	public static void addThermal(ItemStack stack) {
		add("Thermal", stack);
	}

	public static void addEnergy(ItemStack stack) {
		// TODO Auto-generated method stub
		add("Energy", stack);
	}

	public static void addUtilities(ItemStack stack) {
		// TODO Auto-generated method stub
		add("Utilities",stack);
	}

	public static void addSignal(ItemStack stack) {
		// TODO Auto-generated method stub
		add("Signal",stack);
	}

	public static void addOre(ItemStack stack) {
		// TODO Auto-generated method stub
		add("Ore",stack);
	}

	public static void addPortable(ItemStack stack) {
		// TODO Auto-generated method stub
		add("Portable",stack);
	}
}
