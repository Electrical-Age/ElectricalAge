package mods.eln.node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;



import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class NodeManager extends WorldSavedData{
	public static NodeManager instance = null;
	
	HashMap<Coordonate, Node> nodeArray;
	
	public HashMap<Coordonate, Node> getNodeArray()
	{
		return nodeArray;
	}
	public static Class[] UUIDToClass = new Class[4096];
	public static void registerBlock(Block block,Class classType)
	{
		UUIDToClass[block.blockID] = classType;
	}
	
	Collection<Node> getNodeList()
	{
		return nodeArray.values();
	}
	
	//private ArrayList<Node> nodeArray = new ArrayList<Node>();
	
	
	public NodeManager(String par1Str) {	
		super(par1Str);
		nodeArray = new HashMap<Coordonate, Node>();
		instance = this;
		// TODO Auto-generated constructor stub
	}

	public void addNode(Node node)
	{
	//	nodeArray.add(node);
		if(node.coordonate == null) 
		{
			System.out.println("Null coordonate addnode");
			while(true);
		}
		nodeArray.put(node.coordonate, node);
		System.out.println("NodeManager has " + nodeArray.size() +"node");
		//nodeArray.put(new NodeIdentifier(node), node);
	}
	public void removeNode(Node node)
	{
	//	nodeArray.remove(node);
		nodeArray.remove(node.coordonate);
		System.out.println("NodeManager has " + nodeArray.size() +"node");
	}
	@Override
    public boolean isDirty()
    {
        return true;
    }
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		for(Object o : nbt.getTags())
		{
			NBTTagCompound tag = (NBTTagCompound) o;
			Class nodeClass = UUIDToClass[tag.getShort("UUID")];
			try {
				Node node = (Node) nodeClass.getConstructor().newInstance();
				node.readFromNBT(tag, "");
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
		for(Node node : nodeArray.values())
		{
			NBTTagCompound nbtNode = new NBTTagCompound();
			nbtNode.setShort("UUID", node.getBlockId());
			node.writeToNBT(nbtNode,"");
			nbt.setCompoundTag("n" + nodeCounter++, nbtNode);
		}
	}
	
	
	public Node getNodeFromCoordonate(Coordonate nodeCoordonate)
	{
		int idx = 0;
		idx++;
//		for(Node node : nodeArray)
		{
		//	if(nodeCoordonate.equals(node.coordonate)) return node;
		}
		return nodeArray.get(nodeCoordonate);
		//return null;
	}
	
}
