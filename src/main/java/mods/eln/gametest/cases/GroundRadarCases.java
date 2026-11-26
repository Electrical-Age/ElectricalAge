package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import mods.eln.transparentnode.groundradar.GroundRadarElement;
import mods.eln.transparentnode.groundradar.GroundScan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** The Ground Radar (Baughn 2026-11-24): what it sees, what a scan costs, and its batteries riding in its item. */
public final class GroundRadarCases {
    private GroundRadarCases() {
    }

    static final int RADAR = Ids.transparent("Ground Radar");

    public static void addTo(List<TestCase> cases) {
        cases.add(new SeesAHollow());
        cases.add(new NoBatteries());
        cases.add(new KeepsItsBatteries());
        cases.add(new OverTheBus());
        cases.add(new MinerView());
        cases.add(new GogglesUpgrade());
    }

    /** A full Portable Condensator Pack (15 kJ, drawn at 4.5 kW): a scan at 0.9 J a cell done in seconds. */
    static ItemStack fastPack() {
        ItemStack b = Eln.findItemStack("Portable Condensator Pack");
        var e = (IItemEnergyBattery) Utils.getItemObject(b);
        e.setEnergy(b, e.getEnergyMax(b));
        return b;
    }

    static ItemStack battery(double joules) {
        ItemStack b = Eln.findItemStack("Portable Battery");   // (40 kJ)
        ((IItemEnergyBattery) Utils.getItemObject(b)).setEnergy(b, joules);
        return b;
    }

    static double energy(ItemStack s) {
        return Utils.getItemObject(s) instanceof IItemEnergyBattery b ? b.getEnergy(s) : -1;
    }

    /**
     * Stone under the radar with a 2 x 2 x 2 hollow at its second layer of cells (2-3 blocks down), right under it: the
     * picture shows the hollow dark against the stone, and it drew its cells' energy from the batteries.
     */
    static final class SeesAHollow implements TestCase {
        GroundRadarElement r;
        double before;

        public String id() {
            return "ground_radar_sees_a_hollow";
        }

        public int width() {
            return 11;
        }

        public int depth() {
            return 9;
        }

        public int height() {
            return 12;
        }

        public int ticks() {
            return 120;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 11; x++)
                for (int z = 0; z < 9; z++)
                    for (int y = 1; y < 10; y++) c.level().setBlock(c.at(x, y, z), Blocks.STONE.defaultBlockState(), 3);
            // (the radar at (5, 10, 4): its centre cells start at x = 5, z = 4; the second layer is y = 7..6)
            for (int x = 5; x < 7; x++)
                for (int z = 4; z < 6; z++)
                    for (int y = 6; y < 8; y++) c.level().setBlock(c.at(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            r = (GroundRadarElement) c.placeTransparent(RADAR, c.at(5, 10, 4));
            r.getInventory().setItem(0, fastPack());
            before = energy(r.getInventory().getItem(0));
            c.check("ground radar: a scan starts with charged batteries", r.startScan(), "");
        }

        public void measure(TestContext c) {
            GroundScan p = r.lastPicture();
            c.check("ground radar: the scan finished", p != null, String.valueOf(r.getWaila()));
            if (p == null) return;
            int mid = GroundScan.N / 2;
            double hollow = p.densityAt(1, mid, mid), stone = p.densityAt(1, mid - 2, mid - 2), top = p.densityAt(0, mid, mid);
            String st = String.format("hollow %.2f, stone beside it %.2f, stone over it %.2f g/cm3; %d layers", hollow, stone, top, p.layers.length);
            c.line("ground radar: " + st);
            c.check("ground radar: the hollow shows dark", hollow < 0.8, st);
            c.check("ground radar: ... against the stone around it, and over it", stone > 2.2 && top > 2.2, st);
            c.check("ground radar: it looks down to the world's floor", p.layers[p.layers.length - 1].depth() + p.layers[p.layers.length - 1].cell()
                >= p.cy - 1 - c.level().getMinY(), st);
            double used = before - energy(r.getInventory().getItem(0));
            double cost = mods.eln.transparentnode.groundradar.GroundRadarDescriptor.J_PER_CELL * p.cells();
            c.check("ground radar: a scan costs its cells' energy", Math.abs(used - cost) < 0.05 * cost + 1, String.format("%.0f J for %.0f J of cells", used, cost));
        }
    }

    /** No batteries: no scan, and it says why. */
    static final class NoBatteries implements TestCase {
        GroundRadarElement r;
        boolean started;

        public String id() {
            return "ground_radar_without_batteries";
        }

        public void build(TestContext c) {
            r = (GroundRadarElement) c.placeTransparent(RADAR, c.at(3, 1, 1));
            started = r.startScan();
        }

        public void measure(TestContext c) {
            c.check("ground radar: no scan without batteries", !started && r.lastPicture() == null, "");
            c.check("ground radar: ... and Jade says why", String.valueOf(r.getWaila()).contains("flat"), String.valueOf(r.getWaila()));
            r.getInventory().setItem(0, battery(1000));
            r.getInventory().setChanged();   // (as a slot click does)
            c.check("ground radar: batteries put in clear the complaint", !String.valueOf(r.getWaila()).contains("flat"), String.valueOf(r.getWaila()));
        }
    }

    /**
     * Broken, it drops one item, with its battery (and the battery's charge) inside; placed again, the battery is back, and
     * so is the goggles' band a player picked (Baughn 1575643239036223550); a new radar starts on Everything.
     */
    static final class KeepsItsBatteries implements TestCase {
        String detail = "", placed = "";
        int loose = -1;
        double charge = -1;
        mods.eln.transparentnode.groundradar.RadarBand fresh, picked = new mods.eln.transparentnode.groundradar.RadarBand(
            mods.eln.transparentnode.groundradar.RadarBand.CUSTOM, 4.6, 6.0), back;

        public String id() {
            return "ground_radar_keeps_its_batteries";
        }

        public void build(TestContext c) {
            BlockPos at = c.at(3, 1, 1);
            GroundRadarElement r = (GroundRadarElement) c.placeTransparent(RADAR, at);
            r.getInventory().setItem(1, battery(12345));
            fresh = r.band();
            r.networkUnserialize(c.stream(o -> {   // (the goggles' packet, as GroundRadarRender.setBand sends it)
                o.writeByte(GroundRadarElement.BAND_ID);
                o.writeByte(picked.kind());
                o.writeFloat((float) picked.lo());
                o.writeFloat((float) picked.hi());
            }));
            c.level().destroyBlock(at, true);
            var items = c.level().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(2));
            loose = 0;
            ItemStack radar = ItemStack.EMPTY;
            for (ItemEntity e : items) {
                if (Utils.getItemObject(e.getItem()) instanceof mods.eln.transparentnode.groundradar.GroundRadarDescriptor) radar = e.getItem().copy();
                else loose++;
                detail += e.getItem() + " ";
                e.discard();
            }
            if (radar.isEmpty()) return;
            var d = (mods.eln.node.transparent.TransparentNodeDescriptor) Utils.getItemObject(radar);
            var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(radar, c.player(), c.level(), at, mods.eln.misc.Direction.YN);
            if (node != null && node.element instanceof GroundRadarElement again) {
                charge = energy(again.getInventory().getItem(1));
                placed = "" + again.getInventory().getItem(1);
                back = again.band();
            }
        }

        public void measure(TestContext c) {
            c.check("ground radar: broken, it spills no battery", loose == 0, detail);
            c.check("ground radar: placed again, its battery is back with its charge", Math.abs(charge - 12345) < 1, placed + " " + charge);
            c.check("ground radar: a new one's goggles show everything", mods.eln.transparentnode.groundradar.RadarBand.DEFAULT.equals(fresh), String.valueOf(fresh));
            c.check("ground radar: placed again, its goggles' band is the one picked", picked.equals(back), picked + " vs " + back);
        }
    }

    /**
     * Its bus port (Baughn: players get creative): at address 60, a scan written over the bus, then its registers read and a
     * layer's densities asked for as data packets; the pieces put together are the picture's layer.
     */
    static final class OverTheBus implements TestCase {
        GroundRadarElement r;
        mods.eln.bus.BusPort pc;
        mods.eln.sim.IProcess watch;
        int tick;
        final java.util.Map<Integer, Float> replies = new java.util.TreeMap<>();
        final java.util.Map<Integer, byte[]> pieces = new java.util.TreeMap<>(), flagPieces = new java.util.TreeMap<>();
        int refused = -1;
        boolean forgedAnswered;

        public String id() {
            return "ground_radar_over_the_bus";
        }

        public int ticks() {
            return 160;
        }

        public void build(TestContext c) {
            r = (GroundRadarElement) c.placeTransparent(RADAR, c.at(3, 1, 1));
            r.getInventory().setItem(0, fastPack());
            r.networkUnserialize(c.stream(o -> {   // (its screen's packet: a colour channel's number isn't a device address)
                o.writeByte(GroundRadarElement.ADDRESS_ID);
                o.writeFloat(5);
            }));
            refused = r.port.address();
            r.networkUnserialize(c.stream(o -> {
                o.writeByte(GroundRadarElement.ADDRESS_ID);
                o.writeFloat(60);
            }));
            pc = new mods.eln.bus.BusPort();
            pc.setAddress(200);
            pc.listen(true);
            mods.eln.bus.BusNet.link(pc, r.port);
            watch = dt -> {
                ++tick;
                if (tick == 2) pc.send(new mods.eln.bus.Packet(60, 200, mods.eln.bus.PacketType.WRITE, GroundRadarElement.REG_SCAN, 1));
                if (tick == 100) {
                    pc.send(new mods.eln.bus.Packet(60, 200, mods.eln.bus.PacketType.WRITE, GroundRadarElement.REG_CUR_LAYER, 0));
                    pc.send(new mods.eln.bus.Packet(60, 200, mods.eln.bus.PacketType.WRITE, GroundRadarElement.REG_CUR_X, 12));
                    pc.send(new mods.eln.bus.Packet(60, 200, mods.eln.bus.PacketType.WRITE, GroundRadarElement.REG_CUR_Z, 12));
                }
                if (tick == 102) {
                    for (int reg : new int[]{GroundRadarElement.REG_STATE, GroundRadarElement.REG_LAYERS, GroundRadarElement.REG_CELLS,
                        GroundRadarElement.REG_DENSITY, GroundRadarElement.REG_LAYER_CELL, 99})
                        pc.send(new mods.eln.bus.Packet(60, 200, mods.eln.bus.PacketType.READ, reg, 0));
                    pc.send(new mods.eln.bus.Packet(60, 200, mods.eln.bus.PacketType.WRITE, GroundRadarElement.REG_SEND_DENSITY, 0));
                    // (a forged source, a colour channel: no answer)
                    pc.send(new mods.eln.bus.Packet(60, 5, mods.eln.bus.PacketType.READ, GroundRadarElement.REG_AGE, 0));
                }
                if (tick == 120) pc.send(new mods.eln.bus.Packet(60, 200, mods.eln.bus.PacketType.WRITE, GroundRadarElement.REG_SEND_FLAGS, 1));
                if (tick > 100)
                    for (mods.eln.bus.Packet k; (k = pc.poll()) != null; ) {
                        if (k.type() == mods.eln.bus.PacketType.REPLY) {
                            replies.put(k.index(), k.value());
                            if (k.index() == GroundRadarElement.REG_AGE) forgedAnswered = true;
                        }
                        if (k.type() == mods.eln.bus.PacketType.DATA) (tick < 120 ? pieces : flagPieces).put(k.index(), k.data());
                    }
            };
            mods.eln.Eln.simulator.addSlowProcess(watch);
        }

        public void measure(TestContext c) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            mods.eln.bus.BusNet.detach(List.of(pc));
            GroundScan p = r.lastPicture();
            c.line("ground radar over the bus: replies " + replies + ", " + pieces.size() + " data pieces");
            c.check("ground radar: a scan written over the bus ran", p != null, String.valueOf(r.getWaila()));
            if (p == null) return;
            c.check("ground radar: it answers its registers (idle, layers, cells across, the cell's size)", replies.getOrDefault(0, -1f) == 0
                && replies.getOrDefault(4, -1f) == p.layers.length && replies.getOrDefault(5, -1f) == p.n && replies.getOrDefault(13, -1f) == p.layers[0].cell(), "" + replies);
            c.check("ground radar: ... the cursor's density as the picture has it", Math.abs(replies.getOrDefault(9, -9f) - p.densityAt(0, 12, 12)) < 1e-3, "" + replies);
            c.check("ground radar: ... and not a register it hasn't", !replies.containsKey(99), "" + replies);
            c.check("ground radar: it refuses a colour channel's number as its address", refused == 0, "" + refused);
            c.check("ground radar: it doesn't answer a forged source (a colour channel)", !forgedAnswered, "" + replies);
            var fb = new java.io.ByteArrayOutputStream();
            for (byte[] piece : flagPieces.values()) fb.writeBytes(piece);
            c.check("ground radar: a layer's flags come the same way", java.util.Arrays.equals(fb.toByteArray(),
                java.util.Arrays.copyOfRange(p.flags, p.n * p.n, 2 * p.n * p.n)), fb.size() + " bytes");
            var b = new java.io.ByteArrayOutputStream();
            for (byte[] piece : pieces.values()) b.writeBytes(piece);
            byte[] layer = java.util.Arrays.copyOfRange(p.density, 0, p.n * p.n);
            c.check("ground radar: a layer asked for comes as data packets, in 64-byte pieces, the picture's own bytes",
                java.util.Arrays.equals(b.toByteArray(), layer), b.size() + " of " + layer.length + " bytes");
        }
    }

    /**
     * The auto-miner's views: the slice at its drill head shows a hollow at the head's level; the cross from the miner
     * down to the floor has its two cuts read and the rest left unknown, at full signal.
     */
    static final class MinerView implements TestCase {
        GroundScan slice, cross;

        public String id() {
            return "ground_radar_miner_view";
        }

        public int width() {
            return 11;
        }

        public int depth() {
            return 9;
        }

        public int height() {
            return 12;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 11; x++)
                for (int z = 0; z < 9; z++)
                    for (int y = 1; y < 10; y++) c.level().setBlock(c.at(x, y, z), Blocks.STONE.defaultBlockState(), 3);
            // (the head at (5, 6, 4): the slice is y 6..5; its centre cells start at x = 5, z = 4; up to y 7 the cross's
            // layer over the head (y 7..6) is hollow too)
            for (int x = 5; x < 7; x++)
                for (int z = 4; z < 6; z++)
                    for (int y = 5; y < 8; y++) c.level().setBlock(c.at(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            slice = GroundRadarElement.minerSlice(c.level(), c.at(5, 6, 4));
            cross = GroundRadarElement.minerCross(c.level(), c.at(5, 10, 4));
        }

        public void measure(TestContext c) {
            int mid = slice.n / 2, k = cross.layerAt(10 - 1 - 6);
            String st = String.format("slice: head %.2f, beside %.2f; cross: %d layers, at the head %.2f (signal %.2f), off the cuts flags %d",
                slice.densityAt(0, mid, mid), slice.densityAt(0, mid - 2, mid - 2), cross.layers.length, cross.densityAt(k, mid, mid),
                cross.signalAt(k, mid, mid), cross.flagsAt(k, 1, 1));
            c.line("miner view: " + st);
            c.check("miner view: the slice shows the hollow at the head's level", slice.layers.length == 1 && slice.densityAt(0, mid, mid) < 0.5
                && slice.densityAt(0, mid - 2, mid - 2) > 2.2, st);
            c.check("miner view: the cross reads its cuts to the floor at full signal, and only them", cross.densityAt(k, mid, mid) < 0.5
                && cross.signalAt(k, mid, mid) > 0.99 && (cross.flagsAt(k, 1, 1) & GroundScan.UNKNOWN) != 0
                && (cross.flagsAt(k, 1, mid) & GroundScan.UNKNOWN) == 0, st);
        }
    }

    /** A Ground Radar holding a battery and Radar Goggles craft a Ground Radar w/Goggles with that battery inside. */
    static final class GogglesUpgrade implements TestCase {
        public String id() {
            return "ground_radar_goggles_upgrade";
        }

        public void build(TestContext c) {
        }

        public void measure(TestContext c) {
            BlockPos at = c.at(3, 1, 1);
            var r = (GroundRadarElement) c.placeTransparent(RADAR, at);
            r.getInventory().setItem(0, battery(23456));
            r.getInventory().setItem(1, battery(5000));
            ItemStack radar = r.getDropItemStack();
            ItemStack goggles = Eln.findItemStack("Radar Goggles");
            var recipe = mods.eln.transparentnode.groundradar.GogglesUpgradeRecipe.INSTANCE;
            var in = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(radar, goggles));
            ItemStack out = recipe.assemble(in);
            c.check("radar goggles: a radar and goggles make a Ground Radar w/Goggles", recipe.matches(in, c.level())
                && out.getItem() == Eln.findItemStack("Ground Radar w/Goggles").getItem()
                && Utils.getItemObject(out) instanceof mods.eln.transparentnode.groundradar.GroundRadarDescriptor d && d.goggles, "" + out);
            c.check("radar goggles: two radars don't", !recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(radar, radar.copy())), c.level()), "");
            c.check("radar goggles: a radar already with goggles doesn't take a second pair",
                !recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(out, goggles.copy())), c.level()), "");
            c.check("radar goggles: two pairs of goggles don't",
                !recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 1, List.of(radar, goggles, goggles.copy())), c.level()), "");
            ItemStack empty = Eln.findItemStack("Ground Radar");
            c.check("radar goggles: a radar with no batteries upgrades too", !recipe.assemble(net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(empty, goggles))).isEmpty(), "");
            var found = c.level().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, c.level());
            c.check("radar goggles: the server's recipe for the grid is this one", found.isPresent()
                && found.get().value() instanceof mods.eln.transparentnode.groundradar.GogglesUpgradeRecipe, found.map(h -> h.id().toString()).orElse("none"));
            c.level().removeBlock(at, false);
            var d = (mods.eln.node.transparent.TransparentNodeDescriptor) Utils.getItemObject(out);
            var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(out, c.player(), c.level(), at, mods.eln.misc.Direction.YN);
            double e = node != null && node.element instanceof GroundRadarElement g ? energy(g.getInventory().getItem(0)) : -1;
            double e2 = node != null && node.element instanceof GroundRadarElement g ? energy(g.getInventory().getItem(1)) : -1;
            c.check("radar goggles: placed, it holds both the radar's batteries", Math.abs(e - 23456) < 1 && Math.abs(e2 - 5000) < 1, e + " " + e2);
        }
    }
}
