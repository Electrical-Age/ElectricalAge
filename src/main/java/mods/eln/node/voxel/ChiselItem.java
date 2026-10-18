package mods.eln.node.voxel;

import mods.eln.misc.Coordonate;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.BitSet;

/**
 * The chisel (notes/cables/PLAN-chisel.md; Baughn: carve a hole through a wall for a cable; art; Vintage Story's chisel):
 * left-click takes away a cube of voxels where you aim, right-click puts the block's own material back; the brush is
 * 1, 2, 4 or 8 voxels (grid-aligned), changed with sneak + right-click. A plain block (stone, planks, logs; or a
 * stair, slab, fence...: Mimic) becomes a voxel block imitating it, with the carved voxels gone; cables route through
 * what's free.
 */
public class ChiselItem extends Item implements PipeTool {
    public static final int[] SIZES = {1, 2, 4, 8};

    public ChiselItem(Properties p) {
        super(p.durability(2048));
    }

    public static int size(ItemStack s) {
        int v = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("brush", 1);
        for (int x : SIZES) if (x == v) return v;
        return 1;
    }

    static void cycle(Player player, ItemStack s) {
        int cur = size(s), next = SIZES[0];
        for (int i = 0; i < SIZES.length; i++) if (SIZES[i] == cur) next = SIZES[(i + 1) % SIZES.length];
        int n = next;
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt("brush", n));
        player.sendOverlayMessage(Component.literal("Chisel: " + n + " voxel" + (n > 1 ? "s" : "") + " across"));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId()).append(" (" + size(stack) + ")");
    }

    @Override
    public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) cycle(player, player.getItemInHand(hand));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Right-click: put material back (sneak: change the brush). */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() != null && ctx.getPlayer().isSecondaryUseActive()) {
            if (!ctx.getLevel().isClientSide()) cycle(ctx.getPlayer(), ctx.getItemInHand());
            return InteractionResult.SUCCESS;
        }
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (ctx.getPlayer() instanceof ServerPlayer sp)
            carve(sp, (ServerLevel) ctx.getLevel(), ctx.getClickedPos(), ctx.getClickLocation(), ctx.getClickedFace(), size(ctx.getItemInHand()), false);
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
        if (!(pick instanceof BlockHitResult bh) || !bh.getBlockPos().equals(e.getPos())) return;
        carve(sp, sl, e.getPos(), bh.getLocation(), bh.getDirection(), size(s), true);
    }

    /**
     * Take away (remove) or put back a brush cube at the hit: the voxel just inside the face hit (remove) or just outside
     * it (put back, possibly in the next block). Returns why not, or null.
     */
    public static String carve(ServerPlayer player, ServerLevel level, BlockPos pos, Vec3 hit, net.minecraft.core.Direction face, int size, boolean remove) {
        double eps = remove ? -0.001 : 0.001;
        Vec3 v = hit.add(face.getStepX() * eps, face.getStepY() * eps, face.getStepZ() * eps);
        BlockPos at = BlockPos.containing(v);
        int vx = (int) Math.floor((v.x - at.getX()) * 16), vy = (int) Math.floor((v.y - at.getY()) * 16), vz = (int) Math.floor((v.z - at.getZ()) * 16);
        int x0 = vx / size * size, y0 = vy / size * size, z0 = vz / size * size;
        int[] brush = {x0, y0, z0, x0 + size, y0 + size, z0 + size};
        BlockState st = level.getBlockState(at);
        VoxelNode node = st.getBlock() instanceof VoxelNodeBlock && level.getBlockEntity(at) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n : null;
        if (node == null) {
            if (!remove || !Mimic.carvable(st)) return "can't carve that";
            // a plain block becomes a voxel block imitating it
            node = new VoxelNode();
            node.onBlockPlacedBy(new Coordonate(at, level), mods.eln.misc.Direction.YN, player, ItemStack.EMPTY);
            node.host = st;
            node.carved = new BitSet(4096);
            level.setBlock(at, VoxelNodeBlock.forHost(st), 3);
        }
        if (node.host == null) return "nothing to carve here";
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
