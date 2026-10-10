package mods.eln.selftest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemUsingDamage;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

/**
 * `/eln selftest inventory`: every EA block and item (including every damage sub-item and its descriptor) as JSON,
 * with each descriptor's primitive fields. Port-to-26.x step T1/T4 (notes/port26/PLAN.md): the coverage checklist
 * for the new registry, and golden numbers to diff against. selftestPlaced: placed by a selftest since server start
 * (run `/eln selftest` first). Written to &lt;server dir&gt;/eln-inventory.json.
 */
public final class Inventory {
    private Inventory() {
    }

    public static void command(ICommandSender sender) {
        File out = new File("eln-inventory.json");
        try (Writer w = new FileWriter(out)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
            gson.toJson(dump(), w);
            sender.sendMessage(new TextComponentString("eln inventory written to " + out.getAbsolutePath()));
        } catch (IOException e) {
            sender.sendMessage(new TextComponentString("eln inventory: " + e));
        }
    }

    static JsonObject dump() {
        JsonObject root = new JsonObject();
        JsonArray blocks = new JsonArray();
        for (Block b : ForgeRegistries.BLOCKS.getValuesCollection()) {
            if (b.getRegistryName() == null || !Eln.MODID.equals(b.getRegistryName().getNamespace())) continue;
            JsonObject o = new JsonObject();
            o.addProperty("id", b.getRegistryName().toString());
            o.addProperty("class", b.getClass().getName());
            blocks.add(o);
        }
        root.add("blocks", blocks);

        JsonArray items = new JsonArray();
        for (Item it : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (it.getRegistryName() == null || !Eln.MODID.equals(it.getRegistryName().getNamespace())) continue;
            Map<Integer, ?> subs = null;
            if (it instanceof GenericItemBlockUsingDamage) subs = ((GenericItemBlockUsingDamage<?>) it).subItemList;
            else if (it instanceof GenericItemUsingDamage) subs = ((GenericItemUsingDamage<?>) it).subItemList;
            if (subs == null) {
                items.add(entry(it, 0, null));
            } else {
                for (Map.Entry<Integer, ?> e : new TreeMap<Integer, Object>(subs).entrySet())
                    items.add(entry(it, e.getKey(), e.getValue()));
            }
        }
        root.add("items", items);
        return root;
    }

    static JsonObject entry(Item it, int damage, Object descriptor) {
        JsonObject o = new JsonObject();
        o.addProperty("id", it.getRegistryName().toString());
        o.addProperty("damage", damage);
        if (it.getHasSubtypes()) {
            o.addProperty("subId", damage & 63);
            o.addProperty("groupId", damage >> 6);
        }
        String name;
        try {
            name = new ItemStack(it, 1, damage).getDisplayName();
        } catch (RuntimeException e) {
            name = "?";
        }
        o.addProperty("name", name);
        o.addProperty("itemClass", it.getClass().getName());
        if (it == Eln.sixNodeItem) o.addProperty("selftestPlaced", SelfTest.PLACED_SIX.contains(damage));
        if (it == Eln.transparentNodeItem) o.addProperty("selftestPlaced", SelfTest.PLACED_TRANSPARENT.contains(damage));
        if (descriptor != null) {
            o.addProperty("descriptorClass", descriptor.getClass().getName());
            o.add("fields", fields(descriptor));
        }
        return o;
    }

    /** Primitive, String and enum fields (and primitive arrays) of the descriptor and its superclasses. */
    static JsonObject fields(Object d) {
        JsonObject f = new JsonObject();
        for (Class<?> c = d.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field fl : c.getDeclaredFields()) {
                if (Modifier.isStatic(fl.getModifiers()) || f.has(fl.getName())) continue;
                Class<?> t = fl.getType();
                try {
                    fl.setAccessible(true);
                    Object v = fl.get(d);
                    if (v == null) continue;
                    if (t.isPrimitive() || v instanceof Number || v instanceof Boolean) {
                        if (v instanceof Boolean) f.addProperty(fl.getName(), (Boolean) v);
                        else if (v instanceof Character) f.addProperty(fl.getName(), v.toString());
                        else f.addProperty(fl.getName(), (Number) v);
                    } else if (v instanceof String || t.isEnum()) {
                        f.addProperty(fl.getName(), v.toString());
                    } else if (v instanceof double[]) {
                        JsonArray a = new JsonArray();
                        for (double x : (double[]) v) a.add(new JsonPrimitive(x));
                        f.add(fl.getName(), a);
                    } else if (v instanceof int[]) {
                        JsonArray a = new JsonArray();
                        for (int x : (int[]) v) a.add(new JsonPrimitive(x));
                        f.add(fl.getName(), a);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // inaccessible: skip
                }
            }
        }
        return f;
    }
}
