package mods.eln.selftest;

import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Save/load round trip of one node, the way a world load does it (NodeManager.loadFromNbt): save the node, unload and
 * forget it, build a fresh node of the same class from that tag, then save the fresh one. The two tags, and the
 * elements' client packets (networkSerialize), should be equal. Port step T3 (notes/port26/PLAN.md).
 */
public final class Reload {
    public final NBTTagCompound before, after;
    public final byte[] publishBefore, publishAfter;
    public final NodeBase fresh;

    private Reload(NBTTagCompound before, NBTTagCompound after, byte[] pb, byte[] pa, NodeBase fresh) {
        this.before = before;
        this.after = after;
        this.publishBefore = pb;
        this.publishAfter = pa;
        this.fresh = fresh;
    }

    public static Reload node(WorldServer w, BlockPos p) throws ReflectiveOperationException {
        NodeBase old = NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.getX(), p.getY(), p.getZ(), w));
        if (old == null) throw new IllegalStateException("no node at " + p);
        NBTTagCompound before = save(old);
        byte[] pb = publish(old);
        old.unload();
        NodeManager.instance.removeNode(old);

        NodeBase fresh = old.getClass().getConstructor().newInstance();
        fresh.readFromNBT(before.copy());
        NodeManager.instance.addNode(fresh);
        fresh.initializeFromNBT();
        fresh.globalBoot();
        TileEntity te = w.getTileEntity(p);
        if (te instanceof NodeBlockEntity) ((NodeBlockEntity) te).forgetNode();
        return new Reload(before, save(fresh), pb, publish(fresh), fresh);
    }

    static NBTTagCompound save(NodeBase n) {
        NBTTagCompound t = new NBTTagCompound();
        t.setString("tag", n.getNodeUuid());
        n.writeToNBT(t);
        return t;
    }

    /** The elements' client state as the server would send it. */
    static byte[] publish(NodeBase n) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        if (n instanceof SixNode) {
            for (SixNodeElement e : ((SixNode) n).sideElementList) if (e != null) e.networkSerialize(out);
        } else if (n instanceof TransparentNode) {
            ((TransparentNode) n).element.networkSerialize(out);
        }
        try {
            out.flush();
        } catch (IOException ignored) {
        }
        return bos.toByteArray();
    }

    public boolean tagsEqual() {
        return before.equals(after);
    }

    public boolean publishEqual() {
        return Arrays.equals(publishBefore, publishAfter);
    }

    /** Paths of the keys that differ between the two tags (at most 8). */
    public List<String> differences() {
        List<String> out = new ArrayList<>();
        diff("", before, after, out);
        return out.size() > 8 ? out.subList(0, 8) : out;
    }

    private static void diff(String path, NBTTagCompound a, NBTTagCompound b, List<String> out) {
        TreeSet<String> keys = new TreeSet<>(a.getKeySet());
        keys.addAll(b.getKeySet());
        for (String k : keys) {
            NBTBase x = a.getTag(k), y = b.getTag(k);
            if (x == null || y == null) {
                out.add(path + k + (x == null ? " (only after)" : " (only before)"));
            } else if (x instanceof NBTTagCompound && y instanceof NBTTagCompound) {
                diff(path + k + "/", (NBTTagCompound) x, (NBTTagCompound) y, out);
            } else if (!x.equals(y)) {
                out.add(path + k + ": " + x + " -> " + y);
            }
        }
    }
}
