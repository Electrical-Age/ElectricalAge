package mods.eln.sixnode.pipetransmitter;

import mods.eln.Eln;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.voxel.PipeSensorPart;

/**
 * A pipe sensor as a device (Baughn 2026-11-02: the sensor fitted on a pipe ended up in mid-air, where signal cables can't
 * go): a small terminal placed anywhere on a floor, wall or ceiling of a voxel block, with a signal pin on its front
 * and a pipe port 4 voxels above its centre (PORT_HEIGHT, on the pipe routers' lattice). The player lays pipe from the
 * port to the pipe to measure; it reads the run of whatever pipe passes through or ends at the port. Placed by the
 * pipe sensor items (PipeSensorItem); one descriptor per kind (PipeSensorPart.PRESSURE, TEMPERATURE, QUALITY). It wears
 * the Voltage Probe's model, with a short iron stem up to the port drawn by its render.
 */
public class PipeTransmitterDescriptor extends SixNodeDescriptor {
    /** The port's height off the face, voxels: the pipe routers' lattice (PipeRouter.LATTICE) and clear of the body (2 high). */
    public static final int PORT_HEIGHT = 4;
    /** Their ids: FIRST_ID + the kind (PipeSensorPart.PRESSURE, TEMPERATURE, QUALITY), beside the probes (sub-ids 2-4 of 100). */
    public static final int FIRST_ID = 2 + (100 << 6);

    public final int sensing;
    Obj3DPart main;

    public PipeTransmitterDescriptor(String name, int sensing) {
        super(name, PipeTransmitterElement.class, null);
        this.sensing = sensing;
        main = Eln.obj.getPart("voltagesensor", "main");
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw() {
        if (main != null) main.draw();
    }

    /** The full scale a new one starts at. */
    public double defaultScale() {
        return PipeSensorPart.defaultScale(sensing);
    }

    /** Off centre in a voxel block, at its drawn size; on any face (Baughn: floor, wall or ceiling). */
    @Override
    public boolean placeAnywhere() {
        return true;
    }

    /** Turned as the Voltage Probe is (on a wall its signal pin points down, to the floor). */
    @Override
    public mods.eln.misc.LRDU getFrontFromPlace(mods.eln.misc.Direction side, net.minecraft.world.entity.player.Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }

    @Override
    public void drawPlaced(mods.eln.misc.Direction side, mods.eln.misc.LRDU front) {
        front.glRotateOnX();
        draw();
        PipeTransmitterRender.drawStem();
        PipeTransmitterRender.drawMark(sensing);
    }

    @Override
    protected float[] bodyBounds() {
        return boundsOf(main);
    }
}
