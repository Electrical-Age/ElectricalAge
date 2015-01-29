package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class GhostManager extends WorldSavedData {
	public GhostManager(String par1Str) {
		super(par1Str);
	}

	Hashtable<Coordonate, GhostElement> ghostTable = new Hashtable<Coordonate, GhostElement>();
	Hashtable<Coordonate, GhostObserver> observerTable = new Hashtable<Coordonate, GhostObserver>();

	public void clear() {
		ghostTable.clear();
		observerTable.clear();
	}
	
	public void init() {
	}

	@Override
	public boolean isDirty() {
		return true;
	}

	/*
	public void addGhost(GhostElement element) {
		ghostTable.put(element.elementCoordonate, element);
	}*/

	public GhostElement getGhost(Coordonate coordonate) {
		return ghostTable.get(coordonate);
	}

	public void removeGhost(Coordonate coordonate) {
		removeGhostNode(coordonate);
		ghostTable.remove(coordonate);
	}
	
	public void addObserver(GhostObserver observer) {
		observerTable.put(observer.getGhostObserverCoordonate(), observer);
	}

	public GhostObserver getObserver(Coordonate coordonate) {
		return observerTable.get(coordonate);
	}

	public void removeObserver(Coordonate coordonate) {
		observerTable.remove(coordonate);
	}
	
	public void removeGhostAndBlockWithObserver(Coordonate observerCoordonate) {
		Iterator<Entry<Coordonate, GhostElement>> iterator = ghostTable.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry<Coordonate, GhostElement> entry = iterator.next();
			GhostElement element = entry.getValue();
			if(element.observatorCoordonate.equals(observerCoordonate)) {
				iterator.remove();
				removeGhostNode(element.elementCoordonate);
				element.elementCoordonate.world().setBlockToAir(element.elementCoordonate.x, element.elementCoordonate.y, element.elementCoordonate.z);
			}			
		}	
	}

	public void removeGhostAndBlockWithObserver(Coordonate observerCoordonate, int uuid) {
		Iterator<Entry<Coordonate, GhostElement>> iterator = ghostTable.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry<Coordonate, GhostElement> entry = iterator.next();
			GhostElement element = entry.getValue();
			if(element.observatorCoordonate.equals(observerCoordonate) && element.getUUID() == uuid) {
				iterator.remove();
				removeGhostNode(element.elementCoordonate);
				element.elementCoordonate.world().setBlockToAir(element.elementCoordonate.x, element.elementCoordonate.y, element.elementCoordonate.z);
			}			
		}	
	}

	public void removeGhostAndBlockWithObserverAndNotUuid(Coordonate observerCoordonate, int uuid) {
		Iterator<Entry<Coordonate, GhostElement>> iterator = ghostTable.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry<Coordonate, GhostElement> entry = iterator.next();
			GhostElement element = entry.getValue();
			if(element.observatorCoordonate.equals(observerCoordonate) && element.getUUID() != uuid) {
				iterator.remove();
				removeGhostNode(element.elementCoordonate);
				element.elementCoordonate.world().setBlockToAir(element.elementCoordonate.x, element.elementCoordonate.y, element.elementCoordonate.z);
			}			
		}	
	}	

	public void removeGhostNode(Coordonate c) {
		NodeBase node = NodeManager.instance.getNodeFromCoordonate(c);
		if(node == null) return;
		node.onBreakBlock();
	}
	
	public void removeGhostAndBlock(Coordonate coordonate) {
		removeGhost(coordonate);
		coordonate.world().setBlockToAir(coordonate.x, coordonate.y, coordonate.z); //caca1.5.1
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		/*for(NBTTagCompound o : Utils.getTags(nbt)) {
			NBTTagCompound tag = (NBTTagCompound) o;

			GhostElement ghost = new GhostElement();
			ghost.readFromNBT(tag, "");
			ghostTable.put(ghost.elementCoordonate, ghost);
		}*/		
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
	/*	int nodeCounter = 0;
		
		for(GhostElement ghost : ghostTable.values()) {
			NBTTagCompound nbtGhost = new NBTTagCompound();
			ghost.writeToNBT(nbtGhost, "");
			nbt.setTag("n" + nodeCounter++, nbtGhost);
		}*/	
	}

	public void loadFromNBT(NBTTagCompound nbt) {
		for(NBTTagCompound o : Utils.getTags(nbt)) {
			NBTTagCompound tag = (NBTTagCompound) o;

			GhostElement ghost = new GhostElement();
			ghost.readFromNBT(tag, "");
			ghostTable.put(ghost.elementCoordonate, ghost);
		}		
	}

	public void saveToNBT(NBTTagCompound nbt,int dim) {
		int nodeCounter = 0;
		
		for(GhostElement ghost : ghostTable.values()) {
			if(dim != Integer.MIN_VALUE && ghost.elementCoordonate.dimention != dim) continue;
			NBTTagCompound nbtGhost = new NBTTagCompound();
			ghost.writeToNBT(nbtGhost, "");
			nbt.setTag("n" + nodeCounter++, nbtGhost);
		}		
	}

	public void unload(int dimensionId) {
		Iterator<GhostElement> i = ghostTable.values().iterator();
		
		while(i.hasNext()) {
			GhostElement n = i.next();
			if(n.elementCoordonate.dimention == dimensionId) {
				i.remove();
			}
		}
	}
	
	public boolean canCreateGhostAt(World world,int x,int y, int z) {
		if(!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) {
			return false;
		} else if (world.getBlock(x, y, z) != Blocks.air && !world.getBlock(x, y, z).isReplaceable(world, x, y, z)) {
			return false;
		} else return true;
	}

	public void createGhost(Coordonate coordonate, Coordonate observerCoordonate, int UUID) {
		createGhost(coordonate, observerCoordonate, UUID, Eln.ghostBlock, GhostBlock.tCube);
	}
	
	public void createGhost(Coordonate coordonate, Coordonate observerCoordonate, int UUID, Block block, int meta) {
		coordonate.world().setBlockToAir(coordonate.x, coordonate.y, coordonate.z);
		if(coordonate.world().setBlock(coordonate.x, coordonate.y, coordonate.z, block, meta, 3)) {
			coordonate = new Coordonate(coordonate);
			GhostElement element = new GhostElement(coordonate, observerCoordonate, UUID);
			ghostTable.put(element.elementCoordonate, element);
		}
	}
}
