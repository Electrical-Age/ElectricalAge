package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.GROUND;
import static mods.eln.gametest.cases.Wp9aCases.SOURCE;
import static mods.eln.gametest.cases.Wp9aCases.current;

/** Cable air runs (Baughn 2026-11-11, D5): a cable crosses up to 1.2 blocks of air between supports. */
public final class CableAirCases {
    private CableAirCases() {
    }

    static final int LV = CablePart.tierOf("Low Voltage Cable");

    public static void addTo(List<TestCase> cases) {
        cases.add(new BridgeCase());
    }

    static VoxelNode voxel(TestContext c, BlockPos p) {
        return mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level())) instanceof VoxelNode v ? v : null;
    }

    /**
     * Two islands of one block strip, two blocks over the floor, a block apart: the kit lays a cable from a source on
     * one to a resistor on the other straight across the gap (down into it and up again is dearer than the air), and the
     * circuit conducts. The piece over the gap can't take a device (it would hang on nothing) and a branch can't start on
     * it (it would get a fresh air allowance).
     */
    static final class BridgeCase implements TestCase {
        ResistorElement r;
        String err, branch = "none", place = "none";
        int gapPieces, lowPieces;

        public String name() {
            return "cable bridges a one block gap";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 3;
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 10; x++) if (x != 4) c.floor(c.at(x, 2, 1));
            var src = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) c.placeSix(SOURCE, c.at(0, 3, 1));
            src.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(50);
            }));
            r = VoxelDeviceCases.resistor(c, c.at(7, 3, 1), Direction.XP, CableCases.CORE);
            c.placeSix(GROUND, c.at(8, 3, 1));
            var hitS = Vec3.atLowerCornerOf(c.at(0, 3, 1)).add(1.0, 0.05, 0.5);
            var hitR = Vec3.atLowerCornerOf(c.at(7, 3, 1)).add(0.0, 0.05, 0.5);
            long[][] j = new long[1][];
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(0, 3, 1), net.minecraft.core.Direction.EAST, hitS, true, j);
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(7, 3, 1), net.minecraft.core.Direction.WEST, hitR, false, j);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(c.level(), LV, a, b);
            VoxelNode gap = voxel(c, c.at(4, 3, 1));
            if (gap != null) {
                gapPieces = gap.parts.size();
                long[][] jb = new long[1][];
                var e = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(4, 3, 1), net.minecraft.core.Direction.UP,
                    Vec3.atLowerCornerOf(c.at(4, 3, 1)).add(0.5, 0.05, 0.5), true, jb);
                branch = e == null ? "refused" : jb[0] == null ? "not a join" : "join";
                place = String.valueOf(gap.placeFace(net.minecraft.core.Direction.NORTH, 0.5, 0.05, 0.5));
            }
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 3; z++) if (voxel(c, c.at(4, y, z)) instanceof VoxelNode v) lowPieces += v.parts.size();
        }

        public void measure(TestContext c) {
            c.check(name() + ": laid", err == null, String.valueOf(err));
            c.check(name() + ": straight across (pieces over the gap, none below)", gapPieces > 0 && lowPieces == 0,
                gapPieces + " over, " + lowPieces + " below");
            double i = current(r);
            c.check(name() + ": the circuit conducts", Math.abs(i) > 1, String.format("%.4f A", i));
            c.check(name() + ": no branch from the piece over the gap", !"join".equals(branch), branch);
            c.check(name() + ": a device clicked onto it doesn't go on the cable's (air) face", !"YN".equals(place), place);
        }
    }
}
