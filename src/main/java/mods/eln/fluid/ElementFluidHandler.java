package mods.eln.fluid;

import mods.eln.misc.INBTTReady;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Use one of these if you want your block to support fluids! See the steam turbine for an example.
 * <p>
 * 1.12: implemented the side-less IFluidHandler capability. 26.x: a one-tank NeoForge transfer handler
 * (Capabilities.Fluid.BLOCK, registered for transparent nodes in ElnCapabilities) with the 1.12 fill/drain calls kept
 * for the device code. The whitelist (setFilter) applies while the tank is empty; a non-empty tank takes only its fluid.
 */
public class ElementFluidHandler extends FluidStacksResourceHandler implements INBTTReady {
    private Fluid[] whitelist;
    private float fluid_heat_mb = 0;

    /**
     * Stores fluids.
     *
     * @param tankSize Tank size, in mB.
     */
    public ElementFluidHandler(int tankSize) {
        super(1, tankSize);
    }

    public void setFilter(Fluid[] whitelist) {
        assert whitelist != null;
        this.whitelist = whitelist;
    }

    public float getHeatEnergyPerMilliBucket() {
        if (fluid_heat_mb == 0 && !getFluid().isEmpty()) setHeatEnergyPerMilliBucket(getFluid().getFluid());
        return fluid_heat_mb;
    }

    private void setHeatEnergyPerMilliBucket(Fluid fluid) {
        fluid_heat_mb = (float) FuelRegistry.INSTANCE.heatEnergyPerMilliBucket(fluid);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) return false;
        FluidStack in = getFluid();
        if (!in.isEmpty()) return resource.matches(in);
        if (whitelist == null) return true;
        for (Fluid f : whitelist)
            if (sameFluid(f, resource.getFluid())) return true;
        return false;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        FluidStack now = getFluid();
        if (!now.isEmpty()) setHeatEnergyPerMilliBucket(now.getFluid());
    }

    /** The tank contents (copy). */
    public FluidStack getFluid() {
        return stacks.get(0).copy();
    }

    public int getFluidAmount() {
        return stacks.get(0).getAmount();
    }

    public int getCapacity() {
        return capacity;
    }

    /** 1.12 IFluidHandler.fill: amount accepted; changes the tank only if doFill. */
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.isEmpty()) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int n = insert(0, FluidResource.of(resource), resource.getAmount(), tx);
            if (doFill) tx.commit();
            return n;
        }
    }

    /** 1.12 IFluidHandler.drain(int, boolean): what was (or would be) taken, EMPTY if nothing. */
    public FluidStack drain(int maxDrain, boolean doDrain) {
        FluidStack in = getFluid();
        if (in.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int n = extract(0, FluidResource.of(in), maxDrain, tx);
            if (doDrain) tx.commit();
            return n > 0 ? in.copyWithAmount(n) : FluidStack.EMPTY;
        }
    }

    /** 1.7.10 fluid ids are gone; fluids are compared by registry name. */
    private static boolean sameFluid(Fluid a, Fluid b) {
        return a != null && b != null && FuelRegistry.INSTANCE.fluidName(a).equals(FuelRegistry.INSTANCE.fluidName(b));
    }

    public boolean canFill(Fluid fluid) {
        return isValid(0, FluidResource.of(fluid));
    }

    public boolean canDrain(Fluid fluid) {
        return true;
    }

    // 1.12 saved FluidTank NBT; 26.x: the fluid's registry id and amount (no data components on EA fluids)
    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        CompoundTag t = nbt.getCompoundOrEmpty(str + "tank");
        Identifier id = Identifier.tryParse(t.getStringOr("FluidName", ""));
        int amount = t.getIntOr("Amount", 0);
        Fluid fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(id);
        stacks.set(0, fluid == Fluids.EMPTY || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount));
        fluid_heat_mb = nbt.getFloatOr(str + "fhm", 0f);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        CompoundTag t = new CompoundTag();
        FluidStack in = stacks.get(0);
        if (!in.isEmpty()) {
            t.putString("FluidName", BuiltInRegistries.FLUID.getKey(in.getFluid()).toString());
            t.putInt("Amount", in.getAmount());
        }
        nbt.put(str + "tank", t);
        nbt.putFloat(str + "fhm", fluid_heat_mb);
    }
}
