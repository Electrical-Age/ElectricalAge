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

/**
 * The tar sand separator (OilSeparatorDescriptor): a macerator whose finished items also give crude to its tank, which
 * its outlet on the back pushes into whatever takes it there (Baughn 2026-11-03: it lacked a pipe output).
 */
public class OilSeparatorElement extends ElectricalMachineElement implements mods.eln.sim.fluid.FluidDevice {
    final ElementFluidHandler tank = new ElementFluidHandler(OilSeparatorDescriptor.TANK_MB);

    public OilSeparatorElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        tank.setFilter(new Fluid[]{mods.eln.oil.Oil.CRUDE.get()});
        slowRefreshProcess.extraCondition = () -> tank.getCapacity() - tank.getFluidAmount() >= OilSeparatorDescriptor.CRUDE_PER_ITEM;
        slowProcessList.add(time -> {
            // (an EA pipe on its outlet's flange takes it, PortBridge; else whatever takes fluid on that face)
            if (!outletPort.piped() && node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
                mods.eln.fluid.FluidExport.push(tank, level, node.coordonate.pos(), outlet().toEnumFacing(), (int) Math.ceil(OilSeparatorDescriptor.OUTLET_MB_PER_S * time));
            if (Math.abs(basin() - shownBasin) > 0.02) needPublish();
        });
    }

    /** Its outlet on an EA pipe: up to OUTLET_MB_PER_S against up to OUTLET_HEAD_PA. */
    final mods.eln.fluid.PortBridge outletPort = new mods.eln.fluid.PortBridge("outlet", mods.eln.fluid.PortBridge.Mode.OUT,
        OilSeparatorDescriptor.OUTLET_MB_PER_S, OilSeparatorDescriptor.OUTLET_HEAD_PA);

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (outletPort.stub == null) return java.util.List.of();
        return java.util.List.of(outletPort.port(node.coordonate.pos().asLong(), outlet().toEnumFacing().get3DDataValue(),
            mods.eln.fluid.PortBridge.endSpot(front, outlet(), OilSeparatorDescriptor.OUTLET_Y, OilSeparatorDescriptor.OUTLET_Z)));
    }

    @Override
    public void beforeFluidStep(double dt) {
        outletPort.step(tank, dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void initialize() {
        outletPort.make(node.coordonate.y + OilSeparatorDescriptor.OUTLET_Y / 16.0, mods.eln.sim.Ambient.kelvin(node.coordonate));
        super.initialize();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (outletPort.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    /** Its outlet: the back, where the model's oil outlet pipe comes out (mods.eln.cad.models.TarSandSeparator). */
    public mods.eln.misc.Direction outlet() {
        return front.back();
    }

    @Override
    public void done(ElectricalStackMachineProcess who) {
        tank.fill(new FluidStack(mods.eln.oil.Oil.CRUDE.get(), OilSeparatorDescriptor.CRUDE_PER_ITEM), true);
        super.done(who);
    }

    /** Its crude tank's fill (0..1): the oil in its basin, drawn; last published. */
    public float basin() {
        return (float) tank.getFluidAmount() / tank.getCapacity();
    }

    float shownBasin = -1;

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            shownBasin = basin();
            stream.writeFloat(shownBasin);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt, "tank");
        outletPort.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        outletPort.read(nbt);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Crude oil"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Outlet"), I18N.tr("the pipe on its back"));
        return info;
    }
}
