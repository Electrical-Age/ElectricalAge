package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Toolbag's shears (Baughn 2026-11-18, #517): with shears in the bag's shears slot, a left-click on a cable or pipe
 * starts a cut there; the run from that piece to the one under the crosshair is outlined in red as the aim moves along
 * it (KitPreview's outline packet; the run stops at a junction or a device); a second left-click takes those pieces
 * out and puts their length back into the bag as kits of their kind (topping up its kits of that kind first; what
 * doesn't fit drops at the player), at one point of the shears' durability. A right-click cancels. A cable's run follows
 * its joints (CableLinks CONT); a pipe's follows its ends, block to block, stopping at fittings.
 */
public final class ToolbagShears {
    private ToolbagShears() {
    }

    /** A piece of the run: its block and the part (by identity). */
    record Piece(BlockPos pos, VoxelPart part) {
    }

    /** A cut under way: the run through the first piece clicked (in order), that piece's index, and the outlined span. */
    static final class Cut {
        final net.minecraft.resources.ResourceKey<Level> dim;
        final List<Piece> run;
        final int anchor;
        int from, to;

        Cut(net.minecraft.resources.ResourceKey<Level> dim, List<Piece> run, int anchor) {
            this.dim = dim;
            this.run = run;
            this.anchor = anchor;
            from = to = anchor;
        }

        List<Piece> span() {
            return run.subList(from, to + 1);
        }
    }

    static final Map<UUID, Cut> CUTS = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean active(UUID p) {
        return CUTS.containsKey(p);
    }

    public static void forget(UUID p) {
        CUTS.remove(p);
    }

    /** A left-click on a block: with the bag in hand, no block is broken; it starts or finishes a cut. */
    public static void onLeftClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) {
        ItemStack bag = e.getEntity().getMainHandItem();
        if (!(bag.getItem() instanceof ToolbagItem)) return;
        e.setCanceled(true);
        if (e.getAction() != net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!(e.getEntity() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel sl)) return;
        var pick = sp.pick(sp.blockInteractionRange(), 1f, false);
        Vec3 hit = pick instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK && bh.getBlockPos().equals(e.getPos()) ? bh.getLocation() : null;
        leftClick(sp, sl, bag, e.getPos(), hit);
    }

    /** A left-click with the bag at hit in the block at pos (null hit: not on it). */
    public static void leftClick(ServerPlayer p, ServerLevel level, ItemStack bag, BlockPos pos, Vec3 hit) {
        Cut c = CUTS.get(p.getUUID());
        if (c != null) {
            if (hit != null) aim(p, level, pos, hit);
            cut(p, level, bag, c);
            return;
        }
        if (hit == null) return;
        Piece at = pieceAt(level, pos, hit);
        if (at == null) return;   // (not a cable or pipe: nothing to cut, and no block broken)
        if (shears(bag).isEmpty()) {
            KitPreview.note(p, mods.eln.i18n.I18N.tr("No shears in toolbag"));
            return;
        }
        List<Piece> run = run(level, at);
        int anchor = indexOf(run, at.part());
        Cut cut = new Cut(level.dimension(), run, anchor);
        CUTS.put(p.getUUID(), cut);
        show(p, cut);
    }

    /** Each tick (KitPreview.tick): the span follows the aim; the cut ends if the bag leaves the hand. True while cutting. */
    static boolean tick(ServerPlayer p, ServerLevel level) {
        Cut c = CUTS.get(p.getUUID());
        if (c == null) return false;
        if (!(p.getMainHandItem().getItem() instanceof ToolbagItem) || c.dim != level.dimension()) {
            cancel(p, mods.eln.i18n.I18N.tr("Cut cancelled"));
            return false;
        }
        if (p.pick(p.blockInteractionRange(), 1f, false) instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK)
            aim(p, level, bh.getBlockPos(), bh.getLocation());
        return true;
    }

    /** Aimed at hit in pos: if it is a piece of the run, the span reaches from the first piece to it. */
    static void aim(ServerPlayer p, ServerLevel level, BlockPos pos, Vec3 hit) {
        Cut c = CUTS.get(p.getUUID());
        if (c == null) return;
        Piece at = pieceAt(level, pos, hit);
        if (at == null) return;
        int i = indexOf(c.run, at.part());
        if (i < 0) return;
        int from = Math.min(i, c.anchor), to = Math.max(i, c.anchor);
        if (from == c.from && to == c.to) return;
        c.from = from;
        c.to = to;
        show(p, c);
    }

    /** aim, for GameTests. */
    public static void aimForTests(ServerPlayer p, ServerLevel level, BlockPos pos, Vec3 hit) {
        aim(p, level, pos, hit);
    }

    /** The span outlined (red), and on the action bar how much it is and what to do. */
    static void show(ServerPlayer p, Cut c) {
        List<int[]> boxes = new ArrayList<>();
        int len = 0;
        for (Piece pc : c.span()) {
            for (int[] b : pc.part().boxes())
                boxes.add(new int[]{pc.pos().getX() * 16 + b[0], pc.pos().getY() * 16 + b[1], pc.pos().getZ() * 16 + b[2],
                    pc.pos().getX() * 16 + b[3], pc.pos().getY() * 16 + b[4], pc.pos().getZ() * 16 + b[5]});
            len += length(pc.part());
        }
        KitPreview.send(p, false, boxes);
        p.sendOverlayMessage(Component.literal(mods.eln.i18n.I18N.tr("Cut: %1$ blocks", String.format("%.1f", len / 16.0))));
    }

    static int length(VoxelPart part) {
        return part instanceof CablePart cp ? cp.isKnuckle() ? 0 : cp.length() : part instanceof PipePart pp ? pp.length() : 0;
    }

    public static void cancel(ServerPlayer p, String why) {
        if (CUTS.remove(p.getUUID()) == null) return;
        KitPreview.send(p, true, List.of());
        KitPreview.forget(p.getUUID());
        p.sendOverlayMessage(Component.literal(why));
    }

    /** Take the span out, its length back into the bag, a point off the shears. */
    static void cut(ServerPlayer p, ServerLevel level, ItemStack bag, Cut c) {
        CUTS.remove(p.getUUID());
        KitPreview.send(p, true, List.of());
        KitPreview.forget(p.getUUID());
        if (shears(bag).isEmpty()) {
            KitPreview.note(p, mods.eln.i18n.I18N.tr("No shears in toolbag"));
            return;
        }
        List<ItemStack> back = new ArrayList<>();
        int len = 0, pieces = 0;
        for (Piece pc : c.span()) {
            if (!(level.getBlockEntity(pc.pos()) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) continue;
            int idx = -1;
            for (int i = 0; i < n.parts.size(); i++) if (n.parts.get(i) == pc.part()) idx = i;
            if (idx < 0) continue;   // (gone since)
            ItemStack item = VoxelNode.partItem(pc.part());
            if (n.removePart(idx, false)) n.removeEmptyBlock();
            pieces++;
            len += length(pc.part());
            if (!item.isEmpty()) back.add(item);
        }
        if (pieces == 0) return;
        // the shears: a point of wear per cut
        var c0 = ToolbagItem.contents(bag);
        ItemStack sh = c0.get(ToolbagItem.SHEARS_SLOT);
        if (sh.isDamageableItem()) {
            sh.setDamageValue(sh.getDamageValue() + 1);
            if (sh.getDamageValue() >= sh.getMaxDamage()) {
                c0.set(ToolbagItem.SHEARS_SLOT, ItemStack.EMPTY);
                p.sendOverlayMessage(Component.literal(mods.eln.i18n.I18N.tr("Toolbag shears broke")));
            }
        }
        ToolbagItem.setContents(bag, c0);
        int dropped = 0;
        for (ItemStack k : back) dropped += give(p, bag, k);
        p.sendOverlayMessage(Component.literal(dropped > 0
            ? mods.eln.i18n.I18N.tr("Cut %1$ blocks (%2$ dropped — bag full)", String.format("%.1f", len / 16.0), String.format("%.1f", dropped / 16.0))
            : mods.eln.i18n.I18N.tr("Cut %1$ blocks", String.format("%.1f", len / 16.0))));
    }

    /** Kit k into the bag: its kits of that kind topped up, then empty kit slots; the rest dropped at p. Voxels dropped. */
    static int give(ServerPlayer p, ItemStack bag, ItemStack k) {
        var c = ToolbagItem.contents(bag);
        int len = KitItem.length(k);
        for (int i = 0; i < ToolbagItem.KITS && len > 0; i++) {
            ItemStack rest = k.copy();
            KitItem.setLength(rest, len);
            len -= KitItem.pour(rest, c.get(i));
        }
        for (int i = 0; i < ToolbagItem.KITS && len > 0; i++) {
            if (!c.get(i).isEmpty()) continue;
            int t = Math.min(KitItem.MAX, len);
            c.set(i, ((KitItem) k.getItem()).make(k, t));
            len -= t;
        }
        ToolbagItem.setContents(bag, c);
        int dropped = len;
        while (len > 0) {
            int t = Math.min(KitItem.MAX, len);
            p.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(),
                ((KitItem) k.getItem()).make(k, t)));
            len -= t;
        }
        return dropped;
    }

    static ItemStack shears(ItemStack bag) {
        ItemStack s = ToolbagItem.contents(bag).get(ToolbagItem.SHEARS_SLOT);
        return s.getItem() instanceof net.minecraft.world.item.ShearsItem ? s : ItemStack.EMPTY;
    }

    // ---- the run

    static Piece pieceAt(Level level, BlockPos pos, Vec3 hit) {
        if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) return null;
        int idx = n.partAt(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
        if (idx < 0) return null;
        VoxelPart part = n.parts.get(idx);
        return part instanceof CablePart || part instanceof PipePart ? new Piece(pos, part) : null;
    }

    static int indexOf(List<Piece> run, VoxelPart part) {
        for (int i = 0; i < run.size(); i++) if (run.get(i).part() == part) return i;
        return -1;
    }

    static VoxelNode node(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n : null;
    }

    /** The run through piece at, in order along it. */
    static List<Piece> run(Level level, Piece at) {
        Set<VoxelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(at.part());
        List<Piece> left = walk(level, at, 0, seen), right = walk(level, at, 1, seen);
        List<Piece> out = new ArrayList<>();
        for (int i = left.size() - 1; i >= 0; i--) out.add(left.get(i));
        out.add(at);
        out.addAll(right);
        return out;
    }

    static List<Piece> walk(Level level, Piece from, int end, Set<VoxelPart> seen) {
        List<Piece> out = new ArrayList<>();
        Piece cur = from;
        int e = end;
        for (int guard = 0; guard < 4096; guard++) {
            int[] next = cur.part() instanceof CablePart cp ? nextCable(level, cur.pos(), cp, e) : nextPipe(level, cur.pos(), (PipePart) cur.part(), e);
            if (next == null) break;
            BlockPos np = new BlockPos(next[0], next[1], next[2]);
            VoxelNode n = node(level, np);
            if (n == null || next[3] < 0 || next[3] >= n.parts.size()) break;
            VoxelPart part = n.parts.get(next[3]);
            if (seen.contains(part)) break;
            if (part instanceof CablePart cp && spliced(level, np, cp)) break;   // (a branch joins its body: a junction)
            seen.add(part);
            cur = new Piece(np, part);
            out.add(cur);
            e = 1 - next[4];
        }
        return out;
    }

    /** The next piece of a cable's run past end e: {x, y, z, part index, its end}, or null (none, several, a device). */
    static int[] nextCable(Level level, BlockPos pos, CablePart c, int e) {
        int[] only = null;
        for (int[] j : c.joints) {
            if (j[1] != e) continue;
            int k = CableLinks.kind(j);
            if (k == CableLinks.TERM_ELEMENT || k == CableLinks.TERM_NEIGHBOUR || k == CableLinks.TERM_FITTING || k == CableLinks.SPLICE) return null;
            if (k != CableLinks.CONT) continue;
            if (only != null) return null;   // (three ends meet: a junction)
            only = j;
        }
        if (only == null) return null;
        BlockPos np = pos.offset(only[2], only[3], only[4]);
        VoxelNode n = node(level, np);
        if (n == null) return null;
        for (int i = 0; i < n.parts.size(); i++)
            if (n.parts.get(i) instanceof CablePart o && o.id == only[5]) return new int[]{np.getX(), np.getY(), np.getZ(), i, only[6]};
        return null;
    }

    /** Does another piece's end splice onto c's body (in its block or a neighbour)? */
    static boolean spliced(Level level, BlockPos pos, CablePart c) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) continue;
                    VoxelNode n = node(level, pos.offset(dx, dy, dz));
                    if (n == null) continue;
                    for (VoxelPart q : n.parts)
                        if (q instanceof CablePart o)
                            for (int[] j : o.joints)
                                if (CableLinks.kind(j) == CableLinks.SPLICE && j[2] == -dx && j[3] == -dy && j[4] == -dz && j[5] == c.id) return true;
                }
        return false;
    }

    /** The next pipe past end e (0 its first point, 1 its last): the one pipe whose end meets it, in this block or across
     *  the face it is on; null if none or several, or a fitting or another pipe's body is there. */
    static int[] nextPipe(Level level, BlockPos pos, PipePart p, int e) {
        int[] q = e == 0 ? new int[]{p.points[0], p.points[1], p.points[2]}
            : new int[]{p.points[p.points.length - 3], p.points[p.points.length - 2], p.points[p.points.length - 1]};
        BlockPos np = pos;
        int[] m = q.clone();
        for (int k = 0; k < 3; k++) {
            if (q[k] != 0 && q[k] != 16) continue;
            int[] d = new int[3];
            d[k] = q[k] == 0 ? -1 : 1;
            np = pos.offset(d[0], d[1], d[2]);
            m[k] = 16 - q[k];
            break;
        }
        VoxelNode here = node(level, pos), there = node(level, np);
        if (there == null || fittingAt(here, q) || fittingAt(there, m)) return null;
        int[] found = null;
        for (int i = 0; i < there.parts.size(); i++) {
            if (!(there.parts.get(i) instanceof PipePart o) || o == p) continue;
            int oe = o.points[0] == m[0] && o.points[1] == m[1] && o.points[2] == m[2] ? 0
                : o.points[o.points.length - 3] == m[0] && o.points[o.points.length - 2] == m[1] && o.points[o.points.length - 1] == m[2] ? 1 : -1;
            if (oe < 0) {
                if (o.passes(m[0], m[1], m[2])) return null;   // (onto another pipe's body)
                continue;
            }
            if (found != null) return null;
            found = new int[]{np.getX(), np.getY(), np.getZ(), i, oe};
        }
        return found;
    }

    static boolean fittingAt(VoxelNode n, int[] q) {
        if (n == null) return false;
        for (VoxelPart part : n.parts) {
            if (part instanceof PipePart || part instanceof CablePart) continue;
            for (int[] b : part.boxes()) if (VoxelPart.contains(b, q[0] / 16.0, q[1] / 16.0, q[2] / 16.0, 0.05)) return true;
        }
        return false;
    }
}
