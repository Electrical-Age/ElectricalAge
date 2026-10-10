package mods.eln.fluid;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * Use one of these if you want your block to support Forge fluids!
 * <p>
 * See the steam turbine for an example.
 * 1.12: implements the side-less fluid capability interface (no direction argument any more).
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
            for (int i = 0; i < whitelist.length; i++) {
                if (sameFluid(whitelist[i], resource.getFluid())) {
                    setHeatEnergyPerMilliBucket(resource.getFluid());
                    return tank.fill(resource, doFill);
                }
            }
            return 0;
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource.isFluidEqual(tank.getFluid()))
            return tank.drain(resource.amount, doDrain);
        else
            return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
    }

    /** 1.7.10 fluid ids are gone; fluids are compared by registry name. */
    private static boolean sameFluid(Fluid a, Fluid b) {
        return a != null && b != null && a.getName().equals(b.getName());
    }

    public boolean canFill(Fluid fluid) {
        if (tank.getFluidAmount() > 0) {
            return sameFluid(tank.getFluid().getFluid(), fluid);
        } else {
            for (int i = 0; i < whitelist.length; i++) {
                if (sameFluid(whitelist[i], fluid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canDrain(Fluid fluid) {
        return true;
    }

    public FluidTankInfo[] getTankInfo() {
        return new FluidTankInfo[]{tank.getInfo()};
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[]{new FluidTankProperties(tank.getFluid(), tank.getCapacity())};
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        tank.readFromNBT(nbt.getCompoundTag(str + "tank"));
        fluid_heat_mb = nbt.getFloat(str + "fhm");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        NBTTagCompound t = new NBTTagCompound();
        tank.writeToNBT(t);
        nbt.setTag(str + "tank", t);
        nbt.setFloat(str + "fhm", fluid_heat_mb);
    }
}
