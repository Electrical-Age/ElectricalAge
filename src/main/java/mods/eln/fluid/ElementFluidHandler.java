package mods.eln.fluid;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

/**
 * Use one of these if you want your block to support Forge fluids!
 *
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
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (tank.getFluidAmount() > 0) {
            // No change in type of fluid.
            return tank.fill(resource, doFill);
        } else if (whitelist == null) {
            // May have a different fluid.
            setHeatEnergyPerMilliBucket(resource.getFluid());
            return tank.fill(resource, doFill);
        } else {
            int resourceId = resource.getFluidID();
            for (int i = 0; i < whitelist.length; i++) {
                if (whitelist[i].getID() == resourceId) {
                    setHeatEnergyPerMilliBucket(resource.getFluid());
                    return tank.fill(resource, doFill);
                }
            }
            return 0;
        }
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
       if (resource.isFluidEqual(tank.getFluid()))
           return tank.drain(resource.amount, doDrain);
       else
           return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        int fluidId = fluid.getID();
        if (tank.getFluidAmount() > 0) {
            return tank.getFluid().getFluidID() == fluidId;
        } else {
            for (int i = 0; i < whitelist.length; i++) {
                if (whitelist[i].getID() == fluidId) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return true;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[]{tank.getInfo()};
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
