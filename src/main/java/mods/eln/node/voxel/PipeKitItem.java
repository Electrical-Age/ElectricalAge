package mods.eln.node.voxel;

import mods.eln.misc.Coordonate;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pipe kit (Baughn, notes/pipes/DESIGN.md 6a): a length of pipe of one tier, kept as the item's damage (stack size
 * is too low for pipes): length = MAX - damage voxels (16 voxels = one block). Right-click a port to route a pipe
 * (PipePlacer: waypoints, then a port to finish); right-clicking any other face lays one block of straight pipe,
 * centred, in the block in front of it, along the face's axis.
 */
public class PipeKitItem extends Item implements PipeTool {
    public static final int MAX = 1024;
    public final int tier;

    public PipeKitItem(int tier, Properties p) {
        super(p.durability(MAX));
        this.tier = tier;
    }

    public static int length(ItemStack s) {
        return s.getItem() instanceof PipeKitItem ? MAX - s.getDamageValue() : 0;
    }

    /** A kit holding len voxels of a tier's pipe (empty if none). */
    public static ItemStack kit(int tier, int len) {
        if (len <= 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(tier == 0 ? ElnRegistry.PIPE_KIT_IRON.get() : ElnRegistry.PIPE_KIT_ALLOY.get());
        s.setDamageValue(MAX - Math.min(MAX, len));
        return s;
    }

    /**
     * Picking up a kit tops up the player's other kits of its tier first (Baughn: stacking, reimplemented: a kit is a
     * one-item stack holding up to MAX voxels). What's left is picked up as usual.
     */
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e) {
        var ent = e.getItemEntity();
        ItemStack s = ent.getItem();
        if (!(s.getItem() instanceof PipeKitItem k) || e.getPlayer().level().isClientSide() || ent.hasPickUpDelay()) return;
        int len = length(s);
        var inv = e.getPlayer().getInventory();
        for (int i = 0; i < inv.getContainerSize() && len > 0; i++) {
            ItemStack o = inv.getItem(i);
            if (o == s || !(o.getItem() instanceof PipeKitItem ok) || ok.tier != k.tier) continue;
            int room = MAX - length(o);
            if (room <= 0) continue;
            int take = Math.min(room, len);
            o.setDamageValue(MAX - (length(o) + take));
            len -= take;
        }
        if (len == length(s)) return;
        if (len <= 0) {
            e.getPlayer().take(ent, 1);
            ent.discard();
            e.setCanPickup(net.minecraft.util.TriState.FALSE);
        } else s.setDamageValue(MAX - len);
    }

    /** Not by vanilla's repair (two kits would gain 5%): kits merge by their own recipe (PipeKitRecipes). */
    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(String.format(" (%.2f blocks)", length(stack) / 16.0));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        boolean creative = player != null && player.isCreative();
        var face = ctx.getClickedFace();
        // a route: started at a port, waypoints, finished at a port (PipePlacer); else one straight block
        if (player instanceof net.minecraft.server.level.ServerPlayer sp && level instanceof net.minecraft.server.level.ServerLevel sl
            && PipePlacer.use(sp, sl, ctx.getClickedPos(), face, stack, tier, ctx.getClickLocation())) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(face);
        if (!creative && length(stack) < 16) {
            if (player != null) player.sendOverlayMessage(Component.literal("Not enough pipe in this kit for a block"));
            return InteractionResult.FAIL;
        }
        int[] pts = switch (face.getAxis()) {
            case X -> new int[]{0, 8, 8, 16, 8, 8};
            case Y -> new int[]{8, 0, 8, 8, 16, 8};
            case Z -> new int[]{8, 8, 0, 8, 8, 16};
        };
        String err = place(level, pos, new PipePart(tier, pts), player);
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't lay pipe here: " + err));
            return InteractionResult.FAIL;
        }
        if (!creative) {
            int left = length(stack) - 16;
            if (left <= 0) stack.shrink(1);
            else stack.setDamageValue(MAX - left);
        }
        return InteractionResult.SUCCESS;
    }

    /** Put a part into the block at pos: a new voxel node in air (or anything replaceable), or into one already there. */
    public static String place(Level level, BlockPos pos, VoxelPart part, Player player) {
        BlockState old = level.getBlockState(pos);
        VoxelNodeBlock block = ElnRegistry.VOXEL_NODE_BLOCK.get();
        if (old.getBlock() == block) {
            if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode node)) return "no node here";
            return node.addPart(part);
        }
        if (!(old.isAir() || old.canBeReplaced())) return "the block is solid";
        VoxelNode node = new VoxelNode();
        node.onBlockPlacedBy(new Coordonate(pos, level), mods.eln.misc.Direction.YN, player, ItemStack.EMPTY);
        String err = node.addPart(part);
        if (err != null) {
            node.onBreakBlock();
            return err;
        }
        level.setBlock(pos, block.defaultBlockState(), 3);
        return null;
    }
}
