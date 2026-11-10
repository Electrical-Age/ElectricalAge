package mods.eln.item.belt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The battery belt in Curios' belt slot, if Curios is loaded (no hard dependency: Impl is only touched after the ModList
 * check). A right-click opens the belt's pockets; a sneak-right-click puts it on (AdventurAgent: Curios' equip-on-use
 * took every right-click, so the pockets never opened).
 */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    static final boolean LOADED = net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("curios");

    /** The worn battery belt, or EMPTY (both sides: the HUD reads it). */
    static ItemStack belt(Player p) {
        return LOADED ? Impl.belt(p) : ItemStack.EMPTY;
    }

    /** The headlamp worn in Curios' head slot, or EMPTY. */
    public static ItemStack headlamp(Player p) {
        return LOADED ? Impl.headlamp(p) : ItemStack.EMPTY;
    }

    /** Curios is loaded (client code checks this before touching Curios classes). */
    public static boolean loaded() {
        return LOADED;
    }

    /** The Curios slot types the battery charger shows, in order: the battery belt's, the headlamp's, and the necklace
     *  (Stasis' rift pendants charge in it: nero 2026-11-10). */
    static final String[] WORN_TYPES = {"belt", "head", "necklace"};

    /** The player's Curios belt and head slots, for device menus (ElnNodeMenu; none without Curios). */
    public static final mods.eln.gui.ElnNodeMenu.Wearables WEARABLES = new mods.eln.gui.ElnNodeMenu.Wearables() {
        @Override
        public java.util.List<String> keys(Player player, int max) {
            return LOADED ? Impl.keys(player, max) : java.util.List.of();
        }

        @Override
        public net.minecraft.world.inventory.Slot slot(Player player, String key, int x, int y) {
            net.minecraft.world.inventory.Slot s = LOADED ? Impl.slot(player, key, x, y) : null;
            return s != null ? s : new mods.eln.gui.ElnNodeMenu.NoSlot(x, y);
        }
    };

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        if (LOADED) modBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class, e -> e.enqueueWork(Impl::register));
    }

    private static final class Impl {
        static void register() {
            top.theillusivec4.curios.api.CuriosApi.registerCurio(mods.eln.registry.ElnRegistry.BATTERY_BELT.get(),
                new top.theillusivec4.curios.api.type.capability.ICurioItem() {
                    /** Only sneaking, and only into an empty slot (Curios 17.0.0-beta.3 leaks a transaction swapping with an
                     *  occupied one: Stasis saw the server crash on it, 10-26). */
                    @Override
                    public boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext ctx, ItemStack stack) {
                        return ctx.entity().isShiftKeyDown() && top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(ctx.entity())
                            .flatMap(h -> h.getStacksHandler(ctx.identifier()))
                            .map(sh -> ctx.index() < sh.getStacks().getSlots() && sh.getStacks().getStackInSlot(ctx.index()).isEmpty())
                            .orElse(false);
                    }
                });
            // the headlamp in the head slot: lit while worn there; put on in the Curios screen (no equip on use: Curios
            // 17.0.0-beta.3 leaks a transaction swapping into an occupied slot, and right-click would wear it as a helmet)
            top.theillusivec4.curios.api.CuriosApi.registerCurio(mods.eln.registry.ElnRegistry.HEADLAMP.get(),
                new top.theillusivec4.curios.api.type.capability.ICurioItem() {
                    @Override
                    public void curioTick(top.theillusivec4.curios.api.SlotContext ctx, ItemStack stack) {
                        if (!ctx.entity().level().isClientSide()) mods.eln.item.headlamp.HeadlampItem.wornTick(stack, ctx.entity(), 0.05);
                    }

                    @Override
                    public boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext ctx, ItemStack stack) {
                        return false;
                    }
                });
        }

        static java.util.List<String> keys(Player p, int max) {
            java.util.List<String> out = new java.util.ArrayList<>();
            var h = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElse(null);
            if (h == null) return out;
            for (String type : WORN_TYPES) {
                var sh = h.getStacksHandler(type).orElse(null);
                if (sh == null || !sh.isVisible()) continue;
                for (int i = 0; i < sh.getSlots() && out.size() < max; i++) out.add(type + "#" + i);
            }
            return out;
        }

        /**
         * Curios' own slot class (as its screen builds them): Curios' handler checks what may go in (the slot type, canEquip,
         * CurioCanEquipEvent) and come out (canUnequip, CurioCanUnequipEvent, binding curses); the slot adds the type's icon,
         * its tooltip and the equip sound. Null if the player has no such slot.
         */
        static net.minecraft.world.inventory.Slot slot(Player p, String key, int x, int y) {
            int hash = key.lastIndexOf('#');
            if (hash <= 0) return null;
            String id = key.substring(0, hash);
            int index;
            try {
                index = Integer.parseInt(key.substring(hash + 1));
            } catch (NumberFormatException e) {
                return null;
            }
            var sh = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).flatMap(h -> h.getStacksHandler(id)).orElse(null);
            if (sh == null || index < 0 || index >= sh.getStacks().getSlots()) return null;
            return new top.theillusivec4.curios.common.inventory.CurioSlot(p, sh.getStacks(), index, id, x, y, sh.getRenders(),
                sh.getActiveStates(), sh.canToggleRendering(), false);
        }

        static ItemStack headlamp(Player p) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                .flatMap(h -> h.findFirstCurio(mods.eln.registry.ElnRegistry.HEADLAMP.get()))
                .map(top.theillusivec4.curios.api.SlotResult::stack).orElse(ItemStack.EMPTY);
        }

        static ItemStack belt(Player p) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                .flatMap(h -> h.findFirstCurio(mods.eln.registry.ElnRegistry.BATTERY_BELT.get()))
                .map(top.theillusivec4.curios.api.SlotResult::stack).orElse(ItemStack.EMPTY);
        }
    }
}
