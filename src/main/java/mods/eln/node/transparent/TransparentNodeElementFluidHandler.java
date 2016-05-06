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
    private Fluid filter;
    FluidTank tank;

    /**
     * Stores fluids.
     *
     * @param tankSize Tank size, in mB.
     */
    public TransparentNodeElementFluidHandler(int tankSize) {
        tank = new FluidTank(tankSize);
    }

    public void setFilter(Fluid filter) {
        assert filter != null;
        this.filter = filter;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (filter != null && resource.getFluid().getID() != filter.getID()) return 0;
        return tank.fill(resource, doFill);
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
        if (filter != null && fluid.getID() != filter.getID()) return false;
        return true;
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
