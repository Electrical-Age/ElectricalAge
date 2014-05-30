package mods.eln.node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Random;



import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class NodeManager extends WorldSavedData{
	public static NodeManager instance = null;
	
	HashMap<Coordonate, NodeBase> nodesmap;
	ArrayList<NodeBase> nodes;
	
	public HashMap<Coordonate, NodeBase> getNodeArray()
	{
		return nodesmap;
	}
	public ArrayList<NodeBase> getNodes(){
		return nodes;
	}
	public static HashMap<String, Class> UUIDToClass = new HashMap<String, Class>();
	public static void registerUuid(String uuid,Class classType)
	{
		UUIDToClass.put(uuid, classType);
	}
	
	Collection<NodeBase> getNodeList()
	{
		return nodesmap.values();
	}
	
	//private ArrayList<Node> nodeArray = new ArrayList<Node>();
	
	
	public NodeManager(String par1Str) {	
		super(par1Str);
		nodesmap = new HashMap<Coordonate, NodeBase>();
		nodes = new ArrayList<NodeBase>();
		instance = this;
		// TODO Auto-generated constructor stub
	}

	public void addNode(NodeBase node)
	{
	//	nodeArray.add(node);
		if(node.coordonate == null) 
		{
			Utils.println("Null coordonate addnode");
			while(true);
		}
		nodesmap.put(node.coordonate, node);
		nodes.add(node);
		Utils.println("NodeManager has " + nodesmap.size() +"node");
		//nodeArray.put(new NodeIdentifier(node), node);
	}
	public void removeNode(NodeBase node)
	{
	//	nodeArray.remove(node);
		nodesmap.remove(node.coordonate);
		nodes.remove(node);
		Utils.println("NodeManager has " + nodesmap.size() +"node");
	}
	public void removeCoordonate(Coordonate c)
	{
	//	nodeArray.remove(node);
		NodeBase n = nodesmap.remove(c);
		if(n != null) nodes.remove(n);
		Utils.println("NodeManager has " + nodesmap.size() +"node");
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
		for(Object o : Utils.getTags(nbt))
		{
			NBTTagCompound tag = (NBTTagCompound) o;
			Class nodeClass = UUIDToClass.get(tag.getString("tag"));
			try {
				NodeBase node = (NodeBase) nodeClass.getConstructor().newInstance();
				node.readFromNBT(tag);
				addNode(node);
				node.initializeFromNBT();
				
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		int nodeCounter = 0;
	/*	for(Entry<NodeIdentifier, Node> entry : nodeArray.entrySet())
		{
			NBTTagCompound nbtNode = new NBTTagCompound();
			entry.getKey().writeToNBT(nbtNode,"nId");
			entry.getValue().writeToNBT(nbtNode,"nData");
			nbt.setCompoundTag("node" + nodeCounter++, nbtNode);
		}*/
		//for(Node node : nodeArray)
		for(NodeBase node : nodesmap.values())
		{
			if(node.mustBeSaved() == false) continue;
			NBTTagCompound nbtNode = new NBTTagCompound();
			nbtNode.setString("tag", node.getInfo().getUuid());
			node.writeToNBT(nbtNode);
			nbt.setTag("n" + nodeCounter++, nbtNode);
		}
	}
	
	
	public NodeBase getNodeFromCoordonate(Coordonate nodeCoordonate)
	{
		int idx = 0;
		idx++;
//		for(Node node : nodeArray)
		{
		//	if(nodeCoordonate.equals(node.coordonate)) return node;
		}
		return nodesmap.get(nodeCoordonate);
		//return null;
	}
	Random rand = new Random();
	public NodeBase getRandomNode() {
		if(nodes.size() == 0) return null;
		return nodes.get(rand.nextInt(nodes.size()));
	}
	
}
