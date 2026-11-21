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

    /** CablePart.TIERS or PipePart tier. */
    int tier();

    /** A power cable's spec (CableKitItem.specOf: the tier's preset if none is set), else null. */
    CableSpec spec();

    /** Voxels it holds (several kits together may hold more than one kit's KitItem.MAX). */
    int length();

    /** Use up len voxels (no more than length()). */
    void take(int len);

    /** What to sneak-click for to undo, in the placers' messages. */
    default String undoHint() {
        return "sneak-click";
    }

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
        boolean pipe = kit.getItem() instanceof PipeKitItem;
        int tier = kit.getItem() instanceof PipeKitItem p ? p.tier : kit.getItem() instanceof CableKitItem c ? c.tier : -1;
        CableSpec spec = CableKitItem.specOf(kit);
        return new KitSupply() {
            public boolean pipe() {
                return pipe;
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

    /** The supply in p's main hand: a kit, or a Toolbag with a kind chosen that it holds; else null. */
    static KitSupply held(Player p) {
        if (p == null) return null;
        ItemStack s = p.getMainHandItem();
        if (s.getItem() instanceof CableKitItem || s.getItem() instanceof PipeKitItem) return of(s);
        if (s.getItem() instanceof ToolbagItem) return ToolbagItem.supply(s);
        return null;
    }

    /** Does supply s lay what a route of this kind was started with (pipe or cable, tier, spec)? */
    static boolean lays(KitSupply s, boolean pipe, int tier, CableSpec spec) {
        return s != null && s.pipe() == pipe && s.tier() == tier && java.util.Objects.equals(s.spec(), spec);
    }
}
