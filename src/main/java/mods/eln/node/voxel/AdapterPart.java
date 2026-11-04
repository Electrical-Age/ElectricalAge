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
     * The run's liquid (tank 0: the top layer's main species) and gas (tank 1: its main vapour that is a fluid, steam
     * for water), in and out, and an intake (tank 2) any fluid a species is can be put into, taken into the run at the
     * next step (Baughn 2026-11-03: other fluids are refused). What pipes do between steps is taken in at the next.
     */
    public static final class Tanks extends FluidStacksResourceHandler {
        public static final int WATER_MB = 8000, STEAM_MB = 32000;
        public static final int LIQUID = 0, GAS = 1, INTAKE = 2;
        private final int[] shown = new int[3];
        private final net.minecraft.world.level.material.Fluid[] shownFluid = new net.minecraft.world.level.material.Fluid[3];
        /** How much more the run can take: liquid to 90% full (mB of any liquid: they go by volume), gas to about 9 bar. */
        private int roomLiquid, roomGas;

        Tanks() {
            super(3, STEAM_MB);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            if (resource.isEmpty()) return false;
            var p = mods.eln.fluid.FluidSpecies.of(resource.getFluid());
            if (p == null) return false;
            return index == INTAKE || (index == GAS) == p.vapour();
        }

        @Override
        protected int getCapacity(int index, FluidResource resource) {
            // (the run behind is often small: let pipes fill only what it can hold, or the pressure would burst it)
            var p = mods.eln.fluid.FluidSpecies.of(resource.getFluid());
            boolean gas = p != null && p.vapour();
            if (index == INTAKE) return gas ? Math.min(STEAM_MB, roomGas) : Math.min(WATER_MB, roomLiquid);
            return index == LIQUID ? Math.min(WATER_MB, shown[LIQUID] + roomLiquid) : Math.min(STEAM_MB, shown[GAS] + roomGas);
        }

        private void put(int index, net.minecraft.world.level.material.Fluid fluid, int amount) {
            set(index, amount > 0 && fluid != null ? FluidResource.of(fluid) : FluidResource.EMPTY, fluid == null ? 0 : Math.max(0, amount));
        }

        /** Liquid shown and held (mB). */
        public int liquid() {
            return stacks.get(LIQUID).getAmount();
        }

        public int gas() {
            return stacks.get(GAS).getAmount();
        }

        /** What pipes did since the last step, per fluid: mB in (positive) or out (negative). */
        java.util.List<java.util.Map.Entry<net.minecraft.world.level.material.Fluid, Integer>> take() {
            java.util.List<java.util.Map.Entry<net.minecraft.world.level.material.Fluid, Integer>> out = new java.util.ArrayList<>();
            for (int i = LIQUID; i <= GAS; i++) {
                var st = stacks.get(i);
                var f = st.isEmpty() ? shownFluid[i] : st.getFluid();
                int d = st.getAmount() - shown[i];
                if (d != 0 && f != null) out.add(java.util.Map.entry(f, d));
                shown[i] = st.getAmount();
            }
            var in = stacks.get(INTAKE);
            if (!in.isEmpty() && in.getAmount() > 0) out.add(java.util.Map.entry(in.getFluid(), in.getAmount()));
            set(INTAKE, FluidResource.EMPTY, 0);
            return out;
        }

        void show(net.minecraft.world.level.material.Fluid liquid, int liquidMb, net.minecraft.world.level.material.Fluid gas, int gasMb, int roomL, int roomG) {
            roomLiquid = Math.max(0, roomL);
            roomGas = Math.max(0, roomG);
            put(LIQUID, liquid, Math.min(WATER_MB, liquidMb));
            put(GAS, gas, Math.min(STEAM_MB, gasMb));
            shownFluid[LIQUID] = liquid;
            shownFluid[GAS] = gas;
            shown[LIQUID] = liquid();
            shown[GAS] = gas();
        }
    }
}
