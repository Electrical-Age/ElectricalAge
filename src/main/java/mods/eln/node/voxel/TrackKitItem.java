package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A Track Kit (Baughn 2026-11-27, track B): a length of servo track, laid on the shared placer from a Linear Servo Drive
 * by clicking the corners (TrackPlacer). Filled with Servo Rails, one block of track each, like a pipe kit with plates;
 * holds up to KitItem.MAX voxels (64 blocks, a drive's longest track).
 */
public class TrackKitItem extends Item implements PipeTool, KitItem {
    /** Servo Rails per block of track. */
    public static final int PER_BLOCK = 1;

    public TrackKitItem(Properties p) {
        super(p.stacksTo(1));
    }

    /** A kit holding len voxels (empty if none). */
    public static ItemStack kit(int len) {
        if (len <= 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(mods.eln.robotics.Robotics.TRACK_KIT.get());
        KitItem.setLength(s, len);
        return s;
    }

    @Override
    public ItemStack make(int len) {
        return kit(len);
    }

    /** Is this stack Servo Rails (tier 0 for KitRecipes), else -1? */
    static int railTier(ItemStack s) {
        return s.is(mods.eln.robotics.Robotics.SERVO_RAIL_ITEM.get()) ? 0 : -1;
    }

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return KitItem.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return KitItem.barColor(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        int v = KitItem.length(stack);
        String blocks = v % 16 == 0 ? (v / 16) + (v == 16 ? " block" : " blocks") : String.format("%.1f blocks", v / 16.0);
        return Component.translatable(getDescriptionId()).append(" (" + blocks + ")");
    }

    /** Servo Rails into the kit, as plates go into a pipe kit: click a stack onto the kit, or the kit onto them. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, net.minecraft.world.inventory.Slot slot,
                                            net.minecraft.world.inventory.ClickAction action, Player player,
                                            net.minecraft.world.entity.SlotAccess carried) {
        return KitRecipes.clicked(self, 0, other, railTier(other), PER_BLOCK, other::shrink, slot, action, player);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                                          Player player) {
        ItemStack rails = slot.getItem();
        return KitRecipes.clicked(self, 0, rails, railTier(rails), PER_BLOCK, n -> slot.safeTake(n, n, player), slot, action, player);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (ctx.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp && level instanceof net.minecraft.server.level.ServerLevel sl
            && RoutePlacer.use(sp, sl, ctx.getClickedPos(), ctx.getClickedFace(), KitSupply.of(ctx.getItemInHand()), ctx.getClickLocation(),
            sp.isSecondaryUseActive())) return InteractionResult.SUCCESS;
        return InteractionResult.PASS;
    }
}
