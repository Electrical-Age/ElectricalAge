package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class GhostManager extends WorldSavedData {
    public GhostManager(String par1Str) {
        super(par1Str);
    }

    Map<Coordinate, GhostElement> ghostTable = new Hashtable<Coordinate, GhostElement>();
    Map<Coordinate, GhostObserver> observerTable = new Hashtable<Coordinate, GhostObserver>();

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
		ghostTable.put(element.elementCoordinate, element);
	}*/

    public GhostElement getGhost(Coordinate coordinate) {
        return ghostTable.get(coordinate);
    }

    public void removeGhost(Coordinate coordinate) {
        removeGhostNode(coordinate);
        ghostTable.remove(coordinate);
    }

    public void addObserver(GhostObserver observer) {
        observerTable.put(observer.getGhostObserverCoordinate(), observer);
    }

    public GhostObserver getObserver(Coordinate coordinate) {
        return observerTable.get(coordinate);
    }

    public void removeObserver(Coordinate coordinate) {
        observerTable.remove(coordinate);
    }

    public void removeGhostAndBlockWithObserver(Coordinate observerCoordinate) {
        Iterator<Entry<Coordinate, GhostElement>> iterator = ghostTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Coordinate, GhostElement> entry = iterator.next();
            GhostElement element = entry.getValue();
            if (element.observatorCoordinate.equals(observerCoordinate)) {
                iterator.remove();
                removeGhostNode(element.elementCoordinate);
                element.elementCoordinate.world().setBlockToAir(element.elementCoordinate.pos);
            }
        }
    }

    public void removeGhostAndBlockWithObserver(Coordinate observerCoordinate, int uuid) {
        Iterator<Entry<Coordinate, GhostElement>> iterator = ghostTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Coordinate, GhostElement> entry = iterator.next();
            GhostElement element = entry.getValue();
            if (element.observatorCoordinate.equals(observerCoordinate) && element.getUUID() == uuid) {
                iterator.remove();
                removeGhostNode(element.elementCoordinate);
                element.elementCoordinate.world().setBlockToAir(element.elementCoordinate.pos);
            }
        }
    }

    public void removeGhostAndBlockWithObserverAndNotUuid(Coordinate observerCoordinate, int uuid) {
        Iterator<Entry<Coordinate, GhostElement>> iterator = ghostTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Coordinate, GhostElement> entry = iterator.next();
            GhostElement element = entry.getValue();
            if (element.observatorCoordinate.equals(observerCoordinate) && element.getUUID() != uuid) {
                iterator.remove();
                removeGhostNode(element.elementCoordinate);
                element.elementCoordinate.world().setBlockToAir(element.elementCoordinate.pos);
            }
        }
    }

    public void removeGhostNode(Coordinate c) {
        NodeBase node = NodeManager.instance.getNodeFromCoordinate(c);
        if (node == null) return;
        node.onBreakBlock();
    }

    public void removeGhostAndBlock(Coordinate coordinate) {
        removeGhost(coordinate);
        coordinate.world().setBlockToAir(coordinate.pos); //caca1.5.1
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
		/*for(NBTTagCompound o : Utils.getTags(nbt)) {
			NBTTagCompound tag = (NBTTagCompound) o;

			GhostElement ghost = new GhostElement();
			ghost.readFromNBT(tag, "");
			ghostTable.put(ghost.elementCoordinate, ghost);
		}*/
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
	/*	int nodeCounter = 0;
		
		for(GhostElement ghost : ghostTable.values()) {
			NBTTagCompound nbtGhost = new NBTTagCompound();
			ghost.writeToNBT(nbtGhost, "");
			nbt.setTag("n" + nodeCounter++, nbtGhost);
		}*/
    }

    public void loadFromNBT(NBTTagCompound nbt) {
        for (NBTTagCompound o : Utils.getTags(nbt)) {
            NBTTagCompound tag = (NBTTagCompound) o;

            GhostElement ghost = new GhostElement();
            ghost.readFromNBT(tag, "");
            ghostTable.put(ghost.elementCoordinate, ghost);
        }
    }

    public void saveToNBT(NBTTagCompound nbt, int dim) {
        int nodeCounter = 0;

        for (GhostElement ghost : ghostTable.values()) {
            if (dim != Integer.MIN_VALUE && ghost.elementCoordinate.getDimension() != dim) continue;
            NBTTagCompound nbtGhost = new NBTTagCompound();
            ghost.writeToNBT(nbtGhost, "");
            nbt.setTag("n" + nodeCounter++, nbtGhost);
        }
    }

    public void unload(int dimensionId) {
        Iterator<GhostElement> i = ghostTable.values().iterator();

        while (i.hasNext()) {
            GhostElement n = i.next();
            if (n.elementCoordinate.getDimension() == dimensionId) {
                i.remove();
            }
        }
    }

    public boolean canCreateGhostAt(World world, BlockPos pos) {
        //Probably isn't needed anymore since now when asked for a chunk even if it isnt generated it auto generates it
        /*
        if (!world.getChunkProvider().chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        */
        if (world.isAirBlock(pos) && !world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            return false;
        } else return true;
    }

    public void createGhost(Coordinate coordinate, Coordinate observerCoordinate, int UUID) {
        createGhost(coordinate, observerCoordinate, UUID, Eln.ghostBlock, GhostBlock.tCube);
    }

    public void createGhost(Coordinate coordinate, Coordinate observerCoordinate, int UUID, Block block, int meta) {
        coordinate.world().setBlockToAir(coordinate.pos);

        if (coordinate.world().setBlockState(coordinate.pos, block.getStateFromMeta(meta), 3)) {
            coordinate = new Coordinate(coordinate);
            GhostElement element = new GhostElement(coordinate, observerCoordinate, UUID);
            ghostTable.put(element.elementCoordinate, element);
        }
    }
}
