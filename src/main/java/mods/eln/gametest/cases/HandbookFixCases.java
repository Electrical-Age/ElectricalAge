package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.CablePart;
import mods.eln.sixnode.lampsocket.LampSocketElement;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fixes from the handbook play-through (AdventurAgent, Baughn 1573669111920066631): a new heat furnace takes fuel at
 * once; a cable kit on a Lamp Socket still in Lamp Supply mode says to switch it to Cable; a kit route can't end where
 * it started.
 */
public final class HandbookFixCases {
    private HandbookFixCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Fixes());
    }

    static final class Fixes implements TestCase {
        HeatFurnaceElement furnace;
        LampSocketElement lamp;
        BlockPos lampAt, floor;
        boolean handled1, handled2;
        long kitBefore, kitAfter;

        public String id() {
            return "handbook_playthrough_fixes";
        }

        public int width() {
            return 9;
        }

        public void build(TestContext c) {
            furnace = (HeatFurnaceElement) c.placeTransparent(Ids.transparent("Stone Heat Furnace"), c.at(1, 1, 1));
            lampAt = c.at(4, 1, 1);
            lamp = (LampSocketElement) c.placeSix(LampLightCases.LAMP_ID << 6, lampAt);
            // a kit clicked twice on the same floor spot: start, then "finish" there
            floor = c.at(7, 1, 1);
            var player = Wp10bCases.player(c);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            Vec3 hit = Vec3.atLowerCornerOf(floor).add(0.5, 0, 0.5);
            double px = player.getX(), py = player.getY(), pz = player.getZ();
            player.snapTo(hit.x, hit.y + 0.5, hit.z, 0, 90);
            int lv = CablePart.tierOf("Low Voltage Cable");
            var kit = mods.eln.node.voxel.CableKitItem.kit(lv, 16 * 20);
            kitBefore = mods.eln.node.voxel.KitItem.lengthForTests(kit);
            handled1 = mods.eln.node.voxel.CablePlacer.use(player, c.level(), floor.below(), net.minecraft.core.Direction.UP, kit, lv, hit);
            handled2 = mods.eln.node.voxel.CablePlacer.use(player, c.level(), floor.below(), net.minecraft.core.Direction.UP, kit, lv, hit);
            // (a third click: before, the second was a waypoint at the start and this one finished a loop there)
            mods.eln.node.voxel.CablePlacer.use(player, c.level(), floor.below(), net.minecraft.core.Direction.UP, kit, lv, hit);
            kitAfter = mods.eln.node.voxel.KitItem.lengthForTests(kit);
            mods.eln.node.voxel.CablePlacer.cancelForTests(player.getUUID());
            player.snapTo(px, py, pz, 0, 0);
        }

        public void measure(TestContext c) {
            c.check("heat furnace: a new one takes fuel at once", furnace.getTakeFuel(), "");
            String said = mods.eln.node.voxel.CablePlacer.noEndForTests(c.level(), lampAt, Vec3.atLowerCornerOf(lampAt).add(0.5, 0.1, 0.5));
            c.check("lamp socket in Lamp Supply mode: the kit says to switch it to Cable", said.contains("switch it to Cable"), said);
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(floor, c.level()));
            c.check("a kit route can't end where it started: nothing laid, nothing taken", handled1 && handled2 && kitBefore == kitAfter
                && !(n instanceof mods.eln.node.voxel.VoxelNode v && !v.parts.isEmpty()), kitBefore + " -> " + kitAfter + ", " + n);
        }
    }
}
