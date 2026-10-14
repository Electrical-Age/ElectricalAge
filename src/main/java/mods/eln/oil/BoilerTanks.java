package mods.eln.oil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** The boiler's two tanks as one fluid capability: water goes in (tank 0), steam comes out (tank 1). */
public class BoilerTanks extends FluidStacksResourceHandler {
    public static final int WATER_MB = 8000, STEAM_MB = 32000;

    public BoilerTanks() {
        super(2, STEAM_MB);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        return index == 0 ? resource.getFluid().isSame(Fluids.WATER) : resource.getFluid().isSame(Oil.STEAM.get());
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return index == 0 ? WATER_MB : STEAM_MB;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        return index == 0 ? super.insert(index, resource, amount, tx) : 0;   // steam only comes out
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        return index == 1 ? super.extract(index, resource, amount, tx) : 0;  // water stays in
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

    /** Internal: boil `water` mB into `steam` mB (no checks). */
    void boil(int water, int steam) {
        put(0, Fluids.WATER, water() - water);
        put(1, Oil.STEAM.get(), steam() + steam);
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putInt(prefix + "water", water());
        nbt.putInt(prefix + "steam", steam());
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        put(0, Fluids.WATER, nbt.getIntOr(prefix + "water", 0));
        put(1, Oil.STEAM.get(), nbt.getIntOr(prefix + "steam", 0));
    }

    /** Tests and the 1.12-style API: fill water. */
    public int fillWater(int mB) {
        int n = Math.min(mB, WATER_MB - water());
        if (n > 0) put(0, Fluids.WATER, water() + n);
        return n;
    }

    public FluidStack steamStack() {
        return stacks.get(1).copy();
    }
}
