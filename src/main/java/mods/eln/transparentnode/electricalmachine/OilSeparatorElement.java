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
 * The tar sand separator (OilSeparatorDescriptor): a macerator whose finished items also give crude to its basin (a
 * fluid node that breathes: DeviceOutlet), which its outlet pump on the back sends down the pipe there (Baughn
 * 2026-11-10: PortBridge push becomes the device's pump), or unpiped pushes into whatever takes it there (Baughn
 * 2026-11-03: it lacked a pipe output). Tar sand's crude is dead: no natural gas.
 */
public class OilSeparatorElement extends ElectricalMachineElement implements mods.eln.sim.fluid.FluidDevice {
    /** Its basin and outlet: a pump of OUTLET_MB_PER_S free flow and OUTLET_HEAD_PA shut-off; the old PortBridge's key. */
    public final mods.eln.fluid.DeviceOutlet outlet = new mods.eln.fluid.DeviceOutlet("outlet", OilSeparatorDescriptor.TANK_MB,
        OilSeparatorDescriptor.OUTLET_MB_PER_S, OilSeparatorDescriptor.OUTLET_HEAD_PA, true, false);

    public OilSeparatorElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        slowRefreshProcess.extraCondition = () -> outlet.room(OilSeparatorDescriptor.CRUDE_PER_ITEM);
        slowProcessList.add(time -> {
            // (an EA pipe on its outlet's flange takes it, its pump; else whatever takes fluid on that face)
            if (node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
                outlet.export(level, node.coordonate.pos(), outlet().toEnumFacing(), (int) Math.ceil(OilSeparatorDescriptor.OUTLET_MB_PER_S * time));
            if (Math.abs(basin() - shownBasin) > 0.02) needPublish();
        });
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (outlet.outlet == null) return java.util.List.of();
        return java.util.List.of(outlet.port(node.coordonate.pos().asLong(), outlet().toEnumFacing().get3DDataValue(),
            mods.eln.fluid.PortBridge.endSpot(front, outlet(), OilSeparatorDescriptor.OUTLET_Y, OilSeparatorDescriptor.OUTLET_Z)));
    }

    @Override
    public java.util.List<Link> fluidLinks() {
        return outlet.links();
    }

    @Override
    public java.util.List<Vent> fluidVents() {
        return outlet.vents();
    }

    @Override
    public void beforeFluidStep(double dt) {
        outlet.before(mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void afterFluidStep(double dt) {
        outlet.after();
    }

    @Override
    public void initialize() {
        outlet.make(node.coordonate.y, node.coordonate.y + OilSeparatorDescriptor.OUTLET_Y / 16.0, mods.eln.sim.Ambient.kelvin(node.coordonate));
        super.initialize();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (outlet.outlet != null) mods.eln.fluid.FluidWorld.add(this);
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
        outlet.made(mods.eln.oil.Oil.CRUDE.get(), OilSeparatorDescriptor.CRUDE_PER_ITEM);
        super.done(who);
    }

    /** Its basin's fill (0..1 of its old tank's size): the crude in it as its view shows it, drawn; last published. */
    public float basin() {
        return (float) Math.min(1, (double) outlet.view.amount(mods.eln.fluid.NodeFluidView.LIQUID) / OilSeparatorDescriptor.TANK_MB);
    }

    /** Put mB of fluid straight into its basin (tests). */
    public void putFluid(Fluid fluid, int mB) {
        outlet.put(fluid, mB, mods.eln.sim.Ambient.kelvin(node.coordonate));
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
        return outlet.view;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        outlet.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        // (an old save's tank goes into the basin, its PortBridge stub's contents into the outlet)
        outlet.read(nbt, OilSeparatorDescriptor.TANK_MB);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Crude oil"), String.format("%.0f / %.0f mB", outlet.liquidMb(), outlet.capacityMb()));
        info.put(I18N.tr("Outlet"), I18N.tr("the pipe on its back"));
        return info;
    }
}
