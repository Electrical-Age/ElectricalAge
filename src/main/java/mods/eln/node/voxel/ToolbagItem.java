package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Toolbag (Baughn 2026-11-18, todos #510/#517): a bag of cable and pipe kits (KITS slots) and a pair of shears
 * (SHEARS_SLOT: a left-click cuts cables and pipes back into the bag, ToolbagShears), used in place of a kit. Right-click in the air opens it (ToolbagMenu).
 * Right-click on a cable or pipe takes its kind from the bag and starts or finishes a route as that kit would (an error
 * if the bag holds none); right-click anywhere else to start a route, or sneak-right-click, opens a radial menu of the
 * kinds it holds (client ToolbagRadial; kind or voltage, then cross-section, then colour), the start kept. The route is
 * laid from the bag's kits of the chosen kind together (supply). With a route on, the radial also offers Undo and
 * Cancel (the bag's sneak-click opens the radial instead of undoing).
 */
public class ToolbagItem extends Item implements PipeTool {
    public static final int KITS = 15, SHEARS_SLOT = 15, SLOTS = 16;

    public ToolbagItem(Properties p) {
        // (its kits are listed by kind in its tooltip: not again one by one, as the container's own tooltip would)
        super(p.stacksTo(1).component(DataComponents.TOOLTIP_DISPLAY,
            net.minecraft.world.item.component.TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true)));
    }

    // ---- what it holds

    /** What may go in slot k: a cable or pipe kit; the shears slot vanilla shears. */
    public static boolean accepts(int k, ItemStack s) {
        if (s.isEmpty()) return false;
        if (k == SHEARS_SLOT) return s.getItem() instanceof net.minecraft.world.item.ShearsItem;
        return s.getItem() instanceof CableKitItem || s.getItem() instanceof PipeKitItem;
    }

    public static NonNullList<ItemStack> contents(ItemStack bag) {
        NonNullList<ItemStack> out = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(out);
        return out;
    }

    public static void setContents(ItemStack bag, List<ItemStack> items) {
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /**
     * A kind of kit: a pipe tier, or a cable tier with a power cable's cross-section and colour (-1 for the others).
     */
    public record Kind(boolean pipe, int tier, int mm2, int colour) {
        static Kind of(ItemStack kit) {
            if (kit.getItem() instanceof PipeKitItem p) return new Kind(true, p.tier, -1, -1);
            if (kit.getItem() instanceof CableKitItem c) return cable(c.tier, CableKitItem.specOf(kit));
            return null;
        }

        public static Kind cable(int tier, CableSpec spec) {
            if (CableSpec.power(tier) && spec == null) spec = CableSpec.preset(tier);   // (a cable laid without one: the preset)
            return spec == null ? new Kind(false, tier, -1, -1) : new Kind(false, tier, spec.mm2(), spec.colour());
        }

        CableSpec spec() {
            return mm2 < 0 ? null : new CableSpec(mm2, colour);
        }

        /** The kind's first-ring name: a pipe's, or a cable's voltage or kind. */
        public String first() {
            if (pipe) return new String[]{"Iron pipe", "Alloy pipe", "Large pipe"}[Math.max(0, Math.min(2, tier))];
            return switch (tier) {
                case 0 -> "Signal";
                case 1, 2, 3, 4, CablePart.KV -> CableSpec.voltsLabel(tier);
                case 5 -> "Thermal";
                default -> "Bus";
            };
        }

        /** Its cross-section ("2.5 mm²"), or "". */
        public String size() {
            if (mm2 < 0) return "";
            double m = CableSpec.MM2[mm2];
            return (m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m)) + " mm²";
        }

        /** Its colour's name, or "". */
        public String colourName() {
            return colour < 0 ? "" : net.minecraft.world.item.DyeColor.byId(colour).getName().replace('_', ' ');
        }

        public String label() {
            if (pipe) return first();
            if (mm2 < 0) return first() + " cable";
            return first() + ", " + size() + ", " + colourName();
        }

        int[] ints() {
            return new int[]{pipe ? 1 : 0, tier, mm2, colour};
        }

        static Kind of(int[] a) {
            return a == null || a.length != 4 ? null : new Kind(a[0] == 1, a[1], a[2], a[3]);
        }
    }

    /** The kinds the bag holds, with the voxels of each (in slot order). */
    public static Map<Kind, Integer> kinds(ItemStack bag) {
        Map<Kind, Integer> out = new LinkedHashMap<>();
        var c = contents(bag);
        for (int k = 0; k < KITS; k++) {
            Kind kind = Kind.of(c.get(k));
            if (kind != null) out.merge(kind, KitItem.length(c.get(k)), Integer::sum);
        }
        return out;
    }

    static final String SEL_KEY = "toolbagKind";

    /** The kind chosen last (null if none). */
    public static Kind selected(ItemStack bag) {
        var data = bag.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        return Kind.of(data.copyTag().getIntArray(SEL_KEY).orElse(null));
    }

    public static void select(ItemStack bag, Kind k) {
        CustomData.update(DataComponents.CUSTOM_DATA, bag, t -> t.putIntArray(SEL_KEY, k.ints()));
    }

    /** The bag's kits of its chosen kind as one supply; null if none is chosen or it holds none of it. */
    public static KitSupply supply(ItemStack bag) {
        Kind k = selected(bag);
        if (k == null || !kinds(bag).containsKey(k)) return null;
        return supply(bag, k);
    }

    static KitSupply supply(ItemStack bag, Kind kind) {
        return new KitSupply() {
            public boolean pipe() {
                return kind.pipe();
            }

            public int tier() {
                return kind.tier();
            }

            public CableSpec spec() {
                return kind.spec();
            }

            public int length() {
                return kinds(bag).getOrDefault(kind, 0);
            }

            public void take(int len) {
                var c = contents(bag);
                for (int k = 0; k < KITS && len > 0; k++) {
                    ItemStack s = c.get(k);
                    if (!kind.equals(Kind.of(s))) continue;
                    int have = KitItem.length(s), t = Math.min(have, len);
                    len -= t;
                    if (have - t <= 0) c.set(k, ItemStack.EMPTY);
                    else KitItem.setLength(s, have - t);
                }
                setContents(bag, c);
            }

            @Override
            public String undoSentence() {
                return "Sneak-click for Undo.";
            }

            @Override
            public String cancelClause() {
                return "sneak-click for Undo";
            }

        };
    }

    // ---- using it

    /** A click kept to start a route at once its kind is chosen (the radial): where, and when. */
    record Click(net.minecraft.resources.ResourceKey<Level> dim, BlockPos pos, net.minecraft.core.Direction face, Vec3 hit, long time) {
    }

    /** Per player: the click waiting for the radial's choice; the click the bag's current route started at. */
    static final Map<UUID, Click> PENDING = new java.util.concurrent.ConcurrentHashMap<>(), STARTED = new java.util.concurrent.ConcurrentHashMap<>();
    /** A pending click is kept this long (ticks). */
    static final long PENDING_TICKS = 20 * 30;

    /** Is a click waiting for the radial's choice (GameTests)? */
    public static boolean pendingForTests(UUID p) {
        return PENDING.containsKey(p);
    }

    public static void forget(UUID p) {
        PENDING.remove(p);
        STARTED.remove(p);
    }

    static boolean routing(Player p) {
        return RoutePlacer.active(p.getUUID());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer sp && ToolbagShears.active(sp.getUUID())) {
            ToolbagShears.cancel(sp, "Cut cancelled");
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (sp.isSecondaryUseActive()) openRadial(sp, player.getMainHandItem(), null);
            else ToolbagMenu.open(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;   // (consumed: no inventory opens on a block click)
        if (ctx.getPlayer() instanceof ServerPlayer sp && ctx.getLevel() instanceof ServerLevel sl)
            click(sp, sl, ctx.getItemInHand(), ctx.getClickedPos(), ctx.getClickedFace(), ctx.getClickLocation(), sp.isSecondaryUseActive());
        return InteractionResult.SUCCESS;
    }

    /** A right-click on a block with the bag (sneak: the radial). */
    public static void click(ServerPlayer p, ServerLevel level, ItemStack bag, BlockPos pos, net.minecraft.core.Direction face, Vec3 hit, boolean sneak) {
        if (ToolbagShears.active(p.getUUID())) {   // (a right-click cancels a cut)
            ToolbagShears.cancel(p, "Cut cancelled");
            return;
        }
        Click c = new Click(level.dimension(), pos, face, hit, level.getGameTime());
        boolean routing = routing(p), reach = CablePlacer.reachable(p, pos, hit);
        if (sneak) {   // (always the radial, a route on or not: AdventurAgent saw a sneak-click on a pipe try to lay)
            openRadial(p, bag, routing || !reach ? null : c);
            return;
        }
        if (!reach) {
            KitPreview.note(p, "Out of reach");
            return;
        }
        if (!routing) {
            Kind k = kindAt(level, pos, hit);
            if (k == null) {   // (no cable or pipe there: which kind to lay from here?)
                openRadial(p, bag, c);
                return;
            }
            if (!kinds(bag).containsKey(k)) {
                KitPreview.note(p, "No " + k.label() + " kit in the toolbag");
                return;
            }
            select(bag, k);
            STARTED.put(p.getUUID(), c);
            lay(p, level, supply(bag, k), c);
            return;
        }
        KitSupply s = supply(bag);
        if (s == null) {
            KitPreview.note(p, "The toolbag has no kit of this route's kind left");
            return;
        }
        lay(p, level, s, c);
    }

    static void lay(ServerPlayer p, ServerLevel level, KitSupply s, Click c) {
        RoutePlacer.use(p, level, c.pos(), c.face(), s, c.hit(), false);
    }

    /** The kind of the cable or pipe under the click (a knuckle's cable too), or null. */
    static Kind kindAt(Level level, BlockPos pos, Vec3 hit) {
        if (hit == null || !(level.getBlockEntity(pos) instanceof VoxelNodeEntity e) || !(e.getNode() instanceof VoxelNode n)) return null;
        int idx = n.partAt(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
        if (idx < 0) return null;
        VoxelPart part = n.parts.get(idx);
        if (part instanceof CablePart cp) return Kind.cable(cp.tier, cp.spec);
        if (part instanceof PipePart pp) return new Kind(true, pp.tier, -1, -1);
        return null;
    }

    // ---- the radial: the server sends the kinds it holds, the client sends the choice back

    /** Open the radial on p's client (packetToolbagRadial): the kinds in the bag, the one chosen, whether a route is on. */
    static void openRadial(ServerPlayer p, ItemStack bag, Click start) {
        var kinds = kinds(bag);
        boolean routing = routing(p);
        if (kinds.isEmpty() && !routing) {
            KitPreview.note(p, "The toolbag holds no kits: right-click the air to open it");
            return;
        }
        RoutePlacer.touch(p.getUUID(), p.level().getGameTime());   // (the radial open isn't idleness)
        if (start != null) PENDING.put(p.getUUID(), start);
        else PENDING.remove(p.getUUID());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(mods.eln.Eln.packetToolbagRadial);
            out.writeBoolean(routing);
            Kind sel = selected(bag);
            for (int v : sel == null ? new int[]{-2, -2, -2, -2} : sel.ints()) out.writeByte(v);
            out.writeShort(kinds.size());
            for (var e : kinds.entrySet()) {
                for (int v : e.getKey().ints()) out.writeByte(v);
                out.writeInt(e.getValue());
            }
        } catch (IOException ignored) {
        }
        mods.eln.network.ElnNet.sendToPlayer(p, bos);
    }

    /** The radial's actions (packetToolbagChoice's first byte). */
    public static final int CHOOSE = 0, UNDO = 1, CANCEL = 2;

    /** packetToolbagChoice from p's client: CHOOSE (kind's four bytes), UNDO or CANCEL. */
    public static void choice(ServerPlayer p, DataInputStream in) throws IOException {
        int action = in.readByte();
        int[] k = action == CHOOSE ? new int[]{in.readByte(), in.readByte(), in.readByte(), in.readByte()} : null;
        choice(p, action, Kind.of(k));
    }

    public static void choice(ServerPlayer p, int action, Kind kind) {
        ItemStack bag = p.getMainHandItem();
        if (!(bag.getItem() instanceof ToolbagItem) || !(p.level() instanceof ServerLevel level)) return;
        Click pending = PENDING.remove(p.getUUID());
        RoutePlacer.touch(p.getUUID(), level.getGameTime());
        if (action == UNDO || action == CANCEL) {
            if (action == CANCEL) {
                boolean had = RoutePlacer.drop(p.getUUID()) != null;
                STARTED.remove(p.getUUID());
                if (had) KitPreview.note(p, "Route cancelled");
            } else RoutePlacer.undo(p, level);
            return;
        }
        if (kind == null || !kinds(bag).containsKey(kind)) {
            KitPreview.note(p, "The toolbag holds no such kit");
            return;
        }
        Kind was = selected(bag);
        select(bag, kind);
        KitSupply s = supply(bag, kind);
        if (routing(p)) {
            // a route on: the new kind from the same start, its waypoints kept where they fit: a cable's for a cable, a
            // pipe's for a pipe (points on a face, points in a block's middle: they don't carry over between the two)
            if (kind.equals(was)) return;
            Click start = STARTED.get(p.getUUID());
            RoutePlacer.Plan old = RoutePlacer.drop(p.getUUID());   // (not null: a route is on)
            if (start == null || start.dim() != level.dimension()) {
                KitPreview.note(p, "Route dropped: start it again with the new kind");
                return;
            }
            lay(p, level, s, start);
            if (old.kind == RoutePlacer.of(s)) {
                // (over the start message: the route goes on, as a new kind; AdventurAgent read "route started" as a restart.
                // A start refused at the new kind keeps its own reason)
                if (!RoutePlacer.active(p.getUUID())) return;
                RoutePlacer.addWaypoints(p.getUUID(), old.waypoints);
                p.sendOverlayMessage(Component.literal("Now " + kind.label() + (old.waypoints.isEmpty() ? "." : "; waypoints kept.")));
            } else p.sendOverlayMessage(Component.literal("Restarted as " + kind.label() + " \u2014 waypoints cleared"));
            return;
        }
        if (pending == null) {
            p.sendOverlayMessage(Component.literal("Toolbag: " + kind.label()));
            return;
        }
        // the click the radial was opened from: the route starts there now, if it's still fresh and in reach
        if (pending.dim() != level.dimension() || level.getGameTime() - pending.time() > PENDING_TICKS
            || p.position().distanceToSqr(pending.hit()) > 10 * 10) {
            p.sendOverlayMessage(Component.literal("Toolbag: " + kind.label() + " (click again to start)"));
            return;
        }
        STARTED.put(p.getUUID(), pending);
        lay(p, level, s, pending);
    }

    /** The packetToolbagChoice bytes for an action (the client's radial). */
    public static byte[] choicePacket(int action, int[] kind) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(mods.eln.Eln.packetToolbagChoice);
        bos.write(action);
        if (kind != null) for (int v : kind) bos.write(v);
        return bos.toByteArray();
    }

    // ---- display

    @Override
    public Component getName(ItemStack stack) {
        Kind k = selected(stack);
        var n = Component.translatable(getDescriptionId());
        return k == null ? n : n.append(" (" + k.label() + ")");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        var kinds = kinds(stack);
        for (var e : kinds.entrySet())
            out.accept(Component.literal(String.format("%s: %.1f blocks", e.getKey().label(), e.getValue() / 16.0)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (kinds.isEmpty()) out.accept(Component.literal("No kits").withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("Right-click the air to open; a cable: lay its kind; elsewhere or sneaking: choose").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
