package mods.eln.compat;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10 GameRegistry calls EA uses, mapped to 1.12 (porting guide rules 2 and 6).
 * <ul>
 * <li>registerBlock / registerItem: called during preInit as before; objects get the registry name
 * {@code eln:<snake_case(name)>} and are registered when Forge fires RegistryEvent.Register (after preInit).
 * The ItemBlock is created immediately (EA fills its descriptor tables during preInit): use
 * {@link #getItemBlock(Block)} instead of Item.getItemFromBlock, which only works after registration.</li>
 * <li>registerCustomItemStack / findItemStack: EA's lookup of stacks by display name (recipes).
 * Only custom stacks are known here; 1.7.10 also fell back to items/blocks registered under that name.</li>
 * </ul>
 * Register the instance on MinecraftForge.EVENT_BUS (Eln does, in its constructor).
 */
public final class GameRegistryCompat {
    public static final String MODID = "eln";
    public static final GameRegistryCompat EVENTS = new GameRegistryCompat();

    private static final Map<String, ItemStack> customItemStacks = new HashMap<String, ItemStack>();
    private static final List<Block> blocks = new ArrayList<Block>();
    private static final List<Item> items = new ArrayList<Item>();
    private static final Map<Block, ItemBlock> itemBlocks = new IdentityHashMap<Block, ItemBlock>();

    private GameRegistryCompat() {
    }

    /** "Eln.SixNode" -> "six_node", "eln.itemCreativeTab" -> "item_creative_tab", "Copper Sword" -> "copper_sword". */
    public static String registryName(String name) {
        if (name.startsWith("Eln.") || name.startsWith("eln.")) name = name.substring(4);
        String s = name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("[^A-Za-z0-9_]+", "_");
        return s.toLowerCase().replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    public static <T extends Block> T registerBlock(T block, String name) {
        return registerBlock(block, ItemBlock.class, name);
    }

    /** 1.7.10 registerBlock(block, itemBlockClass, name): the item block class needs a (Block) constructor. */
    public static <T extends Block> T registerBlock(T block, Class<? extends ItemBlock> itemClass, String name) {
        String id = registryName(name);
        block.setRegistryName(new ResourceLocation(MODID, id));
        blocks.add(block);
        if (itemClass != null) {
            ItemBlock item;
            try {
                item = itemClass.getConstructor(Block.class).newInstance(block);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Cannot create item block " + itemClass + " for " + name, e);
            }
            item.setRegistryName(new ResourceLocation(MODID, id));
            items.add(item);
            itemBlocks.put(block, item);
        }
        return block;
    }

    public static <T extends Item> T registerItem(T item, String name) {
        item.setRegistryName(new ResourceLocation(MODID, registryName(name)));
        items.add(item);
        return item;
    }

    /** The ItemBlock created by registerBlock (usable during preInit). */
    public static ItemBlock getItemBlock(Block block) {
        return itemBlocks.get(block);
    }

    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        for (Block block : blocks) event.getRegistry().register(block);
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (Item item : items) event.getRegistry().register(item);
    }

    public static void registerCustomItemStack(String name, ItemStack stack) {
        customItemStacks.put(name, stack);
    }

    /** A copy of the stack registered under {@code name} with the given size, or ItemStack.EMPTY. */
    public static ItemStack findItemStack(String modId, String name, int stackSize) {
        ItemStack stack = customItemStacks.get(name);
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        stack = stack.copy();
        stack.setCount(stackSize);
        return stack;
    }
}
