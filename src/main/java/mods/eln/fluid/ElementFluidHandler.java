package mods.eln.fluid;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

/**
 * Use one of these if you want your block to support Forge fluids!
 * <p>
 * See the steam turbine for an example.
 */
public class ElementFluidHandler implements IFluidHandler, INBTTReady {
    private Fluid[] whitelist;
    private float fluid_heat_mb = 0;
    FluidTank tank;

    /**
     * Stores fluids.
     *
     * @param tankSize Tank size, in mB.
     */
    public ElementFluidHandler(int tankSize) {
        tank = new FluidTank(tankSize);
    }

    public void setFilter(Fluid[] whitelist) {
        assert whitelist != null;
        this.whitelist = whitelist;
    }

    public float getHeatEnergyPerMilliBucket() {
        if (fluid_heat_mb == 0 && tank.getFluid() != null) setHeatEnergyPerMilliBucket(tank.getFluid().getFluid());
        return fluid_heat_mb;
    }

    private void setHeatEnergyPerMilliBucket(Fluid fluid) {
        fluid_heat_mb = (float) FuelRegistry.INSTANCE.heatEnergyPerMilliBucket(fluid);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (tank.getFluidAmount() > 0) {
            // No change in type of fluid.
            return tank.fill(resource, doFill);
        } else if (whitelist == null) {
            // May have a different fluid.
            setHeatEnergyPerMilliBucket(resource.getFluid());
            return tank.fill(resource, doFill);
        } else {
            for (Fluid whitelisted : whitelist) {
                if (whitelisted == resource.getFluid()) {
                    setHeatEnergyPerMilliBucket(resource.getFluid());
                    return tank.fill(resource, doFill);
                }
            }
            return 0;
        }
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource.isFluidEqual(tank.getFluid())) {
            return tank.drain(resource.amount, doDrain);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return tank.getTankProperties();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        tank.readFromNBT(nbt.getCompoundTag(str + "tank"));
        fluid_heat_mb = nbt.getFloat(str + "fhm");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {
        NBTTagCompound t = new NBTTagCompound();
        tank.writeToNBT(t);
        nbt.setTag(str + "tank", t);
        nbt.setFloat(str + "fhm", fluid_heat_mb);
        return nbt;
    }
}
