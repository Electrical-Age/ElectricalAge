package mods.eln.oil;

import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** A column segment: one cut's tank, filled by the base below; pipes and buckets take from it. */
public class ColumnSegmentElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    /** On EA pipes: its cut out of the draw-off nozzle on its front (mods.eln.cad.models.Column); natural gas as a gas. */
    final mods.eln.fluid.PortBridge drawOff = new mods.eln.fluid.PortBridge("drawOff", mods.eln.fluid.PortBridge.Mode.OUT, ColumnDescriptor.PIPE_MB_PER_S, ColumnDescriptor.PIPE_HEAD_PA);

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (drawOff.stub == null) return java.util.List.of();
        int face = front.toEnumFacing().get3DDataValue();
        return java.util.List.of(drawOff.port(node.coordonate.pos().asLong(), face, TankElement.sideSpot(face, 8)));
    }

    @Override
    public void beforeFluidStep(double dt) {
        drawOff.step(tank, dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (drawOff.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    public final OutputTank tank = new OutputTank(ColumnDescriptor.SEGMENT_MB);
    /** Set by the base on its scan: which cut this segment gets (null: not on a base). */
    Oil.Cut cut;

    public ColumnSegmentElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        // face the same way as the base or segment below, so the stacked tower's nozzles and ladder line up
        var below = mods.eln.node.NodeManager.instance == null ? null
            : mods.eln.node.NodeManager.instance.getNodeFromCoordonate(node.coordonate.newWithOffset(0, -1, 0));
        if (below instanceof TransparentNode tn && (tn.element instanceof ColumnBaseElement || tn.element instanceof ColumnSegmentElement))
            front = tn.element.front;
        drawOff.make(node.coordonate.y + 0.5, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        drawOff.write(nbt);
        tank.writeToNBT(nbt, "tank");
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        drawOff.read(nbt);
        tank.readFromNBT(nbt, "tank");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Cut"), cut == null ? I18N.tr("not on a column base") : cut.display);
        var f = tank.getFluid();
        info.put(I18N.tr("Tank"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB" + (f.isEmpty() ? "" : " " + f.getHoverName().getString()));
        return info;
    }
}
