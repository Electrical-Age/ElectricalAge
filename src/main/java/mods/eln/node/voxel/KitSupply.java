package mods.eln.node.voxel;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What a route is laid from: a cable or pipe kit, or a Toolbag's kits of its chosen kind taken as one (ToolbagItem). The placer
 * (RoutePlacer), its preview (KitPreview) and its lapse check ask the item in the main hand for one.
 */
public interface KitSupply {
    /** A pipe (else a cable). */
    boolean pipe();

    /** A servo track (TrackKitItem; then pipe() is false). */
    default boolean track() {
        return false;
    }

    /** CablePart.TIERS or PipePart tier. */
    int tier();

    /** A power cable's spec (CableKitItem.specOf: the tier's preset if none is set), else null. */
    CableSpec spec();

    /** Voxels it holds (several kits together may hold more than one kit's KitItem.MAX). */
    int length();

    /** Use up len voxels (no more than length()). */
    void take(int len);

    /** The undo, as a sentence after a refusal: "Sneak-click to undo." (the Toolbag: "Sneak-click for Undo."). */
    default String undoSentence() {
        return "Sneak-click to undo.";
    }

    /** How to give up instead: "sneak-click to cancel" (the Toolbag: "sneak-click for Undo"). */
    default String cancelClause() {
        return "sneak-click to cancel";
    }

    /** One kit. */
    static KitSupply of(ItemStack kit) {
        boolean pipe = kit.getItem() instanceof PipeKitItem, track = kit.getItem() instanceof TrackKitItem;
        int tier = kit.getItem() instanceof PipeKitItem p ? p.tier : kit.getItem() instanceof CableKitItem c ? c.tier : -1;
        CableSpec spec = CableKitItem.specOf(kit);
        return new KitSupply() {
            public boolean pipe() {
                return pipe;
            }

            public boolean track() {
                return track;
            }

            @Override
            public String undoSentence() {   // (the Track Kit: a sneak-click elsewhere puts a stop)
                return track ? "Sneak-click the last point to undo." : KitSupply.super.undoSentence();
            }

            @Override
            public String cancelClause() {
                return track ? "sneak-click the start to cancel" : KitSupply.super.cancelClause();
            }

            public int tier() {
                return tier;
            }

            public CableSpec spec() {
                return spec;
            }

            public int length() {
                return KitItem.length(kit);
            }

            public void take(int len) {
                int left = KitItem.length(kit) - len;
                if (left <= 0) kit.shrink(1);
                else KitItem.setLength(kit, left);
            }
        };
    }

    /**
     * Pipe of a tier for something another item builds (a valve clicked on a tank builds its stub: SafetyValveItem),
     * from what p carries: a pipe kit of the tier in the inventory, or a Toolbag's kits of it there, holding at least
     * need voxels. Loose kits are not added up (they merge by themselves within a second: KitItem.tick). Not from a
     * Toolbag whose menu is open: the menu holds its own copy of the bag's slots and writes it back over the bag at any
     * change, which would give the pipe back. Null if nothing carried has that much.
     */
    static KitSupply pipeCarried(Player p, int tier, int need) {
        if (p == null) return null;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            KitSupply k = null;
            if (s.getItem() instanceof PipeKitItem kit && kit.tier == tier) k = of(s);
            else if (s.getItem() instanceof ToolbagItem && !(p.containerMenu instanceof ToolbagMenu m && m.bag == s))
                k = ToolbagItem.supply(s, new ToolbagItem.Kind(true, tier, -1, -1));
            if (k != null && k.length() >= need) return k;
        }
        return null;
    }

    /** The supply in p's main hand: a kit, or a Toolbag with a kind chosen that it holds; else null. */
    static KitSupply held(Player p) {
        if (p == null) return null;
        ItemStack s = p.getMainHandItem();
        if (s.getItem() instanceof CableKitItem || s.getItem() instanceof PipeKitItem || s.getItem() instanceof TrackKitItem) return of(s);
        if (s.getItem() instanceof ToolbagItem) return ToolbagItem.supply(s);
        return null;
    }
}
