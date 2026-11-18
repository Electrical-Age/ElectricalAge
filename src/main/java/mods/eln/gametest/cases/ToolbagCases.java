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

    static void radial(ServerPlayer pl, int action, int[] kind) throws java.io.IOException {
        byte[] b = ToolbagItem.choicePacket(action, kind);
        var in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(b, 1, b.length - 1));
        ToolbagItem.choice(pl, in);
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
}
