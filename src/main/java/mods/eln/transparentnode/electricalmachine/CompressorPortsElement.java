package mods.eln.transparentnode.electricalmachine;

import mods.eln.fluid.DeviceInlet;
import mods.eln.fluid.DeviceOutlet;
import mods.eln.fluid.PortBridge;
import mods.eln.i18n.I18N;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.oil.Latex;
import mods.eln.sim.ElectricalStackMachineProcess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * The 230V compressor with its latex ports (CompressorPortsDescriptor; the rubber chain, 2026-11-07). Its back carries
 * two flanges (the model: mods.eln.cad.models.Compressor, IN_Y / OUT_Y at FLANGE_Z): the line on the blue IN fills a
 * latex vessel (DeviceInlet) that it presses to Raw Rubber, 250 mB at a time, while its input slot is empty; a pump
 * sends the latex of a second vessel, an open basin filled by pressing dandelions (PressRecipes), out of the red OUT
 * (DeviceOutlet). With no EA pipe on the OUT flange, it pushes into whatever takes fluid on its back.
 */
public class CompressorPortsElement extends ElectricalMachineElement implements mods.eln.sim.fluid.FluidDevice {
    public final DeviceInlet inlet = new DeviceInlet("latexIn", CompressorPortsDescriptor.TANK_MB, Latex.SOURCE.get());
    public final DeviceOutlet outlet = new DeviceOutlet("latexOut", CompressorPortsDescriptor.TANK_MB / 0.95, CompressorPortsDescriptor.PORT_MB_PER_S,
        CompressorPortsDescriptor.OUT_HEAD_PA, true, false).only(Latex.SOURCE.get()).keys("latexOutVessel", "latexOutTank");
    /** Latex a pressing has used that is still in the IN vessel (mB): it comes out before the next fluid step, between
     *  the view's sync and show. */
    double owed;

    public CompressorPortsElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        PressRecipes press = (PressRecipes) ((ElectricalMachineDescriptor) descriptor).recipe;
        // dandelions only while the out vessel has room for their latex
        slowRefreshProcess.extraCondition = () -> !press.dandelions().canBeCraftedBy(getInventory().getItem(inSlotId))
            || outlet.room(Latex.MB_PER_RUBBER);
        slowRefreshProcess.fluidRecipe = new ElectricalStackMachineProcess.FluidRecipe() {
            public boolean ready() {
                return inlet.feedMb() - owed >= Latex.MB_PER_RUBBER;
            }

            public ItemStack[] output() {
                return new ItemStack[]{new ItemStack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get())};
            }

            public double energy() {
                return CompressorPortsDescriptor.LATEX_JOULES;
            }

            public void consume() {
                owed += Latex.MB_PER_RUBBER;
            }
        };
        slowProcessList.add(time -> {
            if (node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
                outlet.export(level, node.coordonate.pos(), back().toEnumFacing(), (int) Math.ceil(CompressorPortsDescriptor.PORT_MB_PER_S * time));
        });
    }

    /** Latex in its IN vessel that no pressing has used (mB; tests, Jade). */
    public int latexIn() {
        return (int) Math.round(inlet.feedMb() - owed);
    }

    /** Latex in its OUT vessel (mB; tests, Jade). */
    public int latexOut() {
        return (int) Math.round(outlet.liquidMb());
    }

    /** Put mB of latex straight into its IN (or OUT) vessel (tests). */
    public void putLatex(boolean in, int mB) {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        if (in) inlet.put(Latex.SOURCE.get(), mB, t);
        else outlet.put(Latex.SOURCE.get(), mB, t);
    }

    /** Both flanges are on the back. */
    public mods.eln.misc.Direction back() {
        return front.back();
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (inlet.stub == null || outlet.outlet == null) return java.util.List.of();
        long pos = node.coordonate.pos().asLong();
        int face = back().toEnumFacing().get3DDataValue();
        return java.util.List.of(
            inlet.port(pos, face, PortBridge.endSpot(front, back(), CompressorPortsDescriptor.IN_Y, CompressorPortsDescriptor.FLANGE_Z)),
            outlet.port(pos, face, PortBridge.endSpot(front, back(), CompressorPortsDescriptor.OUT_Y, CompressorPortsDescriptor.FLANGE_Z)));
    }

    @Override
    public java.util.List<Link> fluidLinks() {
        var all = new java.util.ArrayList<>(inlet.links());
        all.addAll(outlet.links());
        return all;
    }

    @Override
    public java.util.List<Vent> fluidVents() {
        var all = new java.util.ArrayList<>(inlet.vents());
        all.addAll(outlet.vents());
        return all;
    }

    @Override
    public void beforeFluidStep(double dt) {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inlet.sync(dt, t);
        if (owed > 0) owed -= inlet.take(owed);
        inlet.settle(dt);
        outlet.before(t);
    }

    @Override
    public void afterFluidStep(double dt) {
        inlet.after();
        outlet.after();
    }

    @Override
    public void initialize() {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inlet.make(node.coordonate.y, node.coordonate.y + CompressorPortsDescriptor.IN_Y / 16.0, t);
        outlet.make(node.coordonate.y, node.coordonate.y + CompressorPortsDescriptor.OUT_Y / 16.0, t);
        super.initialize();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    @Override
    public void done(ElectricalStackMachineProcess who) {
        PressRecipes press = (PressRecipes) ((ElectricalMachineDescriptor) descriptor).recipe;
        if (who.lastRecipe == press.dandelions())
            outlet.made(Latex.SOURCE.get(), Latex.MB_PER_RUBBER);
        super.done(who);
    }

    /** Buckets and other mods' pipes see the latex it makes (the OUT vessel). */
    @Override
    public Object getFluidHandler() {
        return outlet.view;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        inlet.write(nbt);
        outlet.write(nbt);
        nbt.putDouble("latexOwed", owed);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        inlet.read(nbt, "latexInTank", CompressorPortsDescriptor.TANK_MB);
        outlet.read(nbt, CompressorPortsDescriptor.TANK_MB);
        owed = nbt.getDoubleOr("latexOwed", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Latex in (blue)"), latexIn() + " / " + CompressorPortsDescriptor.TANK_MB + " mB");
        info.put(I18N.tr("Latex out (red)"), latexOut() + " / " + CompressorPortsDescriptor.TANK_MB + " mB");
        return info;
    }
}
