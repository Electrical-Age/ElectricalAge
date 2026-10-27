package mods.eln.handbook;

import mods.eln.Eln;
import mods.eln.registry.ElnRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Electrical Engineer's Ledger: EA's handbook, a GuideME guide opened by its own item (Baughn 2026-10-27). EA holds
 * the code and a stub start page; a mod that writes the content (Stasis) puts its pages in the guide's folder,
 * {@code assets/<its namespace>/guides/eln/handbook/}, and calls {@link #setStartPage} from its constructor.
 * Every player is given one Ledger, once, as soon as their inventory has a free slot.
 */
public final class Handbook {
    private Handbook() {
    }

    public static final Identifier GUIDE = Identifier.fromNamespaceAndPath(Eln.MODID, "handbook");
    /** Player persisted NBT: the Ledger has been given. */
    public static final String GIVEN_TAG = "eln_handbook_given";
    private static final int CHECK_TICKS = 40;

    private static volatile Identifier startPage = Identifier.fromNamespaceAndPath(Eln.MODID, "index.md");

    /** Makes the guide open on this page; call it from a mod constructor (the guide is built after them all). */
    public static void setStartPage(Identifier page) {
        startPage = page;
    }

    public static Identifier startPage() {
        return startPage;
    }

    public static boolean guideAvailable() {
        return ModList.get().isLoaded("guideme");
    }

    /** Gives the Ledger if this player never had one and has a free slot; true if it gave one now. */
    public static boolean tryGive(ServerPlayer p) {
        CompoundTag persisted = p.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        if (persisted.getBooleanOr(GIVEN_TAG, false)) return false;
        if (p.getInventory().getFreeSlot() < 0) return false;
        if (!p.getInventory().add(new ItemStack(ElnRegistry.HANDBOOK.get()))) return false;
        persisted.putBoolean(GIVEN_TAG, true);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        Eln.LOG.info("Electrical Engineer's Ledger for {}", p.getName().getString());
        return true;
    }

    public static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % CHECK_TICKS == 0 && !p.isSpectator()) tryGive(p);
    }
}
