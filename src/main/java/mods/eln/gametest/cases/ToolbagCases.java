package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CablePlacer;
import mods.eln.node.voxel.KitItem;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.PipePlacer;
import mods.eln.node.voxel.ToolbagItem;
import mods.eln.node.voxel.ToolbagMenu;
import mods.eln.node.voxel.VoxelNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Toolbag (Baughn 2026-11-18, todos #510/#517): its slots take only kits (and shears in theirs); a click on a cable
 * lays that cable's kind from the bag's kits of it together; a cable kind it lacks is refused; a click on an empty floor
 * waits for the radial's choice, which starts the route there; a new kind mid-route keeps the start; a pipe click takes
 * the pipe kind; Cancel drops the route.
 */
public final class ToolbagCases {
    private ToolbagCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new SlotsCase());
        cases.add(new LayCase());
        cases.add(new ReachSneakUndoCase());
        cases.add(new ShearsCase());
    }

    static final int SIGNAL = CablePart.tierOf("Signal Cable"), LV = CablePart.tierOf("Low Voltage Cable"), MV = CablePart.tierOf("Medium Voltage Cable");

    static ItemStack bag(ItemStack... kits) {
        ItemStack bag = new ItemStack(mods.eln.registry.ElnRegistry.TOOLBAG.get());
        List<ItemStack> items = new ArrayList<>();
        for (int k = 0; k < ToolbagItem.SLOTS; k++) items.add(k < kits.length ? kits[k] : ItemStack.EMPTY);
        ToolbagItem.setContents(bag, items);
        return bag;
    }

    /** The voxels of tier's (preset) cable kits in the bag, and how many kits. */
    static int[] held(ItemStack bag, int tier) {
        int len = 0, n = 0;
        for (ItemStack s : ToolbagItem.contents(bag))
            if (s.getItem() instanceof CableKitItem k && k.tier == tier) {
                len += KitItem.lengthForTests(s);
                n++;
            }
        return new int[]{len, n};
    }

    static VoxelNode node(TestContext c, BlockPos p) {
        return mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level())) instanceof VoxelNode v ? v : null;
    }

    /** Tiers of the cable pieces in the block at p. */
    static String tiers(TestContext c, BlockPos p) {
        var v = node(c, p);
        if (v == null) return "none";
        StringBuilder sb = new StringBuilder();
        for (var part : v.parts) if (part instanceof CablePart cp) sb.append(cp.tier).append(' ');
        return sb.toString().trim();
    }

    /** The top of the floor block under p, at its middle (a click on it). */
    static void floorClick(TestContext c, ServerPlayer pl, ItemStack bag, BlockPos p) {
        ToolbagItem.click(pl, c.level(), bag, p.below(), Direction.UP, Vec3.atLowerCornerOf(p).add(0.5, 0, 0.5), false);
    }

    /** The radial's choice as the client sends it: ToolbagRadial's packet bytes into the server's packet handler. */
    static void radial(ServerPlayer pl, int action, int[] kind) {
        mods.eln.network.ServerPacketHandler.handle(ToolbagItem.choicePacket(action, kind), pl);
    }

    static ServerPlayer player(TestContext c) {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
    }

    static void clean(ServerPlayer pl) {
        pl.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CablePlacer.forget(pl.getUUID());
        PipePlacer.forget(pl.getUUID());
        ToolbagItem.forget(pl.getUUID());
    }

    /** Its kit slots take cable and pipe kits only; its shears slot vanilla shears only. */
    static final class SlotsCase implements TestCase {
        String got = "";
        boolean ok;

        public String id() {
            return "toolbag_takes_only_kits";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            var pl = player(c);
            ItemStack bag = bag();
            var m = ToolbagMenu.forTests(pl.getInventory(), bag, 0);
            var kitSlot = m.slots.get(0);
            var shearsSlot = m.slots.get(ToolbagItem.SHEARS_SLOT);
            boolean cable = kitSlot.mayPlace(CableKitItem.kit(LV, 100)), pipe = kitSlot.mayPlace(PipeKitItem.kit(0, 100));
            boolean dirt = kitSlot.mayPlace(new ItemStack(Items.DIRT)), shearsInKit = kitSlot.mayPlace(new ItemStack(Items.SHEARS));
            boolean shears = shearsSlot.mayPlace(new ItemStack(Items.SHEARS)), kitInShears = shearsSlot.mayPlace(CableKitItem.kit(LV, 100));
            boolean bagInBag = kitSlot.mayPlace(bag());
            got = String.format("cable %b, pipe %b, dirt %b, shears in a kit slot %b, shears %b, kit in the shears slot %b, a bag %b; %d slots",
                cable, pipe, dirt, shearsInKit, shears, kitInShears, bagInBag, ToolbagItem.SLOTS);
            ok = cable && pipe && !dirt && !shearsInKit && shears && !kitInShears && !bagInBag && ToolbagItem.KITS == 15;
            // a kit put in through the menu is in the bag
            kitSlot.set(CableKitItem.kit(LV, 100));
            ok &= ToolbagItem.kinds(bag).size() == 1;
            got += ", kinds after one in: " + ToolbagItem.kinds(bag).keySet();
        }

        public void measure(TestContext c) {
            c.check("toolbag: its slots take only kits, the shears slot only shears", ok, got);
        }
    }

    /**
     * On a floor: an LV cable in (1,1,1). A bag of two 40-voxel LV kits (and a signal kit) clicked on it, then twice on the
     * floor at (4,1,1): an LV cable laid from it, more than one kit's length, drawn from both kits (the first used up).
     * A signal cable at (1,1,3) clicked with a bag that has none: no route. An empty floor at (1,1,5) clicked: no route
     * yet; the radial's 230 V choice starts it there; then 48 V chosen mid-route keeps the start; ended at (5,1,5): 48 V
     * laid. An iron pipe at (7,1,1) clicked: a pipe route with the pipe kind; Cancel drops it.
     */
    static final class LayCase implements TestCase {
        String err = "", laid = "", refused = "", radial = "", pipe = "";
        boolean laidOk, refusedOk, radialOk, pipeOk;

        public String id() {
            return "toolbag_lays_from_its_kits";
        }

        public int width() {
            return 9;
        }

        public int height() {
            return 7;
        }

        public int depth() {
            return 7;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) c.floor(c.at(x, 0, z));
            var pl = player(c);
            var level = c.level();
            // (the FakePlayer stands by the rig, as a player laying routes does: a route lapses far from its last point)
            double px = pl.getX(), py = pl.getY(), pz = pl.getZ();
            BlockPos mid = c.at(4, 1, 3);
            pl.snapTo(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
            try {
                // 1. the clicked cable's kind, from two kits
                BlockPos a = c.at(1, 1, 1);
                mods.eln.node.voxel.PipeKitItem.place(level, a, new CablePart(LV, CableCases.DOWN, new int[]{0, 0, 8, 16, 0, 8}), null);
                ItemStack bag = bag(CableKitItem.kit(LV, 40), CableKitItem.kit(SIGNAL, 200), CableKitItem.kit(LV, 40));
                pl.setItemInHand(InteractionHand.MAIN_HAND, bag);
                ToolbagItem.click(pl, level, bag, a, Direction.UP, Vec3.atLowerCornerOf(a).add(0.5, 0.5 / 16, 0.5), false);
                boolean started = CablePlacer.active(pl.getUUID());
                var sel = ToolbagItem.selected(bag);
                BlockPos far = c.at(4, 1, 1);
                floorClick(c, pl, bag, far);
                floorClick(c, pl, bag, far);
                int[] h = held(bag, LV);
                laid = String.format("started %b, kind %s, at (3,1,1) tiers [%s], LV left %d in %d kit(s), signal %d", started, sel == null ? null : sel.label(),
                    tiers(c, c.at(3, 1, 1)), h[0], h[1], held(bag, SIGNAL)[0]);
                laidOk = started && sel != null && !sel.pipe() && sel.tier() == LV && tiers(c, c.at(3, 1, 1)).equals(String.valueOf(LV))
                    && h[0] < 40 && h[1] == 1 && held(bag, SIGNAL)[0] == 200 && !CablePlacer.active(pl.getUUID());

                clean(pl);
                // 2. a kind it lacks
                BlockPos s = c.at(1, 1, 3);
                mods.eln.node.voxel.PipeKitItem.place(level, s, new CablePart(SIGNAL, CableCases.DOWN, new int[]{0, 0, 8, 16, 0, 8}), null);
                ItemStack noSignal = bag(CableKitItem.kit(LV, 300));
                pl.setItemInHand(InteractionHand.MAIN_HAND, noSignal);
                ToolbagItem.click(pl, level, noSignal, s, Direction.UP, Vec3.atLowerCornerOf(s).add(0.5, 0.5 / 16, 0.5), false);
                refusedOk = !CablePlacer.active(pl.getUUID()) && ToolbagItem.selected(noSignal) == null;
                refused = "route " + CablePlacer.active(pl.getUUID()) + ", kind " + ToolbagItem.selected(noSignal);

                clean(pl);
                // 3. the radial: an empty floor, then 230 V, then 48 V mid-route
                ItemStack two = bag(CableKitItem.kit(MV, 300), CableKitItem.kit(LV, 300));
                pl.setItemInHand(InteractionHand.MAIN_HAND, two);
                BlockPos e = c.at(1, 1, 5);
                floorClick(c, pl, two, e);
                boolean waited = !CablePlacer.active(pl.getUUID());
                var mvKind = mods.eln.node.voxel.CableSpec.preset(MV);
                radial(pl, ToolbagItem.CHOOSE, new int[]{0, MV, mvKind.mm2(), mvKind.colour()});
                boolean mvStarted = CablePlacer.active(pl.getUUID()) && ToolbagItem.selected(two).tier() == MV;
                var lvKind = mods.eln.node.voxel.CableSpec.preset(LV);
                radial(pl, ToolbagItem.CHOOSE, new int[]{0, LV, lvKind.mm2(), lvKind.colour()});
                boolean lvKept = CablePlacer.active(pl.getUUID()) && ToolbagItem.selected(two).tier() == LV;
                BlockPos end = c.at(5, 1, 5);
                floorClick(c, pl, two, end);
                floorClick(c, pl, two, end);
                String t = tiers(c, c.at(3, 1, 5));
                radial = String.format("waited %b, 230 V started %b, 48 V kept the route %b, laid tiers [%s], MV left %d, LV left %d", waited, mvStarted, lvKept, t,
                    held(two, MV)[0], held(two, LV)[0]);
                radialOk = waited && mvStarted && lvKept && t.equals(String.valueOf(LV)) && held(two, MV)[0] == 300 && held(two, LV)[0] < 300;

                clean(pl);
                // 4. a pipe: its kind, then Cancel
                BlockPos pp = c.at(7, 1, 1);
                mods.eln.node.voxel.PipeKitItem.place(level, pp, new PipePart(0, new int[]{8, 4, 0, 8, 4, 16}), null);
                ItemStack pipes = bag(PipeKitItem.kit(0, 300), CableKitItem.kit(LV, 300));
                pl.setItemInHand(InteractionHand.MAIN_HAND, pipes);
                ToolbagItem.click(pl, level, pipes, pp, Direction.UP, Vec3.atLowerCornerOf(pp).add(0.5, 4.5 / 16, 0.5), false);
                boolean pipeOn = PipePlacer.active(pl.getUUID());
                var pk = ToolbagItem.selected(pipes);
                radial(pl, ToolbagItem.CANCEL, null);
                pipe = String.format("pipe route %b, kind %s, after Cancel %b", pipeOn, pk == null ? null : pk.label(), PipePlacer.active(pl.getUUID()));
                pipeOk = pipeOn && pk != null && pk.pipe() && pk.tier() == 0 && !PipePlacer.active(pl.getUUID());
            } catch (Exception ex) {
                err = String.valueOf(ex);
            } finally {
                clean(pl);
                pl.snapTo(px, py, pz, 0, 0);
            }
        }

        public void measure(TestContext c) {
            c.check("toolbag: no error", err.isEmpty(), err);
            c.check("toolbag: a click on a cable lays its kind, from more than one kit", laidOk, laid);
            c.check("toolbag: a cable kind it lacks is refused", refusedOk, refused);
            c.check("toolbag: the radial's choice starts the route at the click; a new kind keeps the start", radialOk, radial);
            c.check("toolbag: a pipe click takes the pipe kind; Cancel drops the route", pipeOk, pipe);
        }
    }

    /**
     * AdventurAgent's client look: a click past reach, or with its point in mid-air off the clicked block, starts nothing
     * (bag and plain kit alike); on a pipe route a sneak-click on the pipe opens the radial and lays nothing; the
     * radial's Undo takes back the last waypoint, and the route then ends at the one before.
     */
    static final class ReachSneakUndoCase implements TestCase {
        String err = "", reach = "", sneak = "", undo = "";
        boolean reachOk, sneakOk, undoOk;

        public String id() {
            return "toolbag_reach_sneak_undo";
        }

        public int width() {
            return 9;
        }

        public int height() {
            return 7;
        }

        public int depth() {
            return 7;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) c.floor(c.at(x, 0, z));
            var pl = player(c);
            var level = c.level();
            double px = pl.getX(), py = pl.getY(), pz = pl.getZ();
            BlockPos mid = c.at(1, 1, 3);
            pl.snapTo(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
            try {
                // 1. past reach (8 blocks off), and a point in mid-air off the clicked block
                ItemStack bag = bag(CableKitItem.kit(LV, 600));
                ToolbagItem.select(bag, ToolbagItem.Kind.cable(LV, null));
                pl.setItemInHand(InteractionHand.MAIN_HAND, bag);
                BlockPos farFloor = c.at(8, 0, 3);
                ToolbagItem.click(pl, level, bag, farFloor, Direction.UP, Vec3.atLowerCornerOf(farFloor).add(0.5, 1, 0.5), false);
                boolean farBag = CablePlacer.active(pl.getUUID()) || ToolbagItem.pendingForTests(pl.getUUID());
                BlockPos near = c.at(2, 0, 3);
                ToolbagItem.click(pl, level, bag, near, Direction.UP, Vec3.atLowerCornerOf(near).add(0.5, 2.5, 0.5), false);
                boolean airBag = CablePlacer.active(pl.getUUID()) || ToolbagItem.pendingForTests(pl.getUUID());
                ItemStack kit = CableKitItem.kit(LV, 600);
                pl.setItemInHand(InteractionHand.MAIN_HAND, kit);
                CablePlacer.use(pl, level, farFloor, Direction.UP, kit, LV, Vec3.atLowerCornerOf(farFloor).add(0.5, 1, 0.5));
                boolean farKit = CablePlacer.active(pl.getUUID());
                CablePlacer.use(pl, level, near, Direction.UP, kit, LV, Vec3.atLowerCornerOf(near).add(0.5, 2.5, 0.5));
                boolean airKit = CablePlacer.active(pl.getUUID());
                // just past reach (5 blocks east, 5.3 from the eyes: inside the server's block of slack), through the kit
                // item's own use, as the client's click arrives
                pl.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                BlockPos past = c.at(6, 0, 3);
                var ctx = new net.minecraft.world.item.context.UseOnContext(pl, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atLowerCornerOf(past).add(0.5, 1, 0.5), Direction.UP, past, false));
                kit.getItem().useOn(ctx);
                boolean pastKit = CablePlacer.active(pl.getUUID());
                BlockPos in = c.at(4, 0, 3);
                kit.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(pl, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atLowerCornerOf(in).add(0.5, 1, 0.5), Direction.UP, in, false)));
                boolean inKit = CablePlacer.active(pl.getUUID());
                reach = String.format("route or pending: bag far %b, bag mid-air %b, kit far %b, kit mid-air %b, kit just past reach %b; within reach %b",
                    farBag, airBag, farKit, airKit, pastKit, inKit);
                reachOk = !farBag && !airBag && !farKit && !airKit && !pastKit && inKit;
                clean(pl);

                // 2. a pipe route, then a sneak-click on the pipe
                BlockPos pp = c.at(2, 1, 1);
                mods.eln.node.voxel.PipeKitItem.place(level, pp, new PipePart(0, new int[]{8, 4, 0, 8, 4, 16}), null);
                ItemStack pipes = bag(PipeKitItem.kit(0, 300));
                pl.setItemInHand(InteractionHand.MAIN_HAND, pipes);
                Vec3 onPipe = Vec3.atLowerCornerOf(pp).add(0.5, 4.5 / 16, 0.5);
                ToolbagItem.click(pl, level, pipes, pp, Direction.UP, onPipe, false);
                boolean on = PipePlacer.active(pl.getUUID());
                int parts = node(c, pp).parts.size();
                ToolbagItem.click(pl, level, pipes, pp, Direction.UP, onPipe, true);
                sneak = String.format("pipe route %b; after the sneak-click: still on %b, parts %d -> %d", on, PipePlacer.active(pl.getUUID()), parts,
                    node(c, pp).parts.size());
                sneakOk = on && PipePlacer.active(pl.getUUID()) && node(c, pp).parts.size() == parts;
                clean(pl);

                // 3. Undo: start at (1,1,5), waypoints (4,1,5) and (4,1,3); Undo; (4,1,5) again ends it there
                ItemStack lv = bag(CableKitItem.kit(LV, 600));
                pl.setItemInHand(InteractionHand.MAIN_HAND, lv);
                floorClick(c, pl, lv, c.at(1, 1, 5));
                var preset = mods.eln.node.voxel.CableSpec.preset(LV);
                radial(pl, ToolbagItem.CHOOSE, new int[]{0, LV, preset.mm2(), preset.colour()});
                floorClick(c, pl, lv, c.at(4, 1, 5));
                floorClick(c, pl, lv, c.at(4, 1, 3));
                int before = CablePlacer.waypoints(pl.getUUID());
                radial(pl, ToolbagItem.UNDO, null);
                int after = CablePlacer.waypoints(pl.getUUID());
                floorClick(c, pl, lv, c.at(4, 1, 5));
                undo = String.format("waypoints %d -> %d after Undo; laid at (3,1,5) [%s], at (4,1,4) [%s], route left %b", before, after,
                    tiers(c, c.at(3, 1, 5)), tiers(c, c.at(4, 1, 4)), CablePlacer.active(pl.getUUID()));
                undoOk = before == 2 && after == 1 && tiers(c, c.at(3, 1, 5)).equals(String.valueOf(LV)) && tiers(c, c.at(4, 1, 4)).equals("none")
                    && !CablePlacer.active(pl.getUUID());
            } catch (Exception ex) {
                err = String.valueOf(ex);
            } finally {
                clean(pl);
                pl.snapTo(px, py, pz, 0, 0);
            }
        }

        public void measure(TestContext c) {
            c.check("toolbag: no error", err.isEmpty(), err);
            c.check("toolbag and kit: a click past reach or in mid-air starts nothing", reachOk, reach);
            c.check("toolbag: on a pipe route a sneak-click opens the radial, lays nothing", sneakOk, sneak);
            c.check("toolbag: Undo takes back the last waypoint", undoOk, undo);
        }
    }

    /**
     * The shears (#517). Runs of LV cable along x on a floor, each block one straight piece, a Signal Source at x 0
     * (a device) and a branch spliced onto the piece at x 4 (a junction). (a) A cut started at x 2 and aimed at x 3:
     * those two pieces go, 32 voxels back into the bag's LV kit, a point off the shears; aimed at x 4 (the junction) the
     * span doesn't reach it; x 1 and x 4 stay. (b) The bag full of full LV kits: the cut drops its length at the player.
     * (c) No shears: no cut starts. (d) A right-click cancels: the run stays whole. (e) An iron pipe run of two blocks
     * cut whole: its length back as an iron pipe kit.
     */
    static final class ShearsCase implements TestCase {
        String err = "", a = "", b = "", cc = "", d = "", e = "";
        boolean aOk, bOk, cOk, dOk, eOk;

        public String id() {
            return "toolbag_shears_cut_runs";
        }

        public int width() {
            return 9;
        }

        public int height() {
            return 7;
        }

        public int depth() {
            return 7;
        }

        public int ticks() {
            return 20;
        }

        /** An LV run along x at row z from x 1 to 4, a source at x 0, a branch onto x 4's piece. */
        static void run(TestContext c, int z) {
            for (int x = 1; x <= 4; x++)
                mods.eln.node.voxel.PipeKitItem.place(c.level(), c.at(x, 1, z), new CablePart(LV, CableCases.DOWN, new int[]{0, 0, 8, 16, 0, 8}), null);
            mods.eln.node.voxel.PipeKitItem.place(c.level(), c.at(4, 1, z), new CablePart(LV, CableCases.DOWN, new int[]{8, 0, 8, 8, 0, 16}), null);
            c.placeSix(Wp9aCases.SOURCE, c.at(0, 1, z));
        }

        static Vec3 on(BlockPos p) {
            return Vec3.atLowerCornerOf(p).add(0.3, 0.5 / 16, 0.5);
        }

        static ItemStack withShears(ItemStack bag) {
            var items = new ArrayList<>(ToolbagItem.contents(bag));
            items.set(ToolbagItem.SHEARS_SLOT, new ItemStack(Items.SHEARS));
            ToolbagItem.setContents(bag, items);
            return bag;
        }

        static int pieces(TestContext c, BlockPos p) {
            var v = node(c, p);
            return v == null ? 0 : v.parts.size();
        }

        public void build(TestContext c) {
            for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) c.floor(c.at(x, 0, z));
            run(c, 0);
            run(c, 2);
            run(c, 4);
            for (int x = 6; x <= 7; x++)
                mods.eln.node.voxel.PipeKitItem.place(c.level(), c.at(x, 1, 6), new PipePart(0, new int[]{0, 4, 8, 16, 4, 8}), null);
            // (cut once the blocks have connected and recorded their joints)
            c.after(5, () -> cut(c));
        }

        void cut(TestContext c) {
            var pl = player(c);
            var level = c.level();
            double px = pl.getX(), py = pl.getY(), pz = pl.getZ();
            BlockPos mid = c.at(3, 1, 3);
            pl.snapTo(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
            try {
                // (a)
                ItemStack bag = withShears(bag(CableKitItem.kit(LV, 100)));
                pl.setItemInHand(InteractionHand.MAIN_HAND, bag);
                BlockPos b2 = c.at(2, 1, 0), b3 = c.at(3, 1, 0), b4 = c.at(4, 1, 0);
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, bag, b2, on(b2));
                boolean started = mods.eln.node.voxel.ToolbagShears.active(pl.getUUID());
                mods.eln.node.voxel.ToolbagShears.aimForTests(pl, level, b4, on(b4));   // (the junction: not in the run)
                mods.eln.node.voxel.ToolbagShears.aimForTests(pl, level, b3, on(b3));
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, bag, b3, on(b3));
                int wear = ToolbagItem.contents(bag).get(ToolbagItem.SHEARS_SLOT).getDamageValue();
                a = String.format("started %b; pieces x1..4: %d %d %d %d; LV in the bag %d; shears wear %d", started, pieces(c, c.at(1, 1, 0)),
                    pieces(c, b2), pieces(c, b3), pieces(c, b4), held(bag, LV)[0], wear);
                aOk = started && pieces(c, c.at(1, 1, 0)) == 1 && pieces(c, b2) == 0 && pieces(c, b3) == 0 && pieces(c, b4) == 2
                    && held(bag, LV)[0] == 132 && wear == 1 && !mods.eln.node.voxel.ToolbagShears.active(pl.getUUID());
                clean(pl);

                // (b) a full bag
                ItemStack[] full = new ItemStack[ToolbagItem.KITS];
                for (int k = 0; k < full.length; k++) full[k] = CableKitItem.kit(LV, KitItem.MAX);
                ItemStack fb = withShears(bag(full));
                pl.setItemInHand(InteractionHand.MAIN_HAND, fb);
                BlockPos r2 = c.at(2, 1, 2);
                var box = new net.minecraft.world.phys.AABB(pl.blockPosition()).inflate(3);
                int before = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).size();
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, fb, r2, on(r2));
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, fb, r2, on(r2));
                var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box);
                int dropLen = 0;
                for (var it : drops) if (it.getItem().getItem() instanceof CableKitItem) dropLen += KitItem.lengthForTests(it.getItem());
                b = String.format("piece at x2 %d; dropped %d item(s), %d voxels of kit; bag %d", pieces(c, r2), drops.size() - before, dropLen, held(fb, LV)[0]);
                bOk = pieces(c, r2) == 0 && dropLen == 16 && held(fb, LV)[0] == ToolbagItem.KITS * KitItem.MAX;
                for (var it : drops) it.discard();
                clean(pl);

                // (c) no shears
                ItemStack ns = bag(CableKitItem.kit(LV, 100));
                pl.setItemInHand(InteractionHand.MAIN_HAND, ns);
                BlockPos s2 = c.at(2, 1, 4);
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, ns, s2, on(s2));
                cc = "cut started " + mods.eln.node.voxel.ToolbagShears.active(pl.getUUID()) + ", piece " + pieces(c, s2);
                cOk = !mods.eln.node.voxel.ToolbagShears.active(pl.getUUID()) && pieces(c, s2) == 1;

                // (d) a right-click cancels
                ItemStack cb = withShears(bag(CableKitItem.kit(LV, 100)));
                pl.setItemInHand(InteractionHand.MAIN_HAND, cb);
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, cb, s2, on(s2));
                boolean on = mods.eln.node.voxel.ToolbagShears.active(pl.getUUID());
                ToolbagItem.click(pl, level, cb, s2, Direction.UP, on(s2), false);
                d = String.format("cut on %b, after the right-click %b; pieces x1..3 %d %d %d; bag %d", on, mods.eln.node.voxel.ToolbagShears.active(pl.getUUID()),
                    pieces(c, c.at(1, 1, 4)), pieces(c, s2), pieces(c, c.at(3, 1, 4)), held(cb, LV)[0]);
                dOk = on && !mods.eln.node.voxel.ToolbagShears.active(pl.getUUID()) && pieces(c, c.at(1, 1, 4)) == 1 && pieces(c, s2) == 1
                    && pieces(c, c.at(3, 1, 4)) == 1 && held(cb, LV)[0] == 100 && !CablePlacer.active(pl.getUUID());
                clean(pl);

                // (e) a pipe run
                ItemStack pb = withShears(bag());
                pl.setItemInHand(InteractionHand.MAIN_HAND, pb);
                BlockPos p6 = c.at(6, 1, 6), p7 = c.at(7, 1, 6);
                Vec3 h6 = Vec3.atLowerCornerOf(p6).add(0.5, 4.5 / 16, 0.5), h7 = Vec3.atLowerCornerOf(p7).add(0.5, 4.5 / 16, 0.5);
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, pb, p6, h6);
                mods.eln.node.voxel.ToolbagShears.aimForTests(pl, level, p7, h7);
                mods.eln.node.voxel.ToolbagShears.leftClick(pl, level, pb, p7, h7);
                int pipeLen = 0;
                for (ItemStack s : ToolbagItem.contents(pb)) if (s.getItem() instanceof PipeKitItem) pipeLen += KitItem.lengthForTests(s);
                e = String.format("pipes left %d %d; iron pipe in the bag %d voxels", pieces(c, p6), pieces(c, p7), pipeLen);
                eOk = pieces(c, p6) == 0 && pieces(c, p7) == 0 && pipeLen == 32;
            } catch (Exception ex) {
                err = String.valueOf(ex);
            } finally {
                clean(pl);
                mods.eln.node.voxel.ToolbagShears.forget(pl.getUUID());
                pl.snapTo(px, py, pz, 0, 0);
            }
        }

        public void measure(TestContext c) {
            c.check("shears: no error", err.isEmpty(), err);
            c.check("shears: a cut takes the span back into the bag, not past a junction, a point of wear", aOk, a);
            c.check("shears: what doesn't fit in a full bag drops", bOk, b);
            c.check("shears: no shears, no cut", cOk, cc);
            c.check("shears: a right-click cancels, the run stays", dOk, d);
            c.check("shears: a pipe run cut whole goes back as a pipe kit", eOk, e);
        }
    }
}
