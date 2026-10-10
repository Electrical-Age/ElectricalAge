package mods.eln.node.transparent;

import mods.eln.node.Node;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

/**
 * Proxy class for TNEs with Forge fluids.
 * 1.12: exposes the element's handler as the fluid capability (was: TE implements the 1.7.10 IFluidHandler,
 * all directions alike). The 1.7.10 TE also answered canFill() = false; there is no such query in 1.12.
 */
public class TransparentNodeEntityWithFluid extends TransparentNodeEntity {

    private IFluidHandler getFluidHandler() {
        if (!world.isRemote) {
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

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(getFluidHandler());
        }
        return super.getCapability(capability, facing);
    }

    private static class FakeFluidHandler implements IFluidHandler {
        static FakeFluidHandler INSTANCE = new FakeFluidHandler();

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[0];
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }
    }
}
