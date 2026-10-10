package mods.eln.node;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeElement;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class NodeManager {
    public static NodeManager instance = null;

    private HashMap<Coordonate, NodeBase> nodesMap;
    private ArrayList<NodeBase> nodes;

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


    public void loadFromNbt(CompoundTag nbt) {
        List<NodeBase> addedNode = new ArrayList<NodeBase>();
        for (Object o : Utils.getTags(nbt)) {
            CompoundTag tag = (CompoundTag) o;
            Class nodeClass = UUIDToClass.get(tag.getStringOr("tag", ""));
            try {
                NodeBase node = (NodeBase) nodeClass.getConstructor().newInstance();
                node.readFromNBT(tag);
                addNode(node);
                addedNode.add(node);
                node.initializeFromNBT();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (NodeBase n : addedNode) {
            n.globalBoot();
        }
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

    }

    public void clear() {
        nodes.clear();
        nodesMap.clear();
    }

    public void unload(int dimensionId) {

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
