package mods.eln.node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class NodeManager extends WorldSavedData {
	public static NodeManager instance = null;

	private HashMap<Coordonate, NodeBase> nodesMap;
	private ArrayList<NodeBase> nodes;

	public HashMap<Coordonate, NodeBase> getNodeArray()
	{
		return nodesMap;
	}

	public  ArrayList<NodeBase> getNodes() {
		return nodes;
	}

	public static final HashMap<String, Class> UUIDToClass = new HashMap<String, Class>();

	public static void registerUuid(String uuid, Class classType)
	{
		UUIDToClass.put(uuid, classType);
	}

	Collection<NodeBase> getNodeList()
	{
		return nodesMap.values();
	}

	// private ArrayList<Node> nodeArray = new ArrayList<Node>();

	public NodeManager(String par1Str) {
		super(par1Str);
		nodesMap = new HashMap<Coordonate, NodeBase>();
		nodes = new ArrayList<NodeBase>();
		instance = this;

	}

	public void addNode(NodeBase node)
	{
		// nodeArray.add(node);
		if (node.coordonate == null)
		{
			Utils.println("Null coordonate addnode");
			while (true)
				;
		}
		NodeBase old = nodesMap.put(node.coordonate, node);
		if (old != null) {
			nodes.remove(old);
		}

		nodes.add(node);
		Utils.println("NodeManager has " + nodesMap.size() + "node");
		// nodeArray.put(new NodeIdentifier(node), node);
	}

	public void removeNode(NodeBase node)
	{
		if (node == null) return;
		nodesMap.remove(node.coordonate);
		nodes.remove(node);
		Utils.println("NodeManager has " + nodesMap.size() + "node");
	}

	public void removeCoordonate(Coordonate c)
	{
		// nodeArray.remove(node);
		NodeBase n = nodesMap.remove(c);
		if (n != null) nodes.remove(n);
		Utils.println("NodeManager has " + nodesMap.size() + "node");
	}

	@Override
	public boolean isDirty()
	{
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
	public void writeToNBT(NBTTagCompound nbt) {
		/*
		 * int nodeCounter = 0; for(NodeBase node : nodesmap.values()) { try { if(node.mustBeSaved() == false) continue; NBTTagCompound nbtNode = new NBTTagCompound(); nbtNode.setString("tag", node.getNodeUuid()); node.writeToNBT(nbtNode); nbt.setTag("n" + nodeCounter++, nbtNode); } catch (Exception e) { e.printStackTrace(); }
		 * 
		 * }
		 */
	}

	public NodeBase getNodeFromCoordonate(Coordonate nodeCoordonate)
	{
		int idx = 0;
		idx++;
		// for(Node node : nodeArray)
		{
			// if(nodeCoordonate.equals(node.coordonate)) return node;
		}
		return nodesMap.get(nodeCoordonate);
		// return null;
	}

	Random rand = new Random();

	public NodeBase getRandomNode() {
		if (nodes.isEmpty()) return null;
		return nodes.get(rand.nextInt(nodes.size()));
	}



	public void loadFromNbt(NBTTagCompound nbt) {
		List<NodeBase> addedNode = new ArrayList<NodeBase>();
		for (Object o : Utils.getTags(nbt))
		{
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
		int nodeCounter = 0;
		List<NodeBase> nodesCopy = new ArrayList<NodeBase>();
		nodesCopy.addAll(nodes);
		for (NodeBase node : nodesCopy)
		{
			try {
				if (node.mustBeSaved() == false) continue;
				if (dim != Integer.MIN_VALUE && node.coordonate.dimention != dim) continue;
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
			if (n.coordonate.dimention == dimensionId) {
				n.unload();
				i.remove();
				nodesMap.remove(n.coordonate);
			}
		}
	}
}
