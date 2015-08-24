package mods.eln.gridnode;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Created by svein on 22/08/15.
 */
abstract public class GridElement extends TransparentNodeElement {
    /**
     * The last place any given player tried to link two instance nodes.
     */
    private static HashMap<UUID, Pair<Coordonate, Direction>> pending = new HashMap<UUID, Pair<Coordonate, Direction>>();
    public HashSet<GridLink> gridLinkList = new HashSet<GridLink>();
    public HashSet<GridLink> gridLinksBooting = new HashSet<GridLink>();
    GridDescriptor desc;
    int connectRange;
    private float idealRenderingAngle;

    public GridElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor, int connectRange) {
        super(transparentNode, descriptor);
        this.desc = (GridDescriptor) descriptor;
        this.connectRange = connectRange;
    }

    /* Connect one GridNode to another. */
    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        // Check if user is holding an appropriate tool.
        final ItemStack stack = entityPlayer.getCurrentEquippedItem();
        final GenericItemBlockUsingDamageDescriptor itemDesc = GenericItemBlockUsingDamageDescriptor.getDescriptor(stack);
        if (itemDesc instanceof ElectricalCableDescriptor) {
            return onTryGridConnect(entityPlayer, stack, (ElectricalCableDescriptor) itemDesc, side);
        }
        // TODO: Scissors. Break the connection without breaking the pole.
        return false;
    }

    private boolean onTryGridConnect(EntityPlayer entityPlayer, ItemStack stack, ElectricalCableDescriptor cable, Direction side) {
        // First node, or second node?
        UUID uuid = entityPlayer.getPersistentID();
        Pair<Coordonate, Direction> p = pending.get(uuid);
        GridElement other = null;
        if (p != null) {
            other = GridLink.getElementFromCoordinate(p.getLeft());
        }
        // Check if it's the *correct* cable descriptor.
        if (!cable.equals(desc.cableDescriptor)) {
            Utils.addChatMessage(entityPlayer, "Wrong cable, you need " + desc.cableDescriptor.name);
            return true;
        }
        if (other == null || other == this) {
            Utils.addChatMessage(entityPlayer, "Setting starting point");
            pending.put(uuid, Pair.of(this.coordonate(), side));
        } else {
            final double distance = other.coordonate().trueDistanceTo(this.coordonate());
            final int cableLength = (int) Math.ceil(distance);
            final int range = Math.min(connectRange, other.connectRange);
            if (stack.stackSize < distance) {
                Utils.addChatMessage(entityPlayer, "You need " + cableLength + " units of cable");
            } else if (distance > range) {
                Utils.addChatMessage(entityPlayer, "Cannot connect, range " + Math.ceil(distance) + " and limit " + range + " blocks");
            } else if (!this.canConnect(other)) {
                Utils.addChatMessage(entityPlayer, "Cannot connect these two objects");
            } else if (!this.validLOS(other)) {
                Utils.addChatMessage(entityPlayer, "Cannot connect, no line of sight");
            } else {
                if (GridLink.addLink(this, other, side, p.getRight(), cable, cableLength)) {
                    Utils.addChatMessage(entityPlayer, "Added connection");
                    stack.splitStack(cableLength);
                } else {
                    Utils.addChatMessage(entityPlayer, "Already connected");
                }
            }
            pending.remove(uuid);
        }
        return true;
    }

    @Override
    public void initialize() {
        connect();
        for (GridLink link : gridLinksBooting) {
            link.connect();
        }
        gridLinksBooting.clear();
        updateIdealRenderAngle();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        for (GridLink link : gridLinkList) {
            link.connect();
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        for (GridLink link : gridLinkList) {
            link.disconnect();
        }
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        HashSet<GridLink> copy = new HashSet<GridLink>(gridLinkList);
        for (GridLink link : copy) {
            node.dropItem(link.onBreakElement());
        }
    }

    @Override
    public void selfDestroy() {
        super.selfDestroy();
        HashSet<GridLink> copy = new HashSet<GridLink>(gridLinkList);
        for (GridLink link : copy) {
            link.selfDestroy();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        Integer i = 0;
        NBTTagCompound gridLinks = Utils.newNbtTagCompund(nbt, "gridLinks");
        for (GridLink link : gridLinkList) {
            link.writeToNBT(Utils.newNbtTagCompund(gridLinks, i.toString()), "");
            i++;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        assert gridLinkList.isEmpty();
        final NBTTagCompound gridLinks = nbt.getCompoundTag("gridLinks");
        for (Integer i = 0; ; i++) {
            final NBTTagCompound linkTag = gridLinks.getCompoundTag(i.toString());
            if (linkTag.hasNoTags())
                break;
            gridLinksBooting.add(new GridLink(linkTag, ""));
        }
    }

    abstract protected ElectricalLoad getGridElectricalLoad(Direction side);

    // TODO: This should check if the wire isn't passing through blocks.
    private boolean validLOS(GridElement other) {
        return true;
    }

    // Return false if connecting grid elements that can't connect.
    protected boolean canConnect(GridElement other) {
        return true;
    }

    // TODO: One pole turns, all connected cables should be recalculated,
    // not just the ones being rendered here.
    /* Compute a rendering angle that minimizes any straight-on cables. */
    public void updateIdealRenderAngle() {
        if (desc.rotationIsFixed()) {
            switch (front) {
                case XN:
                    idealRenderingAngle = 0;
                    break;
                case XP:
                    idealRenderingAngle = 180;
                    break;
                case YN:
                    idealRenderingAngle = 90;
                    break;
                case YP:
                    idealRenderingAngle = 90;
                    break;
                case ZN:
                    idealRenderingAngle = 270;
                    break;
                case ZP:
                    idealRenderingAngle = 90;
                    break;
            }
            //System.out.println(idealRenderingAngle);
        } else if (gridLinkList.size() == 0) {
            idealRenderingAngle = 0;
        } else {
            // Compute angles.
            double angles[] = new double[gridLinkList.size()];
            int i = 0;
            for (GridLink link : gridLinkList) {
                Coordonate vec = link.a.subtract(link.b);
                // Angles 180 degrees apart are equivalent.
                if (vec.z < 0)
                    vec = vec.negate();
                double h = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
                angles[i++] = Math.acos(vec.x / h);
            }
            // This could probably be optimised with a bit of math, but w.e.
            double optAngle = 0;
            double optErr = Double.POSITIVE_INFINITY;
            for (i = 0; i < 128; i++) {
                // Check a half-circle.
                double angle = Math.PI * i / 128.0;
                double error = 0;
                for (double a : angles) {
                    double err = Math.abs(Math.sin(angle - a));
                    error += err * err * err;
                }
                if (error < optErr) {
                    optAngle = angle;
                    optErr = error;
                }
            }
            idealRenderingAngle = (float) Math.toDegrees(-optAngle);
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat(idealRenderingAngle);
            // Each wire pair should be drawn by exactly one node.
            // Check for which ones it's this one.
            ArrayList<GridLink> ourLinks = new ArrayList<GridLink>();
            for (GridLink link : gridLinkList) {
                if (link.a.equals(coordonate())/* && link.connected*/) {
                    ourLinks.add(link);
                }
            }
            // The renderer needs to know, for each catenary:
            // - Vec3 of the starting point.
            // - Vec3 of the end point.
            // There's a finite number of starting points, and a potentially unlimited number of endpoints...
            // But until we get protocol buffers or something, simple remains good.
            // So we'll just send pairs, even if there's some duplication.
            stream.writeInt(ourLinks.size());
            for (GridLink link : ourLinks) {
                GridElement target = link.getOtherElement(this);
                Direction ourSide = link.getSide(this);
                Direction theirSide = link.getSide(target);
                // It's always the "a" side doing this.
                Coordonate offset = link.b.subtract(link.a);
                for (int i = 0; i < 2; i++) {
                    final Vec3 start = getCablePoint(ourSide, i);
                    start.rotateAroundY((float) Math.toRadians(idealRenderingAngle));
                    Vec3 end = target.getCablePoint(theirSide, i);
                    end.rotateAroundY((float) Math.toRadians(target.idealRenderingAngle));
                    end = end.addVector(offset.x, offset.y, offset.z);
                    writeVec(stream, start);
                    writeVec(stream, end);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Vec3 getCablePoint(Direction side, int i) {
        if (i >= 2) throw new AssertionError("Invalid cable point index");
        Obj3D.Obj3DPart part = (i == 0 ? desc.plus : desc.gnd).get(0);
        BoundingBox bb = part.boundingBox();
        return bb.centre();
    }

    private void writeVec(DataOutputStream stream, Vec3 sp) throws IOException {
        stream.writeFloat((float) sp.xCoord);
        stream.writeFloat((float) sp.yCoord);
        stream.writeFloat((float) sp.zCoord);
    }

    @Override
    public String multiMeterString(Direction side) {
        ElectricalLoad electricalLoad = getGridElectricalLoad(side);
        return Utils.plotUIP(electricalLoad.getU(), electricalLoad.getI());
    }

    @Override
    public String thermoMeterString(Direction side) {
        ThermalLoad thermalLoad = getThermalLoad(side, LRDU.Up);
        return Utils.plotCelsius("T", thermalLoad.Tc);
    }
}
