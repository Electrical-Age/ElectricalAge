package mods.eln.sixnode.pipetransmitter;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.PipeSensorPart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A pipe sensor device (PipeTransmitterDescriptor): reads the run of the pipe at its port, in its descriptor's mode, and
 * puts reading / full scale out as 0-50 V on its front pin, as the pipe sensor fitted on a pipe did (PipeSensorPart:
 * same modes, full scales and right-click stepping). The device holds no fluid: the pipe laid to it ends capped at the
 * port, so the fluid world is unchanged. Without a pipe at the port it reads nothing and outputs 0 V.
 */
public class PipeTransmitterElement extends SixNodeElement {
    public final PipeTransmitterDescriptor descriptor;
    public final NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");
    public final NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
    /** Full scale (bar, degrees C or % wet). */
    public double scale;
    /** The last reading, and whether a pipe was at the port then. */
    public double reading;
    public boolean connected;

    public PipeTransmitterElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (PipeTransmitterDescriptor) descriptor;
        scale = this.descriptor.defaultScale();
        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);
        slowProcessList.add(new Reader());
    }

    /** Reads the run every slow step (as the fitted sensor read it before each fluid step). */
    final class Reader implements IProcess {
        @Override
        public void process(double time) {
            read();
        }
    }

    /** The port (block-local voxels): PORT_HEIGHT off the face, over the device's centre. */
    public int[] port() {
        int face = side.toEnumFacing().get3DDataValue(), n = CablePart.normalAxis(face);
        int[] c = {8 + ox, 8 + oy, 8 + oz};
        c[n] = CablePart.plane(face) == 0 ? PipeTransmitterDescriptor.PORT_HEIGHT : 16 - PipeTransmitterDescriptor.PORT_HEIGHT;
        return c;
    }

    /** The pipe passing through or ending at the port, or null. */
    public PipePart pipe() {
        if (!(sixNode instanceof VoxelNode v)) return null;
        int[] p = port();
        return v.pipeAt(p[0], p[1], p[2]);
    }

    public void read() {
        PipePart pipe = pipe();
        connected = pipe != null && pipe.fluid != null;
        reading = connected ? PipeSensorPart.reading(descriptor.sensing, mods.eln.fluid.FluidWorld.nodeFor(pipe.fluid)) : 0;
        outputGateProcess.setOutputNormalized(reading / scale);
    }

    /** Share of full scale (0..1). */
    public double share() {
        return Math.max(0, Math.min(1, reading / scale));
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        return lrdu == front ? outputGate : null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return lrdu == front ? NodeBase.maskElectricalOutputGate : 0;
    }

    String readingText() {
        return String.format(descriptor.sensing == PipeSensorPart.PRESSURE ? "%.2f %s absolute" : "%.0f %s", reading, PipeSensorPart.unit(descriptor.sensing));
    }

    @Override
    public String multiMeterString() {
        return (connected ? readingText() : "No pipe") + "  " + Utils.plotVolt("Uout:", outputGate.getU());
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("Reads", PipeSensorPart.name(descriptor.sensing));
        if (connected) info.put("Reading", readingText());
        else info.put("Pipe", "not connected to a pipe");
        info.put("Full scale", String.format("%.0f %s at %.0f V", scale, PipeSensorPart.unit(descriptor.sensing), Eln.SVU));
        info.put("Output", Utils.plotVolt("", outputGate.getU()));
        return info;
    }

    /** An empty-hand right-click steps the full scale; a wrench turns it. */
    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        if (player != null && player.getMainHandItem().isEmpty()) {
            scale = PipeSensorPart.nextScale(descriptor.sensing, scale);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("%s: full scale %.0f %s at 50 V",
                PipeSensorPart.name(descriptor.sensing), scale, PipeSensorPart.unit(descriptor.sensing))));
            read();
            needPublish();
            return true;
        }
        return onBlockActivatedRotate(player);
    }

    /** The pipe sensor item of its kind (it was placed with one). */
    @Override
    public ItemStack getDropItemStack() {
        return new ItemStack(switch (descriptor.sensing) {
            case PipeSensorPart.PRESSURE -> mods.eln.registry.ElnRegistry.PIPE_PRESSURE_SENSOR.get();
            case PipeSensorPart.TEMPERATURE -> mods.eln.registry.ElnRegistry.PIPE_TEMPERATURE_SENSOR.get();
            default -> mods.eln.registry.ElnRegistry.PIPE_QUALITY_SENSOR.get();
        });
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        scale = nbt.getDoubleOr("scale", descriptor.defaultScale());
        if (!(scale > 0)) scale = descriptor.defaultScale();
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("scale", scale);
    }

    @Override
    public void initialize() {
    }
}
