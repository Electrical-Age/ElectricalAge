package mods.eln.node;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeElement;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NodeManager {
    public static NodeManager instance = null;

    private HashMap<Coordonate, NodeBase> nodesMap;
    private ArrayList<NodeBase> nodes;

    private static final Logger LOG = LoggerFactory.getLogger("eln");

    /**
     * Saved nodes this version could not load (no class for the type, or its load threw), per dimension: raw copies
     * of their tags, written back out unchanged by saveToNbt so that a missing or broken device doesn't delete the
     * player's machine for good. A node placed later at the same position replaces the orphan.
     */
    private final HashMap<Integer, List<Orphan>> orphans = new HashMap<>();

    record Orphan(Coordonate at, CompoundTag tag) {
    }

    /** The orphaned node tags of a dimension (copies). */
    public List<CompoundTag> getOrphans(int dim) {
        List<CompoundTag> r = new ArrayList<>();
        for (Orphan o : orphans.getOrDefault(dim, List.of())) r.add(o.tag().copy());
        return r;
    }

    /** Is a node this version could not load kept at c? */
    public boolean hasOrphanAt(Coordonate c) {
        for (List<Orphan> l : orphans.values()) for (Orphan o : l) if (c.equals(o.at())) return true;
        return false;
    }

    /**
     * Forgets the orphans at c, each with a WARN naming why: its block went for real (not a chunk unload), or a new
     * node took the place. Returns how many went.
     */
    public int dropOrphans(Coordonate c, String why) {
        int n = 0;
        for (List<Orphan> l : orphans.values()) {
            for (Iterator<Orphan> i = l.iterator(); i.hasNext(); ) {
                Orphan o = i.next();
                if (!c.equals(o.at())) continue;
                i.remove();
                n++;
                LOG.warn("Electrical Age: {}; discarding the saved node of type '{}' at {} that this version could not load",
                    why, o.tag().getStringOr("tag", ""), c);
            }
        }
        return n;
    }

    public HashMap<Coordonate, NodeBase> getNodeArray() {
        return nodesMap;
    }

    public ArrayList<NodeBase> getNodes() {
        return nodes;
    }

    public static final HashMap<String, Class> UUIDToClass = new HashMap<String, Class>();

    public static void registerUuid(String uuid, Class classType) {
        UUIDToClass.put(uuid, classType);
    }

    /**
     * Rewrites of saved node tags from older versions, by the saved type ("tag"), applied before the node is built:
     * a device that changed node kind keeps its saves (the simple-node FE exporter, now a transparent node). An
     * upgrade returns the tag unchanged when it can't convert it, and the orphan path keeps it.
     */
    public static final HashMap<String, java.util.function.UnaryOperator<CompoundTag>> TAG_UPGRADES = new HashMap<>();

    public static void registerTagUpgrade(String type, java.util.function.UnaryOperator<CompoundTag> upgrade) {
        TAG_UPGRADES.put(type, upgrade);
    }

    /** The tag as this version reads it (TAG_UPGRADES applied). */
    public static CompoundTag upgradeTag(CompoundTag tag) {
        var u = TAG_UPGRADES.get(tag.getStringOr("tag", ""));
        if (u == null) return tag;
        try {
            return u.apply(tag);
        } catch (RuntimeException e) {
            LOG.warn("Electrical Age: upgrading a saved node of type '{}' failed; keeping it as it was", tag.getStringOr("tag", ""), e);
            return tag;
        }
    }

    Collection<NodeBase> getNodeList() {
        return nodesMap.values();
    }

    // private ArrayList<Node> nodeArray = new ArrayList<Node>();

    public NodeManager(String par1Str) {
        nodesMap = new HashMap<Coordonate, NodeBase>();
        nodes = new ArrayList<NodeBase>();
        instance = this;

    }

    public void addNode(NodeBase node) {
        // nodeArray.add(node);
        if (node.coordonate == null) {
            // 1.12 port, known bug: this used to hang the server thread forever (while (true);).
            Utils.println("Null coordonate addnode: node not added");
            new Throwable("Null coordonate addnode").printStackTrace();
            return;
        }
        dropOrphans(node.coordonate, "a new node was placed there");
        NodeBase old = nodesMap.put(node.coordonate, node);
        if (old != null) {
            nodes.remove(old);
        }

        nodes.add(node);
        Utils.println("NodeManager has " + nodesMap.size() + "node");
        // nodeArray.put(new NodeIdentifier(node), node);
    }

    public void removeNode(NodeBase node) {
        if (node == null) return;
        if (node instanceof Node && node.coordonate.world() != null) NodeBlock.setLight(node.coordonate.world(), node.coordonate.pos(), 0);
        nodesMap.remove(node.coordonate);
        nodes.remove(node);
        Utils.println("NodeManager has " + nodesMap.size() + "node");
    }

    public void removeCoordonate(Coordonate c) {
        // nodeArray.remove(node);
        NodeBase n = nodesMap.remove(c);
        if (n != null) nodes.remove(n);
        Utils.println("NodeManager has " + nodesMap.size() + "node");
    }




    public NodeBase getNodeFromCoordonate(Coordonate nodeCoordonate) {
        int idx = 0;
        idx++;
        // for(Node node : nodeArray)
        {
            // if(nodeCoordonate.equals(node.coordonate)) return node;
        }
        return nodesMap.get(nodeCoordonate);
        // return null;
    }

    public TransparentNodeElement getTransparentNodeFromCoordinate(Coordonate coord) {
        NodeBase base = getNodeFromCoordonate(coord);
        if (base instanceof TransparentNode) {
            TransparentNode n = (TransparentNode) base;
            return n.element;
        }
        return null;
    }

    Random rand = new Random();

    public NodeBase getRandomNode() {
        if (nodes.isEmpty()) return null;
        return nodes.get(rand.nextInt(nodes.size()));
    }


    /** Loads a node file without a dimension (tests): orphans are kept under Integer.MIN_VALUE. */
    public void loadFromNbt(CompoundTag nbt) {
        loadFromNbt(nbt, Integer.MIN_VALUE);
    }

    /** Loads the nodes of dimension dim's node file; nodes that can't be loaded are kept as orphans of dim. */
    public void loadFromNbt(CompoundTag nbt, int dim) {
        List<NodeBase> addedNode = new ArrayList<NodeBase>();
        for (Object o : Utils.getTags(nbt)) {
            CompoundTag tag = upgradeTag((CompoundTag) o);
            String type = tag.getStringOr("tag", "");
            Class nodeClass = UUIDToClass.get(type);
            if (nodeClass == null) {
                keepOrphan(dim, tag, "no node type '" + type + "' in this version", null);
                continue;
            }
            NodeBase node;
            try {
                CompoundTag loadedFrom = tag.copy();
                node = (NodeBase) nodeClass.getConstructor().newInstance();
                node.readFromNBT(tag);
                node.lastSaved = loadedFrom;
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {   // (LinkageError too: one node's missing class must not stop the rest of the file)
                keepOrphan(dim, tag, "loading a node of type '" + type + "' failed", e);
                continue;
            }
            addNode(node);
            addedNode.add(node);
            try {
                node.initializeFromNBT();
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {
                LOG.error("Electrical Age: initializing node {} at {}", type, node.coordonate, e);
            }
        }

        for (NodeBase n : addedNode) {
            try {
                n.globalBoot();
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {   // (one node must not stop the others booting)
                LOG.error("Electrical Age: booting node {} at {}", n.getClass().getSimpleName(), n.coordonate, e);
            }
        }
    }

    private void keepOrphan(int dim, CompoundTag tag, String why, Throwable e) {
        Coordonate at = null;
        CompoundTag pos = tag.getCompoundOrEmpty("node");
        if (pos.contains("cx")) {
            at = new Coordonate();
            at.readFromNBT(pos, "c");
        }
        String where = at == null ? "unknown position" : at.x + " " + at.y + " " + at.z + " (dim " + at.dimention + ")";
        if (e == null) LOG.warn("Electrical Age: {} at {}; keeping its saved data unchanged", why, where);
        else LOG.warn("Electrical Age: {} at {}; keeping its saved data unchanged", why, where, e);
        orphans.computeIfAbsent(dim, k -> new ArrayList<>()).add(new Orphan(at, tag.copy()));
    }

    public void saveToNbt(CompoundTag nbt, int dim) {
        int nodeCounter = 0;
        List<NodeBase> nodesCopy = new ArrayList<NodeBase>();
        nodesCopy.addAll(nodes);
        for (NodeBase node : nodesCopy) {
            if (!node.mustBeSaved()) continue;
            if (dim != Integer.MIN_VALUE && (node.coordonate == null || node.coordonate.dimention != dim)) continue;
            try {
                CompoundTag nbtNode = new CompoundTag();
                nbtNode.putString("tag", node.getNodeUuid());
                node.writeToNBT(nbtNode);
                node.lastSaved = nbtNode;
                nbt.put("n" + nodeCounter++, nbtNode);
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {
                // a node left out of the file is deleted for good at the next load: write its last good data instead
                if (node.lastSaved != null) {
                    LOG.warn("Electrical Age: saving the node {} at {} failed; writing its last good saved data instead",
                        node.getClass().getSimpleName(), node.coordonate, e);
                    nbt.put("n" + nodeCounter++, node.lastSaved.copy());
                } else {
                    LOG.error("Electrical Age: saving the node {} at {} failed and it was never saved before; it is missing from this save",
                        node.getClass().getSimpleName(), node.coordonate, e);
                }
            }
        }
        for (Map.Entry<Integer, List<Orphan>> e : orphans.entrySet()) {
            if (dim != Integer.MIN_VALUE && e.getKey() != dim) continue;
            for (Orphan o : e.getValue()) nbt.put("n" + nodeCounter++, o.tag().copy());
        }
    }

    public void clear() {
        nodes.clear();
        nodesMap.clear();
        orphans.clear();
    }

    public void unload(int dimensionId) {
        orphans.remove(dimensionId);

        Iterator<NodeBase> i = nodes.iterator();
        while (i.hasNext()) {
            NodeBase n = i.next();
            if (n.coordonate.dimention == dimensionId) {
                n.unload();
                i.remove();
                nodesMap.remove(n.coordonate);
            }
        }
    }
}
