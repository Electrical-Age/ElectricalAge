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
        }

        static ItemStack belt(Player p) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                .flatMap(h -> h.findFirstCurio(mods.eln.registry.ElnRegistry.BATTERY_BELT.get()))
                .map(top.theillusivec4.curios.api.SlotResult::stack).orElse(ItemStack.EMPTY);
        }
    }
}
