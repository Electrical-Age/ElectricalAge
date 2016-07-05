package mods.eln.node.transparent;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

/**
 * Use one of these if you want your block to support Forge fluids!
 *
 * See the steam turbine for an example.
 *
 * TODO: This is not really related to transparent nodes and could probably used with other items too - should we
 * create a package fluid in eln that contains all fluid related code?
 */
public class TransparentNodeElementFluidHandler implements IFluidHandler, INBTTReady {
    private Fluid[] whitelist;
    FluidTank tank;

    /**
     * Stores fluids.
     *
     * @param tankSize Tank size, in mB.
     */
    public TransparentNodeElementFluidHandler(int tankSize) {
        tank = new FluidTank(tankSize);
    }

    public void setFilter(Fluid[] whitelist) {
        assert whitelist != null;
        this.whitelist = whitelist;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (whitelist == null || tank.getFluidAmount() > 0) {
            return tank.fill(resource, doFill);
        } else {
            int resourceId = resource.getFluidID();
            for (int i = 0; i < whitelist.length; i++) {
                if (whitelist[i].getID() == resourceId) {
                    return tank.fill(resource, doFill);
                }
            }
        }
        return 0;
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
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        NBTTagCompound t = new NBTTagCompound();
        tank.writeToNBT(t);
        nbt.setTag(str + "tank", t);
    }
}
