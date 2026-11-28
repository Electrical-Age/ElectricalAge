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

    /** The port of one on face side with its centre offset (ox, oy, oz) (block-local voxels): PORT_HEIGHT off the face,
     *  over the device's centre. */
    public static int[] port(mods.eln.misc.Direction side, int ox, int oy, int oz) {
        int face = side.toEnumFacing().get3DDataValue(), n = mods.eln.node.voxel.CablePart.normalAxis(face);
        int[] c = {8 + ox, 8 + oy, 8 + oz};
        c[n] = mods.eln.node.voxel.CablePart.plane(face) == 0 ? PORT_HEIGHT : 16 - PORT_HEIGHT;
        return c;
    }

    /**
     * Where the crosshair means the device rather than the pipe at its port (VoxelPick; block voxels): its body grown
     * by a voxel, and the last 3 voxels of pipe round the port, from the face to past the pipe's outline. From its
     * place alone, so the client works it out as the server does.
     */
    public static java.util.List<int[]> aimBoxes(mods.eln.misc.Direction side, int ox, int oy, int oz, int[] body) {
        int face = side.toEnumFacing().get3DDataValue(), n = mods.eln.node.voxel.CablePart.normalAxis(face);
        int[] p = port(side, ox, oy, oz), b = new int[6];
        for (int a = 0; a < 3; a++) {
            b[a] = p[a] - 3;
            b[a + 3] = p[a] + 3;
        }
        int reach = PORT_HEIGHT + 4;
        b[n] = mods.eln.node.voxel.CablePart.plane(face) == 0 ? 0 : 16 - reach;
        b[n + 3] = mods.eln.node.voxel.CablePart.plane(face) == 0 ? reach : 16;
        return java.util.List.of(new int[]{body[0] - 1, body[1] - 1, body[2] - 1, body[3] + 1, body[4] + 1, body[5] + 1}, b);
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
