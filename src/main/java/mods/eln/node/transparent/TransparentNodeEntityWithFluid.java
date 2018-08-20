package mods.eln.node.transparent;

import mods.eln.node.Node;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * Proxy class for TNEs with Forge fluids.
 */
public class TransparentNodeEntityWithFluid extends TransparentNodeEntity implements IFluidHandler {

    private IFluidHandler getFluidHandler() {
        if (!worldObj.isRemote) {
            Node node = getNode();
            if (node != null && node instanceof TransparentNode) {
                TransparentNode tn = (TransparentNode) node;
                IFluidHandler i = tn.getFluidHandler();
                if (i != null) {
                    return i;
                }
            }
        }
        return FakeFluidHandler.INSTANCE;
    }

    /**
     * Fills fluid into internal tanks, distribution is left entirely to the IFluidHandler.
     *
     * @param from     Orientation the Fluid is pumped in from.
     * @param resource FluidStack representing the Fluid and maximum amount of fluid to be filled.
     * @param doFill   If false, fill will only be simulated.
     * @return Amount of resource that was (or would have been, if simulated) filled.
     */
    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        return getFluidHandler().fill(from, resource, doFill);
    }

    /**
     * Drains fluid out of internal tanks, distribution is left entirely to the IFluidHandler.
     *
     * @param from     Orientation the Fluid is drained to.
     * @param resource FluidStack representing the Fluid and maximum amount of fluid to be drained.
     * @param doDrain  If false, drain will only be simulated.
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        return getFluidHandler().drain(from, resource, doDrain);
    }

    /**
     * Drains fluid out of internal tanks, distribution is left entirely to the IFluidHandler.
     * <p>
     * This method is not Fluid-sensitive.
     *
     * @param from     Orientation the fluid is drained to.
     * @param maxDrain Maximum amount of fluid to drain.
     * @param doDrain  If false, drain will only be simulated.
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return getFluidHandler().drain(from, maxDrain, doDrain);
    }

    /**
     * Returns true if the given fluid can be inserted into the given direction.
     * <p>
     * More formally, this should return true if fluid is able to enter from the given direction.
     *
     * @param from
     * @param fluid
     */
    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        return false;
    }

    /**
     * Returns true if the given fluid can be extracted from the given direction.
     * <p>
     * More formally, this should return true if fluid is able to leave from the given direction.
     *
     * @param from
     * @param fluid
     */
    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        return getFluidHandler().canDrain(from, fluid);
    }

    /**
     * Returns an array of objects which represent the internal tanks. These objects cannot be used
     * to manipulate the internal tanks. See {@link FluidTankInfo}.
     *
     * @param from Orientation determining which tanks should be queried.
     * @return Info for the relevant internal tanks.
     */
    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        return getFluidHandler().getTankInfo(from);
    }

    private static class FakeFluidHandler implements IFluidHandler {
        static FakeFluidHandler INSTANCE = new FakeFluidHandler();

        @Override
        public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
            return null;
        }

        @Override
        public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
            return null;
        }

        @Override
        public boolean canFill(EnumFacing from, Fluid fluid) {
            return false;
        }

        @Override
        public boolean canDrain(EnumFacing from, Fluid fluid) {
            return false;
        }

        @Override
        public FluidTankInfo[] getTankInfo(EnumFacing from) {
            return new FluidTankInfo[0];
        }
    }
}
