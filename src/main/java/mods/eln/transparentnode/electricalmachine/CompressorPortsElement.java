package mods.eln.transparentnode.electricalmachine;

import mods.eln.fluid.ElementFluidHandler;
import mods.eln.fluid.PortBridge;
import mods.eln.i18n.I18N;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.oil.Latex;
import mods.eln.sim.ElectricalStackMachineProcess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;

/**
 * The 230V compressor with its latex ports (CompressorPortsDescriptor; the rubber chain, 2026-11-07). Its back carries
 * two flanges (the model: mods.eln.cad.models.Compressor, IN_Y / OUT_Y at FLANGE_Z): the blue IN feeds a latex tank that
 * it presses to Raw Rubber, 250 mB at a time, while its input slot is empty; the red OUT drains a second tank, filled
 * by pressing dandelions (PressRecipes). With no EA pipe on the OUT flange, it pushes into whatever takes fluid on its back.
 */
public class CompressorPortsElement extends ElectricalMachineElement implements mods.eln.sim.fluid.FluidDevice {
    final ElementFluidHandler inTank = new ElementFluidHandler(CompressorPortsDescriptor.TANK_MB);
    final ElementFluidHandler outTank = new ElementFluidHandler(CompressorPortsDescriptor.TANK_MB);
    final PortBridge inPort = new PortBridge("latexIn", PortBridge.Mode.IN, CompressorPortsDescriptor.PORT_MB_PER_S, 0);
    final PortBridge outPort = new PortBridge("latexOut", PortBridge.Mode.OUT, CompressorPortsDescriptor.PORT_MB_PER_S,
        CompressorPortsDescriptor.OUT_HEAD_PA);

    public CompressorPortsElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        inTank.setFilter(new Fluid[]{Latex.SOURCE.get()});
        outTank.setFilter(new Fluid[]{Latex.SOURCE.get()});
        PressRecipes press = (PressRecipes) ((ElectricalMachineDescriptor) descriptor).recipe;
        // dandelions only while the out tank has room for their latex
        slowRefreshProcess.extraCondition = () -> !press.dandelions().canBeCraftedBy(getInventory().getItem(inSlotId))
            || outTank.getCapacity() - outTank.getFluidAmount() >= Latex.MB_PER_RUBBER;
        slowRefreshProcess.fluidRecipe = new ElectricalStackMachineProcess.FluidRecipe() {
            public boolean ready() {
                return inTank.getFluidAmount() >= Latex.MB_PER_RUBBER;
            }

            public ItemStack[] output() {
                return new ItemStack[]{new ItemStack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get())};
            }

            public double energy() {
                return CompressorPortsDescriptor.LATEX_JOULES;
            }

            public void consume() {
                inTank.drain(Latex.MB_PER_RUBBER, true);
            }
        };
        slowProcessList.add(time -> {
            if (!outPort.piped() && node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
                mods.eln.fluid.FluidExport.push(outTank, level, node.coordonate.pos(), back().toEnumFacing(),
                    (int) Math.ceil(CompressorPortsDescriptor.PORT_MB_PER_S * time));
        });
    }

    /** Latex in its press tank (mB; tests). */
    public int latexIn() {
        return inTank.getFluidAmount();
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
        if (inPort.stub == null || outPort.stub == null) return java.util.List.of();
        long pos = node.coordonate.pos().asLong();
        int face = back().toEnumFacing().get3DDataValue();
        return java.util.List.of(
            inPort.port(pos, face, PortBridge.endSpot(front, back(), CompressorPortsDescriptor.IN_Y, CompressorPortsDescriptor.FLANGE_Z)),
            outPort.port(pos, face, PortBridge.endSpot(front, back(), CompressorPortsDescriptor.OUT_Y, CompressorPortsDescriptor.FLANGE_Z)));
    }

    @Override
    public void beforeFluidStep(double dt) {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inPort.step(inTank, dt, t);
        outPort.step(outTank, dt, t);
    }

    @Override
    public void initialize() {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inPort.make(node.coordonate.y + CompressorPortsDescriptor.IN_Y / 16.0, t);
        outPort.make(node.coordonate.y + CompressorPortsDescriptor.OUT_Y / 16.0, t);
        super.initialize();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inPort.stub != null) mods.eln.fluid.FluidWorld.add(this);
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
            outTank.fill(new FluidStack(Latex.SOURCE.get(), Latex.MB_PER_RUBBER), true);
        super.done(who);
    }

    /** Buckets and other mods' pipes see the latex it makes (the OUT tank). */
    @Override
    public Object getFluidHandler() {
        return outTank;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        inTank.writeToNBT(nbt, "latexInTank");
        outTank.writeToNBT(nbt, "latexOutTank");
        inPort.write(nbt);
        outPort.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        inTank.readFromNBT(nbt, "latexInTank");
        outTank.readFromNBT(nbt, "latexOutTank");
        inPort.read(nbt);
        outPort.read(nbt);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Latex in (blue)"), inTank.getFluidAmount() + " / " + inTank.getCapacity() + " mB");
        info.put(I18N.tr("Latex out (red)"), outTank.getFluidAmount() + " / " + outTank.getCapacity() + " mB");
        return info;
    }
}
