package mods.eln.node.voxel;

import mods.eln.misc.Coordonate;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.BitSet;

/**
 * The chisel (notes/cables/PLAN-chisel.md; Baughn: carve a hole through a wall for a cable; art; Vintage Story's chisel):
 * left-click takes away the brush where you aim, right-click (sneaking or not) puts the block's own material back.
 * The brush is a size (1, 2, 4 or 8 voxels, grid-aligned) and a shape (ChiselBrush.Shape: cube, flat, tunnel), chosen
 * in the radial menu the wrench key opens while the chisel is held (Baughn: sneak + right-click cycling was
 * undiscoverable), sent as packetChiselBrush. A plain block (stone, planks, logs; or a stair, slab, fence...: Mimic)
 * becomes a voxel block imitating it, with the carved voxels gone; cables route through what's free.
 */
public class ChiselItem extends Item implements PipeTool {
    public static final int[] SIZES = ChiselBrush.SIZES;
    /** Custom-data keys: "brush" (the size, as before shapes existed) and "shape" (ChiselBrush.Shape ordinal). */
    static final String SIZE_KEY = "brush", SHAPE_KEY = "shape";

    public ChiselItem(Properties p) {
        super(p.durability(2048));
    }

    public static int size(ItemStack s) {
        int v = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(SIZE_KEY, 1);
        return ChiselBrush.validSize(v) ? v : 1;
    }

    public static ChiselBrush.Shape shape(ItemStack s) {
        var sh = ChiselBrush.Shape.byId(s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(SHAPE_KEY, 0));
        return sh == null ? ChiselBrush.Shape.CUBE : sh;
    }

    public static void setBrush(ItemStack s, int size, ChiselBrush.Shape shape) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> {
            t.putInt(SIZE_KEY, size);
            t.putInt(SHAPE_KEY, shape.ordinal());
        });
    }

    /**
     * packetChiselBrush from a client: sets the brush of the chisel in the player's main hand. Ignored (false) unless
     * that is a chisel and the size and shape are valid; the stack is the server's, never named by the packet.
     */
    public static boolean applyBrush(Player player, int size, int shape) {
        ItemStack s = player.getMainHandItem();
        if (!(s.getItem() instanceof ChiselItem) || !ChiselBrush.valid(size, shape)) return false;
        setBrush(s, size, ChiselBrush.Shape.byId(shape));
        return true;
    }

    /** The packetChiselBrush bytes asking for this brush (sent by the client's radial menu). */
    public static byte[] brushPacket(int size, ChiselBrush.Shape shape) {
        return new byte[]{mods.eln.Eln.packetChiselBrush, (byte) size, (byte) shape.ordinal()};
    }

    public static Component shapeName(ChiselBrush.Shape shape) {
        return Component.translatable("item.eln.chisel.shape." + shape.key());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(" (" + size(stack) + ", ").append(shapeName(shape(stack))).append(")");
    }

    /**
     * At most one stroke per {@link #STROKE_TICKS} per player (Baughn: holding the button down lagged the game; a
     * cancelled left-click is sent again every tick while held, and each stroke re-publishes and re-meshes the block).
     */
    static final int STROKE_TICKS = 4;
    private static final java.util.Map<java.util.UUID, Long> LAST_STROKE = new java.util.concurrent.ConcurrentHashMap<>();

    static boolean strokeReady(ServerPlayer p, ServerLevel level) {
        long now = level.getGameTime();
        Long last = LAST_STROKE.get(p.getUUID());
        if (last != null && now - last < STROKE_TICKS && now >= last) return false;
        LAST_STROKE.put(p.getUUID(), now);
        return true;
    }

    public static void forget(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_STROKE.remove(e.getEntity().getUUID());
    }

    /** Right-click: put material back. */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (ctx.getPlayer() instanceof ServerPlayer sp && strokeReady(sp, (ServerLevel) ctx.getLevel()))
            tell(sp, carve(sp, (ServerLevel) ctx.getLevel(), ctx.getClickedPos(), ctx.getClickLocation(), ctx.getClickedFace(), size(ctx.getItemInHand()),
                shape(ctx.getItemInHand()), false));
        return InteractionResult.SUCCESS;
    }

    /** Left-click with a chisel: carve instead of mining (both sides cancel the mining). */
    public static void onLeftClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) {
        ItemStack s = e.getEntity().getMainHandItem();
        if (!(s.getItem() instanceof ChiselItem)) return;
        e.setCanceled(true);
        if (e.getAction() != net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!(e.getEntity() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel sl)) return;
        var pick = sp.pick(sp.blockInteractionRange(), 1f, false);
        if (!(pick instanceof BlockHitResult bh) || !bh.getBlockPos().equals(e.getPos()) || !strokeReady(sp, sl)) return;
        tell(sp, carve(sp, sl, e.getPos(), bh.getLocation(), bh.getDirection(), size(s), shape(s), true));
    }

    /** Why a stroke did nothing, on the action bar (#163: a chisel on a block it can't work was silent). */
    static void tell(ServerPlayer sp, String why) {
        if (why != null) sp.sendOverlayMessage(net.minecraft.network.chat.Component.literal(why));
    }

    /** carve with a cube brush. */
    public static String carve(ServerPlayer player, ServerLevel level, BlockPos pos, Vec3 hit, net.minecraft.core.Direction face, int size, boolean remove) {
        return carve(player, level, pos, hit, face, size, ChiselBrush.Shape.CUBE, remove);
    }

    /** The block a stroke at this hit works on: just inside the face hit (remove) or just outside it (put back). */
    public static BlockPos target(Vec3 hit, net.minecraft.core.Direction face, boolean remove) {
        return BlockPos.containing(inside(hit, face, remove));
    }

    static Vec3 inside(Vec3 hit, net.minecraft.core.Direction face, boolean remove) {
        double eps = remove ? -0.001 : 0.001;
        return hit.add(face.getStepX() * eps, face.getStepY() * eps, face.getStepZ() * eps);
    }

    /** The brush box (ChiselBrush.box) a stroke at this hit covers in target(hit, face, remove). */
    public static int[] brushBox(Vec3 hit, net.minecraft.core.Direction face, int size, ChiselBrush.Shape shape, boolean remove) {
        Vec3 v = inside(hit, face, remove);
        BlockPos at = BlockPos.containing(v);
        int vx = (int) Math.floor((v.x - at.getX()) * 16), vy = (int) Math.floor((v.y - at.getY()) * 16), vz = (int) Math.floor((v.z - at.getZ()) * 16);
        return ChiselBrush.box(shape, size, face.getAxis().ordinal(), vx, vy, vz);
    }

    /**
     * Take away (remove) or put back the brush at the hit, in the block just inside the face hit (remove) or just
     * outside it (put back, possibly the next block). Returns why not, or null.
     */
    public static String carve(ServerPlayer player, ServerLevel level, BlockPos pos, Vec3 hit, net.minecraft.core.Direction face, int size,
                               ChiselBrush.Shape shape, boolean remove) {
        BlockPos at = target(hit, face, remove);
        int[] brush = brushBox(hit, face, size, shape, remove);
        BlockState st = level.getBlockState(at);
        VoxelNode node = st.getBlock() instanceof VoxelNodeBlock && level.getBlockEntity(at) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n : null;
        if (node == null) {
            if (!remove) return st.isAir() ? "nothing carved here to put back" : "put back only into a carved block";
            if (!Mimic.carvable(st)) return Mimic.whyNotCarvable(st);
            // a plain block becomes a voxel block imitating it
            node = new VoxelNode();
            node.onBlockPlacedBy(new Coordonate(at, level), mods.eln.misc.Direction.YN, player, ItemStack.EMPTY);
            node.host = st;
            node.carved = new BitSet(4096);
            level.setBlock(at, VoxelNodeBlock.forHost(st), 3);
        }
        if (node.host == null) return remove ? "nothing to carve here: no block around these parts" : "nothing carved here to put back";
        if (node.carved == null) node.carved = new BitSet(4096);
        BitSet all = Mimic.voxels(Mimic.boxes(node.host, level, at));
        BitSet region = Mimic.voxels(java.util.List.of(brush));
        region.and(all);
        if (remove) {
            region.andNot(node.carved);
            if (region.isEmpty()) return "already carved";
            node.carved.or(region);
        } else {
            region.and(node.carved);
            region.andNot(node.partsVoxels());   // (not into cables or devices)
            if (region.isEmpty()) return "nothing to put back there";
            node.carved.andNot(region);
        }
        node.elementsChanged();
        if (!player.isCreative()) player.getMainHandItem().hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        BitSet left = (BitSet) all.clone();
        left.andNot(node.carved);
        if (left.isEmpty() && node.isEmptyNode()) {   // (all carved away, nothing else here: air; the material is gone)
            node.host = null;
            node.removeEmptyBlock();
        } else if (node.carved.isEmpty() && node.isEmptyNode()) node.removeEmptyBlock();   // (whole again: the plain block)
        return null;
    }
}
