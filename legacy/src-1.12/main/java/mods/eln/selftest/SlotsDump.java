package mods.eln.selftest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * GUI slot rules of every device with a container: per slot, which probe items it accepts and its stack limit. Written
 * by SmokeCases to &lt;server dir&gt;/eln-slots.json; golden data for the port (step T3/T4, notes/port26/PLAN.md).
 * Probes: every EA shared item (sub-item), every six-node cable-like device item, and some vanilla items.
 */
public final class SlotsDump {
    private SlotsDump() {
    }

    static List<ItemStack> probes;

    static List<ItemStack> probes() {
        if (probes != null) return probes;
        List<ItemStack> p = new ArrayList<>();
        for (Integer d : new TreeMap<>(Eln.sharedItem.subItemList).keySet()) p.add(new ItemStack(Eln.sharedItem, 1, d));
        for (Integer d : new TreeMap<>(Eln.sharedItemStackOne.subItemList).keySet()) p.add(new ItemStack(Eln.sharedItemStackOne, 1, d));
        for (Integer d : new TreeMap<>(Eln.sixNodeItem.subItemList).keySet()) p.add(new ItemStack(Eln.sixNodeItem, 1, d));
        p.add(new ItemStack(Items.COAL));
        p.add(new ItemStack(Items.REDSTONE));
        p.add(new ItemStack(Items.IRON_INGOT));
        p.add(new ItemStack(Items.EGG));
        p.add(new ItemStack(Items.PAPER));
        p.add(new ItemStack(Items.WATER_BUCKET));
        p.add(new ItemStack(Items.LAVA_BUCKET));
        p.add(new ItemStack(Blocks.COBBLESTONE));
        p.add(new ItemStack(Blocks.DIRT));
        probes = p;
        return p;
    }

    /** The device's container slots (not the player's), or null when it has no container. */
    public static JsonObject record(String device, ContainerSource src, Direction side, EntityPlayer player) {
        Container c;
        try {
            c = src.newContainer(side, player);
        } catch (RuntimeException e) {
            JsonObject o = new JsonObject();
            o.addProperty("device", device);
            o.addProperty("error", e.toString());
            return o;
        }
        if (c == null) return null;
        JsonObject o = new JsonObject();
        o.addProperty("device", device);
        o.addProperty("container", c.getClass().getSimpleName());
        JsonArray slots = new JsonArray();
        for (Slot s : c.inventorySlots) {
            if (s.inventory instanceof InventoryPlayer) continue;
            JsonObject so = new JsonObject();
            so.addProperty("index", s.getSlotIndex());
            so.addProperty("class", s.getClass().getSimpleName());
            so.addProperty("limit", s.getSlotStackLimit());
            JsonArray acc = new JsonArray();
            for (ItemStack probe : probes()) {
                boolean ok;
                try {
                    ok = s.isItemValid(probe.copy());
                } catch (RuntimeException e) {
                    ok = false;
                }
                if (ok) acc.add(probe.getItem().getRegistryName() + "@" + probe.getItemDamage() + " " + safeName(probe));
            }
            so.add("accepts", acc);
            slots.add(so);
        }
        o.add("slots", slots);
        return o;
    }

    static String safeName(ItemStack s) {
        try {
            return s.getDisplayName();
        } catch (RuntimeException e) {
            return "?";
        }
    }

    public interface ContainerSource {
        Container newContainer(Direction side, EntityPlayer player);
    }

    public static void write(Map<String, JsonObject> byDevice) {
        JsonArray all = new JsonArray();
        for (JsonObject o : byDevice.values()) all.add(o);
        try (Writer w = new FileWriter(new File("eln-slots.json"))) {
            new GsonBuilder().setPrettyPrinting().create().toJson(all, w);
        } catch (IOException e) {
            SelfTest.LOG.error("eln-slots.json", e);
        }
    }
}
