package mods.eln.oil;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** The column base's two tanks as one fluid capability: crude goes in (tank 0), residue comes out (tank 1). */
public class ColumnBaseTanks extends FluidStacksResourceHandler {
    public ColumnBaseTanks() {
        super(2, ColumnDescriptor.CRUDE_MB);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        return index == 0 ? resource.getFluid().isSame(Oil.CRUDE.get()) : resource.getFluid().isSame(Oil.Cut.RESIDUE.source.get());
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return index == 0 ? ColumnDescriptor.CRUDE_MB : ColumnDescriptor.RESIDUE_MB;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        return index == 0 ? super.insert(index, resource, amount, tx) : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        return index == 1 ? super.extract(index, resource, amount, tx) : 0;
    }

    private void put(int index, net.minecraft.world.level.material.Fluid fluid, int amount) {
        set(index, amount > 0 ? FluidResource.of(fluid) : FluidResource.EMPTY, Math.max(0, amount));
    }

    public int crude() {
        return stacks.get(0).getAmount();
    }

    public int residue() {
        return stacks.get(1).getAmount();
    }

    public void setCrude(int mB) {
        put(0, Oil.CRUDE.get(), Math.min(ColumnDescriptor.CRUDE_MB, mB));
    }

    void setResidue(int mB) {
        put(1, Oil.Cut.RESIDUE.source.get(), Math.min(ColumnDescriptor.RESIDUE_MB, mB));
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putInt(prefix + "crude", crude());
        nbt.putInt(prefix + "residue", residue());
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        setCrude(nbt.getIntOr(prefix + "crude", 0));
        setResidue(nbt.getIntOr(prefix + "residue", 0));
    }
}
