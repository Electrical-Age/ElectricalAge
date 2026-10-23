package mods.eln.node.voxel;

import mods.eln.oil.Oil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
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

    /** Water (tank 0) and steam (tank 1), in and out; what pipes do between steps is taken in at the next. */
    public static final class Tanks extends FluidStacksResourceHandler {
        public static final int WATER_MB = 8000, STEAM_MB = 32000;
        private int shownWater, shownSteam;
        /** How much more the run can take (mB): water to 90% full, steam to about 9 bar (set at each step). */
        private int roomWater, roomSteam;

        Tanks() {
            super(2, STEAM_MB);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            if (resource.isEmpty()) return false;
            return index == 0 ? resource.getFluid().isSame(Fluids.WATER) : resource.getFluid().isSame(Oil.STEAM.get());
        }

        @Override
        protected int getCapacity(int index, FluidResource resource) {
            // (the run behind is often small: let pipes fill only what it can hold, or the pressure would burst it)
            return index == 0 ? Math.min(WATER_MB, shownWater + roomWater) : Math.min(STEAM_MB, shownSteam + roomSteam);
        }

        private void put(int index, net.minecraft.world.level.material.Fluid fluid, int amount) {
            set(index, amount > 0 ? FluidResource.of(fluid) : FluidResource.EMPTY, Math.max(0, amount));
        }

        public int water() {
            return stacks.get(0).getAmount();
        }

        public int steam() {
            return stacks.get(1).getAmount();
        }

        /** What pipes did since the last step: {water in, water out, steam in, steam out} (mB). */
        int[] take() {
            int w = water() - shownWater, s = steam() - shownSteam;
            shownWater = water();
            shownSteam = steam();
            return new int[]{Math.max(0, w), Math.max(0, -w), Math.max(0, s), Math.max(0, -s)};
        }

        void show(int water, int steam, int roomW, int roomS) {
            roomWater = Math.max(0, roomW);
            roomSteam = Math.max(0, roomS);
            show(water, steam);
        }

        void show(int water, int steam) {
            put(0, Fluids.WATER, Math.min(WATER_MB, water));
            put(1, Oil.STEAM.get(), Math.min(STEAM_MB, steam));
            shownWater = water();
            shownSteam = steam();
        }
    }
}
