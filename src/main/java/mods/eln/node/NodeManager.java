package mods.eln.node;

import mods.eln.misc.Coordinate;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeElement;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

import java.util.*;

public class NodeManager extends WorldSavedData {
    public static NodeManager instance = null;

    private HashMap<Coordinate, NodeBase> nodesMap;
    private ArrayList<NodeBase> nodes;

    public HashMap<Coordinate, NodeBase> getNodeArray() {
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
        super(par1Str);
        nodesMap = new HashMap<Coordinate, NodeBase>();
        nodes = new ArrayList<NodeBase>();
        instance = this;

    }

    public void addNode(NodeBase node) {
        // nodeArray.add(node);
        if (node.coordinate == null) {
            Utils.println("Null coordinate addnode");
            while (true)
                ;
        }
        NodeBase old = nodesMap.put(node.coordinate, node);
        if (old != null) {
            nodes.remove(old);
        }

        nodes.add(node);
        Utils.println("NodeManager has " + nodesMap.size() + "node");
        // nodeArray.put(new NodeIdentifier(node), node);
    }

    public void removeNode(NodeBase node) {
        if (node == null) return;
        nodesMap.remove(node.coordinate);
        nodes.remove(node);
        Utils.println("NodeManager has " + nodesMap.size() + "node");
    }

    public void removeCoordonate(Coordinate c) {
        // nodeArray.remove(node);
        NodeBase n = nodesMap.remove(c);
        if (n != null) nodes.remove(n);
        Utils.println("NodeManager has " + nodesMap.size() + "node");
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int i = 0;
        i++;
        /*
		 * for(Object o : Utils.getTags(nbt)) { NBTTagCompound tag = (NBTTagCompound) o; Class nodeClass = UUIDToClass.get(tag.getString("tag")); try { NodeBase node = (NodeBase) nodeClass.getConstructor().newInstance(); node.readFromNBT(tag); addNode(node); node.initializeFromNBT();
		 * 
		 * } catch (Exception e) { e.printStackTrace(); }
		 * 
		 * }
		 * 
		 * 
		 * 
		 * for(NodeBase node : nodes){ node.globalBoot(); }
		 */
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		int nodeCounter = 0; for(NodeBase node : nodesMap.values()) { try { if(node.mustBeSaved() == false) continue; NBTTagCompound nbtNode = new NBTTagCompound(); nbtNode.setString("tag", node.getNodeUuid()); node.writeToNBT(nbtNode); nbt.setTag("n" + nodeCounter++, nbtNode); } catch (Exception e) { e.printStackTrace(); }
    }

    public Coordinate getNodeFromCoordinate(Coordinate coord) {
        int idx = 0;
        idx++;
        // for(Node node : nodeArray)
        {
            // if(nodeCoordinate.equals(node.coordinate)) return node;
        }
        return nodesMap.get(coord);
        // return null;
    }

    public TransparentNodeElement getTransparentNodeFromCoordinate(Coordonate coord) {
        NodeBase base = getNodeFromCoordonate(coord);
        if (base instanceof TransparentNode) {
            TransparentNode n = (TransparentNode) base;
            return n.element.getItemStackNBT();
        }
        return null;
    }

    Random rand = new Random();

    public Coordinate getRandomNode() {
        if (nodes.isEmpty()) return null;
        return nodes.get(rand.nextInt(nodes.size()));
    }


    public void loadFromNbt(NBTTagCompound nbt) {
        List<NodeBase> addedNode = new ArrayList<NodeBase>();
        for (Object o : Utils.getTags(nbt)) {
            NBTTagCompound tag = (NBTTagCompound) o;
            Class nodeClass = UUIDToClass.get(tag.getString("tag"));
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

    public void saveToNbt(NBTTagCompound nbt, int dim) {
        List<NodeBase> nodesCopy = new ArrayList<NodeBase>();
        nodesCopy.addAll(nodes);
        for (NodeBase node : nodesCopy) {
            try {
                if (node.mustBeSaved() == false) continue;
                if (dim != Integer.MIN_VALUE && node.coordinate.getDimension() != dim) continue;
                NBTTagCompound nbtNode = new NBTTagCompound();
                nbtNode.setString("tag", node.getNodeUuid());
                node.writeToNBT(nbtNode);
                nbt.setTag("n" + nodeCounter++, nbtNode);
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
            if (n.coordinate.getDimension() == dimensionId) {
                n.unload();
                i.remove();
                nodesMap.remove(n.coordinate);
            }
        }
    }
}
