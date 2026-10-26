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
public class PipeKitItem extends Item implements PipeTool, KitItem {
    public final int tier;

    public PipeKitItem(int tier, Properties p) {
        super(p.stacksTo(1));
        this.tier = tier;
    }

    public static int length(ItemStack s) {
        return s.getItem() instanceof PipeKitItem ? KitItem.length(s) : 0;
    }

    /** A kit holding len voxels of a tier's pipe (empty if none). */
    public static ItemStack kit(int tier, int len) {
        if (len <= 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(tier == 0 ? ElnRegistry.PIPE_KIT_IRON.get() : ElnRegistry.PIPE_KIT_ALLOY.get());
        KitItem.setLength(s, len);
        return s;
    }

    @Override
    public ItemStack make(int len) {
        return kit(tier, len);
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
    public int getBarWidth(ItemStack stack) {
        return KitItem.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return KitItem.barColor(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(String.format(" (%.2f blocks)", length(stack) / 16.0));
    }

    /**
     * Plates of this tier into the kit, like filling a bundle (as cable kits): click a stack onto the kit, or the kit
     * onto them. As many blocks as fit go in, PipeKitRecipes.PER_BLOCK plates each.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, net.minecraft.world.inventory.Slot slot,
                                            net.minecraft.world.inventory.ClickAction action, net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.entity.SlotAccess carried) {
        return KitRecipes.clicked(self, tier, other, PipeKitRecipes.plateTier(other), PipeKitRecipes.PER_BLOCK[tier], other::shrink, slot, action, player);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                                          net.minecraft.world.entity.player.Player player) {
        ItemStack plates = slot.getItem();
        return KitRecipes.clicked(self, tier, plates, PipeKitRecipes.plateTier(plates), PipeKitRecipes.PER_BLOCK[tier], n -> slot.safeTake(n, n, player), slot, action, player);
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
            else KitItem.setLength(stack, left);
        }
        return InteractionResult.SUCCESS;
    }

    /** Put a part into the block at pos: a new voxel node in air (or anything replaceable), or into one already there. */
    public static String place(Level level, BlockPos pos, VoxelPart part, Player player) {
        BlockState old = level.getBlockState(pos);
        VoxelNodeBlock block = ElnRegistry.VOXEL_NODE_BLOCK.get();
        if (old.getBlock() instanceof VoxelNodeBlock) {
            if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode node)) return "no node here";
            return node.addPart(part);
        }
        boolean mimic = Mimic.allowed(old);   // (a stair, fence, slab...: the voxel block takes its place and imitates it)
        if (!mimic && !(old.isAir() || old.canBeReplaced())) return "the block is solid";
        VoxelNode node = new VoxelNode();
        node.onBlockPlacedBy(new Coordonate(pos, level), mods.eln.misc.Direction.YN, player, ItemStack.EMPTY);
        if (mimic) {
            node.host = old;
            node.elementsChanged();
        }
        String err = node.addPart(part);
        if (err != null) {
            node.host = null;
            node.onBreakBlock();
            return err;
        }
        level.setBlock(pos, mimic ? VoxelNodeBlock.forHost(old) : block.stateFor(level, pos), 3);
        return null;
    }
}
