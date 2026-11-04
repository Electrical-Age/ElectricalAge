package mods.eln.node.voxel;

import mods.eln.oil.Oil;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * The pipe adapter (notes/pipes/DESIGN.md section 9: the converter microblock): a flange on one face of a voxel
 * node's block, at the end of a stub of pipe from the block's middle. On that face the block offers the NeoForge fluid
 * capability (other mods' pipes and tanks: Pipez), with the stub's run behind it: water and steam go in, and come out
 * when the run has them (steam above 1 atm). Routes join the stub like any pipe.
 */
public final class AdapterPart extends VoxelPart {
    public static final String KIND = "adapter";
    /** The face it opens on (net.minecraft.core.Direction 3D data value). */
    public final int face;
    /** Server: the capability, a view of the run (synced each fluid step by the node). */
    public final Tanks tanks = new Tanks();

    public AdapterPart(int face) {
        this.face = face;
    }

    @Override
    public String kind() {
        return KIND;
    }

    /** The flange: a plate 8 voxels square, 2 deep, centred on the face. */
    @Override
    public List<int[]> boxes() {
        return List.of(switch (face) {
            case 0 -> new int[]{4, 0, 4, 12, 2, 12};
            case 1 -> new int[]{4, 14, 4, 12, 16, 12};
            case 2 -> new int[]{4, 4, 0, 12, 12, 2};
            case 3 -> new int[]{4, 4, 14, 12, 12, 16};
            case 4 -> new int[]{0, 4, 4, 2, 12, 12};
            default -> new int[]{14, 4, 4, 16, 12, 12};
        });
    }

    /** The stub from the block's middle to the face. */
    public static int[] stub(int face) {
        return switch (face) {
            case 0 -> new int[]{8, 8, 8, 8, 0, 8};
            case 1 -> new int[]{8, 8, 8, 8, 16, 8};
            case 2 -> new int[]{8, 8, 8, 8, 8, 0};
            case 3 -> new int[]{8, 8, 8, 8, 8, 16};
            case 4 -> new int[]{8, 8, 8, 0, 8, 8};
            default -> new int[]{8, 8, 8, 16, 8, 8};
        };
    }

    @Override
    public void write(CompoundTag t) {
        t.putInt("face", face);
    }

    static AdapterPart read(CompoundTag t) {
        return new AdapterPart(Math.max(0, Math.min(5, t.getIntOr("face", 0))));
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(face);
    }

    static AdapterPart deserialize(DataInputStream in) throws IOException {
        return new AdapterPart(Math.max(0, Math.min(5, (int) in.readByte())));
    }

    /**
     * The run's liquid (LIQUID: the top layer's main species) and gas (GAS: its main vapour that is a fluid, steam for
     * water), in and out, and an intake any fluid a species is can be put into, taken into the run at the next step
     * (Baughn 2026-11-03: other fluids are refused): NodeFluidView over the run, liquid to 90 % of it (a full run of
     * incompressible liquid bursts), gas to about 9 bar.
     */
    public static final class Tanks extends mods.eln.fluid.NodeFluidView {
        Tanks() {
            super(new Spec().gas(true, true, 9e5, false).intake().liquidShare(0.9));
        }

        /** Liquid shown and held (mB). */
        public int liquid() {
            return amount(LIQUID);
        }

        public int gas() {
            return amount(GAS);
        }
    }
}
