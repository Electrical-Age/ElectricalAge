package mods.eln.gridnode;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
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
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        final ItemStack stack = entityPlayer.getMainHandItem();
        // overhead cable only (Baughn 2026-11-09: air insulates a line, so spans take bare copper, no rubber)
        if (OverheadCableItem.is(stack)) return onTryGridConnect(entityPlayer, stack, side);
        if (mods.eln.node.voxel.CableItems.descriptor(stack) != null) {
            // (on the route note's line: the kit's preview owns the action bar while it is held)
            if (entityPlayer instanceof net.minecraft.server.level.ServerPlayer sp)
                mods.eln.node.voxel.KitPreview.note(sp, "Lines between poles take Overhead Cable, made on a wire drawing machine, not cable kits");
            return true;
        }
        // TODO: Scissors. Break the connection without breaking the pole.
        return false;
    }

    private boolean onTryGridConnect(Player entityPlayer, ItemStack stack, Direction side) {
        // First node, or second node?
        UUID uuid = entityPlayer.getUUID();
        Pair<Coordonate, Direction> p = pending.get(uuid);
        GridElement other = null;
        // a starting pole in another dimension is forgotten: a span can't cross dimensions (1.12 measured x, y and z
        // only and linked them for a few blocks of cable)
        if (p != null && p.getLeft().dimention == this.coordonate().dimention) {
            other = GridLink.getElementFromCoordinate(p.getLeft());
        }
        if (other == null || other == this) {
            entityPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Start point set")));
            pending.put(uuid, Pair.of(this.coordonate(), side));
        } else {
            final double distance = other.coordonate().trueDistanceTo(this.coordonate());
            final int cableLength = (int) Math.ceil(distance);
            final int range = Math.min(connectRange, other.connectRange);
            if (OverheadCableItem.blocks(stack) < cableLength) {
                entityPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Need %1$ blocks of cable", cableLength)));
            } else if (distance > range) {
                entityPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Too far: %1$ blocks, a span reaches %2$. Add a pole between.", cableLength, range)));
            } else if (!this.canConnect(other)) {
                entityPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Can't connect these poles")));
            } else if (!this.lineOfSight(other, side, p.getRight())) {
                entityPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("No line of sight")));
            } else {
                ItemStack piece = OverheadCableItem.make(OverheadCableItem.mm2(stack), cableLength);
                if (GridLink.addLink(this, other, side, p.getRight(), piece, OverheadCableItem.rsPerBlock(stack) * cableLength)) {
                    entityPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Connected")));
                    onLinksChanged();
                    other.onLinksChanged();
                    OverheadCableItem.take(stack, cableLength);
                } else {
                    entityPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(mods.eln.i18n.I18N.tr("Already connected")));
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
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);

        Integer i = 0;
        CompoundTag gridLinks = Utils.newNbtTagCompund(nbt, "gridLinks");
        for (GridLink link : gridLinkList) {
            link.writeToNBT(Utils.newNbtTagCompund(gridLinks, i.toString()), "");
            i++;
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);

        assert gridLinkList.isEmpty();
        final CompoundTag gridLinks = nbt.getCompoundOrEmpty("gridLinks");
        for (Integer i = 0; ; i++) {
            final CompoundTag linkTag = gridLinks.getCompoundOrEmpty(i.toString());
            if (linkTag.isEmpty())
                break;
            gridLinksBooting.add(new GridLink(linkTag, ""));
        }
    }

    /** Forgets a player's starting pole (logout). */
    public static void forget(UUID player) {
        pending.remove(player);
    }

    abstract protected ElectricalLoad getGridElectricalLoad(Direction side);

    /** A span was added or removed. */
    protected void onLinksChanged() {
    }

    // TODO: This should check if the wire isn't passing through blocks.
    /**
     * Can a span be strung to other: does the straight line between each pair of cable points cross no block that
     * collides, other than the two poles' own blocks and ghosts? 1.12 left this a stub that always said yes, so a span
     * went straight through a stone wall (AdventurAgent 1573709864033845248). The sag isn't checked: the cable hangs
     * below the line. Blocks in unloaded chunks count as clear. The cable points use each pole's current render angle; a
     * free-turning pole re-aims after the link is added, so near a corner the drawn span may differ a little from the
     * checked line (tolerated). A sampled walk rather than Level.clip, because the poles' own blocks and ghosts must be
     * passed through.
     */
    public boolean lineOfSight(GridElement other, Direction mySide, Direction otherSide) {
        net.minecraft.world.level.Level level = coordonate().world();
        if (level == null) return true;
        for (int i = 0; i < 2; i++) {
            Vec3 a = centre(this).add(getCablePoint(mySide, i).yRot((float) Math.toRadians(idealRenderingAngle)));
            Vec3 b = centre(other).add(other.getCablePoint(otherSide, i).yRot((float) Math.toRadians(other.idealRenderingAngle)));
            int n = (int) Math.ceil(a.distanceTo(b) / 0.125);
            for (int k = 1; k < n; k++) {
                Vec3 q = a.lerp(b, k / (double) n);
                net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(q);
                if (!level.isLoaded(bp) || ownBlock(other, bp, level)) continue;
                var state = level.getBlockState(bp);
                if (state.isAir()) continue;
                for (var box : state.getCollisionShape(level, bp).toAabbs())
                    if (box.move(bp).contains(q)) return false;
            }
        }
        return true;
    }

    static Vec3 centre(GridElement e) {
        Coordonate c = e.coordonate();
        return new Vec3(c.x + 0.5, c.y + 0.5, c.z + 0.5);
    }

    /** Is bp one of the two poles, or a ghost block of either? */
    private boolean ownBlock(GridElement other, net.minecraft.core.BlockPos bp, net.minecraft.world.level.Level level) {
        Coordonate c = new Coordonate(bp, level);
        if (c.equals(coordonate()) || c.equals(other.coordonate())) return true;
        var g = mods.eln.Eln.ghostManager.getGhost(c);
        return g != null && (g.getObservatorCoordonate().equals(coordonate()) || g.getObservatorCoordonate().equals(other.coordonate()));
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
                    // 1.12: Vec3 is immutable (1.7.10 rotateAroundY rotated in place; rotateYaw is the same rotation)
                    final Vec3 start = getCablePoint(ourSide, i).yRot((float) Math.toRadians(idealRenderingAngle));
                    Vec3 end = target.getCablePoint(theirSide, i).yRot((float) Math.toRadians(target.idealRenderingAngle));
                    end = end.add(offset.x, offset.y, offset.z);
                    writeVec(stream, start);
                    writeVec(stream, end);
                }
                // the stasis halo: either pole anchored (Baughn 2026-11-08)
                stream.writeBoolean(timeExempt() || target.timeExempt());
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
        stream.writeFloat((float) sp.x);
        stream.writeFloat((float) sp.y);
        stream.writeFloat((float) sp.z);
    }

    @Override
    public java.util.Map<String, String> getWaila() {
        var info = mods.eln.compat.jade.ProbeLines.of();
        ElectricalLoad load = getGridElectricalLoad(this.front);
        if (load != null) {
            info.headline(mods.eln.i18n.I18N.tr("Voltage"), Utils.plotVolt("", load.getU()));
            info.headline(mods.eln.i18n.I18N.tr("Current"), Utils.plotAmpere("", load.getI()));
            info.detail(mods.eln.i18n.I18N.tr("Power"), Utils.plotPower("", load.getU() * load.getI()));
        }
        return info;
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
