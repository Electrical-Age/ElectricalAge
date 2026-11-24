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
            r.getInventory().setItem(0, battery(20000));
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

    /** Broken, it drops one item, with its battery (and the battery's charge) inside; placed again, the battery is back. */
    static final class KeepsItsBatteries implements TestCase {
        String detail = "", placed = "";
        int loose = -1;
        double charge = -1;

        public String id() {
            return "ground_radar_keeps_its_batteries";
        }

        public void build(TestContext c) {
            BlockPos at = c.at(3, 1, 1);
            GroundRadarElement r = (GroundRadarElement) c.placeTransparent(RADAR, at);
            r.getInventory().setItem(1, battery(12345));
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
            }
        }

        public void measure(TestContext c) {
            c.check("ground radar: broken, it spills no battery", loose == 0, detail);
            c.check("ground radar: placed again, its battery is back with its charge", Math.abs(charge - 12345) < 1, placed + " " + charge);
        }
    }
}
