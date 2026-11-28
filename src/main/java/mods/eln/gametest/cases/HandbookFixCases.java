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
 * once; a cable kit ends only on a connector of its kind (not on a heater's thermal back); a cable kit on a Lamp Socket
 * still in Lamp Supply mode says it is set to Lamp Supply; a kit route can't end where it started.
 */
public final class HandbookFixCases {
    private HandbookFixCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Fixes());
    }

    static final class Fixes implements TestCase {
        HeatFurnaceElement furnace;
        mods.eln.oil.ElectricHeaterElement heater;
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
            heater = (mods.eln.oil.ElectricHeaterElement) Wp10bCases.placeFacing(c, Ids.transparent("48V Electric Heater"), c.at(2, 1, 1), mods.eln.misc.Direction.XN);
            lampAt = c.at(4, 1, 1);
            lamp = (LampSocketElement) c.placeSix(LampLightCases.LAMP_ID << 6, lampAt);
            // a kit clicked twice on the same floor spot: start, then "finish" there
            floor = c.at(7, 1, 1);
            var player = c.player();
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            Vec3 hit = Vec3.atLowerCornerOf(floor).add(0.5, 0, 0.5);
            int lv = CablePart.tierOf("Low Voltage Cable");
            var kit = mods.eln.node.voxel.CableKitItem.kit(lv, 16 * 20);
            kitBefore = mods.eln.node.voxel.KitItem.lengthForTests(kit);
            handled1 = c.kitClick(player, kit, floor.below(), net.minecraft.core.Direction.UP, hit);
            handled2 = c.kitClick(player, kit, floor.below(), net.minecraft.core.Direction.UP, hit);
            // (a third click: before, the second was a waypoint at the start and this one finished a loop there)
            c.kitClick(player, kit, floor.below(), net.minecraft.core.Direction.UP, hit);
            kitAfter = mods.eln.node.voxel.KitItem.lengthForTests(kit);
            mods.eln.node.voxel.CablePlacer.cancelForTests(player.getUUID());
        }

        public void measure(TestContext c) {
            c.check("heat furnace: a new one takes fuel at once", furnace.getTakeFuel(), "");
            // an LV kit on the heater's thermal back finds no connector; on its flank it does (AdventurAgent: "Laid", 0 A)
            int lvTier = CablePart.tierOf("Low Voltage Cable");
            BlockPos hp = heater.node.coordonate.pos();
            var back = heater.front.back().toEnumFacing();
            var flank = heater.front.left().toEnumFacing();
            var atBack = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), hp, back,
                Vec3.atCenterOf(hp).add(back.getStepX() * 0.5, -0.4, back.getStepZ() * 0.5), true, new long[1][], lvTier);
            var atFlank = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), hp, flank,
                Vec3.atCenterOf(hp).add(flank.getStepX() * 0.5, -0.4, flank.getStepZ() * 0.5), true, new long[1][], lvTier);
            var thermalBack = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), hp, back,
                Vec3.atCenterOf(hp).add(back.getStepX() * 0.5, -0.4, back.getStepZ() * 0.5), true, new long[1][], CablePart.tierOf("Copper Thermal Cable"));
            c.check("cable kit: no LV end on the heater's thermal back, one on its flank, a thermal one on its back",
                atBack == null && atFlank != null && thermalBack != null, atBack + " / " + atFlank + " / " + thermalBack);
            String said = mods.eln.node.voxel.CablePlacer.noEndForTests(c.level(), lampAt, Vec3.atLowerCornerOf(lampAt).add(0.5, 0.1, 0.5));
            c.check("lamp socket in Lamp Supply mode: the kit says it is set to Lamp Supply", said.contains("Lamp Supply"), said);
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(floor, c.level()));
            c.check("a kit route can't end where it started: nothing laid, nothing taken", handled1 && handled2 && kitBefore == kitAfter
                && !(n instanceof mods.eln.node.voxel.VoxelNode v && !v.parts.isEmpty()), kitBefore + " -> " + kitAfter + ", " + n);
        }
    }
}
