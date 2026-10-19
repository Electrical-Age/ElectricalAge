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
        for (List<Orphan> l : orphans.values()) l.removeIf(o -> node.coordonate.equals(o.at()));
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
            CompoundTag tag = (CompoundTag) o;
            String type = tag.getStringOr("tag", "");
            Class nodeClass = UUIDToClass.get(type);
            if (nodeClass == null) {
                keepOrphan(dim, tag, "no node type '" + type + "' in this version", null);
                continue;
            }
            NodeBase node;
            try {
                node = (NodeBase) nodeClass.getConstructor().newInstance();
                node.readFromNBT(tag);
            } catch (Exception e) {
                keepOrphan(dim, tag, "loading a node of type '" + type + "' failed", e);
                continue;
            }
            addNode(node);
            addedNode.add(node);
            try {
                node.initializeFromNBT();
            } catch (Exception e) {
                LOG.error("Electrical Age: initializing node {} at {}", type, node.coordonate, e);
            }
        }

        for (NodeBase n : addedNode) {
            n.globalBoot();
        }
    }

    private void keepOrphan(int dim, CompoundTag tag, String why, Exception e) {
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
            try {
                if (node.mustBeSaved() == false) continue;
                if (dim != Integer.MIN_VALUE && node.coordonate.dimention != dim) continue;
                CompoundTag nbtNode = new CompoundTag();
                nbtNode.putString("tag", node.getNodeUuid());
                node.writeToNBT(nbtNode);
                nbt.put("n" + nodeCounter++, nbtNode);
            } catch (Exception e) {
                e.printStackTrace();
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
