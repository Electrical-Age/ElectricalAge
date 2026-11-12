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
        cases.add(new DeviceInAirCase());
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

    /**
     * A machine on a pillar a block from a wall (a capacitor bank), its side face over the air: the kit routes from a source on the wall's
     * top down the wall and straight across the air to the edge of the bank's side face, arriving as a cable on a floor
     * there would (EA's edge rule), and the bank charges through it.
     */
    static final class DeviceInAirCase implements TestCase {
        mods.eln.transparentnode.capacitorbank.CapacitorBankElement bank;
        String err, ends = "";

        public String name() {
            return "cable reaches a machine in the air";
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

        public int floorDepth() {
            return 7;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext c) {
            BlockPos at = c.at(4, 3, 3);
            c.floor(at.below());   // (it needs a block to stand on; the cable's block beside it has none)
            bank = (mods.eln.transparentnode.capacitorbank.CapacitorBankElement) c.placeTransparent(CapacitorBankCases.BANK, at);
            Direction l = bank.front.left(), r = bank.front.right();
            c.floor(Wp10aCases.off(at, r).below());
            c.placeSix(GROUND, Wp10aCases.off(at, r));
            BlockPos wall = Wp10aCases.off(Wp10aCases.off(at, l), l);
            for (int y = 1; y <= 3; y++) c.floor(wall.atY(c.at(0, y, 0).getY()));
            BlockPos top = wall.atY(c.at(0, 4, 0).getY());
            var src = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) c.placeSix(SOURCE, top);
            src.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(20);
            }));
            net.minecraft.core.Direction toBank = r.toEnumFacing(), bankFace = l.toEnumFacing();
            Vec3 hitS = Vec3.atCenterOf(top).add(toBank.getStepX() * 0.5, -0.45, toBank.getStepZ() * 0.5);
            Vec3 hitB = Vec3.atCenterOf(at).add(bankFace.getStepX() * 0.5, -0.45, bankFace.getStepZ() * 0.5);
            long[][] j = new long[1][];
            int hv = CablePart.tierOf("High Voltage Cable");
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), top, toBank, hitS, true, j, hv);
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), at, bankFace, hitB, false, j, hv);
            ends = a + " -> " + b;
            err = a == null || b == null ? "no end" : mods.eln.node.voxel.CablePlacer.routeAndLayForTests(c.level(), hv, a, b);
            if (voxel(c, Wp10aCases.off(at, l)) instanceof VoxelNode v)
                for (var part : v.parts)
                    if (part instanceof CablePart cp && !mods.eln.node.voxel.CablePlacer.supported(c.level(), v.coordonate.pos(), cp)) inAir++;
        }

        int inAir;

        public void measure(TestContext c) {
            c.check(name() + ": laid", err == null, err + " (" + ends + ")");
            c.check(name() + ": across the air in front of the bank", inAir > 0, inAir + " unsupported pieces there");
            double u = bank.voltage();
            c.check(name() + ": the bank charges through it", u > 1, String.format("%.2f V", u));
        }
    }
}

