package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PipeHeaterDescriptor: an Electric Heater whose coils (its thermal load) heat a pipe node instead of a thermal face. One
 * node of its own between the pipes on its flanks (the line runs through it either way), heated through its wall link inside
 * the fluid core's step: the coils at wallT, wallUA; what passed (wallJ) comes out of the coils after the step, as the
 * Boiler's shell. Its regulator holds the node's temperature.
 */
public class PipeHeaterElement extends ElectricHeaterElement implements FluidDevice {
    /** Its node's volume (m^3): two blocks of alloy pipe's (game scale, ~2 B; PipePart.VOLUME_PER_BLOCK); not lumped. */
    public static final double NODE_VOLUME = 2 * mods.eln.node.voxel.PipePart.VOLUME_PER_BLOCK[1];
    final PipeHeaterDescriptor pdesc;
    public FluidNode fluid;
    private double[] loadedFluid;
    private List<FluidPort> ports = List.of();

    public PipeHeaterElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        pdesc = (PipeHeaterDescriptor) d;
        regulator.sense(this::oilTc);
    }

    /** Its contents' temperature, in the thermal loads' units (relative to Tref, as Tc); the coils' while it has none. */
    double oilTc() {
        if (fluid == null) return thermal.Tc;
        return fluid.T() - PhysicalConstant.Tref;   // (its own node, not lumped)
    }

    /** The power its coils take now (W). */
    public double power() {
        return resistor.getP();
    }

    /** Its regulator's set point (relative to Tref, as Tc), as its screen's slider sets it (SET_TARGET_EVENT); for tests. */
    public void setTarget(double tc) {
        regulator.setTarget(tc);
        needPublish();
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (fluid == null) return;
        fluid.wallT = thermal.Tc + PhysicalConstant.Tref;
        fluid.wallUA = pdesc.wallUA;
    }

    @Override
    public void afterFluidStep(double dt) {
        if (fluid == null) return;
        thermal.moveEnergyTo(-fluid.wallJ);
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    void makePorts() {
        long at = node.coordonate.pos().asLong();
        // (the Inline Pump's line: through its flanks, LINE_Y voxels up, so the two sit in one run)
        int a = front.left().toEnumFacing().get3DDataValue(), b = front.right().toEnumFacing().get3DDataValue();
        ports = List.of(new FluidPort(fluid, at, a, 0.5, InlinePumpElement.PORT_K, TankElement.sideSpot(a, InlinePumpElement.LINE_Y)),
            new FluidPort(fluid, at, b, 0.5, InlinePumpElement.PORT_K, TankElement.sideSpot(b, InlinePumpElement.LINE_Y)));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (fluid != null) {
            makePorts();
            FluidWorld.add(this);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public void initialize() {
        initHeater();
        // (its node 4 px tall round the line, 2 to 6 px up, its ports at its middle; not lumped into the pipes: a lump
        // splits its wall heat back by wall conductance regardless of each wall's temperature, and the pipes' iron would
        // pay ~14 % of the coils' heat, falling towards absolute zero)
        fluid = StillPotElement.restored(new FluidNode(NODE_VOLUME, node.coordonate.y + (InlinePumpElement.LINE_Y - 2) / 16.0, 4 / 16.0, false), loadedFluid,
            mods.eln.sim.Ambient.kelvin(node.coordonate));
        loadedFluid = null;
        makePorts();
        connect();
    }

    @Override
    protected double heldTc() {
        return oilTc();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? load : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (fluid != null) mods.eln.fluid.FluidSaves.put(nbt, "fluid", fluid);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loadedFluid = nbt.contains("fluidW") ? mods.eln.fluid.FluidSaves.array(nbt, "fluid") : null;
    }

    @Override
    public Map<String, String> getWaila() {
        var info = mods.eln.compat.jade.ProbeLines.of();
        double w = resistor.getP();
        info.headline(I18N.tr("Power"), mods.eln.api.Meters.watts(w < 0.5 ? 0 : w));
        if (fluid != null) {
            info.headline(I18N.tr("Contents"), String.format("%s, %s", EaUnits.composition(fluid), mods.eln.api.Meters.celsius(fluid.T() - Water.T0)));
            String visc = EaUnits.viscosity(fluid);
            boolean thick = visc != null && visc.contains(I18N.tr("heat or dilute"));
            if (thick) info.headline(I18N.tr("Viscosity"), visc);
            info.headline(I18N.tr("Coils"), mods.eln.api.Meters.celsius(thermal.Tc + PhysicalConstant.Tref - Water.T0));
            if (visc != null && !thick) info.detail(I18N.tr("Viscosity"), visc);
        }
        return info;
    }
}
