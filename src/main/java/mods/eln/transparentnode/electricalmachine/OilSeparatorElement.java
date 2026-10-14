package mods.eln.transparentnode.electricalmachine;

import mods.eln.fluid.ElementFluidHandler;
import mods.eln.i18n.I18N;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalStackMachineProcess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;

/** The tar sand separator (OilSeparatorDescriptor): a macerator whose finished items also give crude to its tank. */
public class OilSeparatorElement extends ElectricalMachineElement {
    final ElementFluidHandler tank = new ElementFluidHandler(OilSeparatorDescriptor.TANK_MB);

    public OilSeparatorElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        tank.setFilter(new Fluid[]{mods.eln.oil.Oil.CRUDE.get()});
        slowRefreshProcess.extraCondition = () -> tank.getCapacity() - tank.getFluidAmount() >= OilSeparatorDescriptor.CRUDE_PER_ITEM;
    }

    @Override
    public void done(ElectricalStackMachineProcess who) {
        tank.fill(new FluidStack(mods.eln.oil.Oil.CRUDE.get(), OilSeparatorDescriptor.CRUDE_PER_ITEM), true);
        super.done(who);
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt, "tank");
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Crude oil"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        return info;
    }
}
