package mods.eln.sixnode.lampsocket;



import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.compat.WorldCompat;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;

public class LightBlockEntity extends TileEntity implements ITickable {

    ArrayList<LightHandle> lightList = new ArrayList<LightHandle>();

    public static final ArrayList<LightBlockObserver> observers = new ArrayList<LightBlockObserver>();

    static void addObserver(LightBlockObserver observer) {
        observers.add(observer);
    }

    static void removeObserver(LightBlockObserver observer) {
        observers.remove(observer);
    }


    public interface LightBlockObserver {
        void lightBlockDestructor(Coordonate coord);
    }

    static class LightHandle implements INBTTReady {
        byte value;
        int timeout;

        public LightHandle() {
            value = 0;
            timeout = 0;
        }

        public LightHandle(byte value, int timeout) {
            this.value = value;
            this.timeout = timeout;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt, String str) {
            value = nbt.getByte(str + "value");
            timeout = nbt.getInteger(str + "timeout");
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt, String str) {
            nbt.setByte(str + "value", value);
            nbt.setInteger(str + "timeout", timeout);
        }
    }

    void addLight(int light, int timeout) {
        lightList.add(new LightHandle((byte) light, timeout));
        lightManager();
    }

	/*void removeLight(int light) {
        //int meta = WorldCompat.getMeta(world, pos.getX(), pos.getY(), pos.getZ());
		for (int idx = 0; idx < lightList.size(); idx++) {
			if (lightList.get(idx) == light) {
				lightList.remove(idx);
				lightManager();
				return;
			}
		}
		Utils.println("Assert void removeLight(int light)");
	}*/
	/*
	void replaceLight(int oldLight, int newLight) {
		for (int idx = 0; idx < lightList.size(); idx++) {
			if (lightList.get(idx) == oldLight) {
				lightList.set(idx, newLight);
				lightManager();
				return;
			}
		}	
		Utils.println("Assert void replaceLight(int oldLight, int newLight)");
	}*/

	/*
	int getLight() {
		int light = 0;
		for (LightHandle l : lightList) {
			if (light < l.value) light = l.value;
		}
		return light;
	}*/

    void lightManager() {
		/*if (lightList.size() == 0) {
			WorldCompat.setBlock(world, pos.getX(), pos.getY(), pos.getZ(), 0);
		} else {
			int light = getLight();
			if (light != WorldCompat.getMeta(world, pos.getX(), pos.getY(), pos.getZ())) {
				WorldCompat.setMeta(world, pos.getX(), pos.getY(), pos.getZ(), light, 2);
				WorldCompat.updateLightByType(world, EnumSkyBlock.BLOCK, pos.getX(), pos.getY(), pos.getZ());
			}
		}*/
    }

    /** 1.7.10 kept the TE when only the meta (light level) changed; 1.12 would recreate it. */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        if (lightList.isEmpty()) {
            //	WorldCompat.setMeta(world, pos.getX(), pos.getY(), pos.getZ(), 1, 2);
            WorldCompat.setBlockToAir(world, pos.getX(), pos.getY(), pos.getZ());
            //WorldCompat.updateLightByType(world, EnumSkyBlock.BLOCK, pos.getX(), pos.getY(), pos.getZ());
            //Eln.instance.tileEntityDestructor.add(this);
            Utils.println("Destroy light at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " ");
            return;
        }

        int light = 0;
        Iterator<LightHandle> iterator = lightList.iterator();

        while (iterator.hasNext()) {
            LightHandle l = iterator.next();
            if (light < l.value) light = l.value;

            l.timeout--;
            if (l.timeout <= 0) {
                iterator.remove();
            }
        }

        if (light != WorldCompat.getMeta(world, pos.getX(), pos.getY(), pos.getZ())) {

            WorldCompat.setMeta(world, pos.getX(), pos.getY(), pos.getZ(), light, 2);
            WorldCompat.updateLightByType(world, EnumSkyBlock.BLOCK, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public static void addLight(World w, int x, int y, int z, int light, int timeout) {
        Block block = WorldCompat.getBlock(w, x, y, z);
        if (block != ElnDeviceRegistry.lightBlock) {
            if (block != Blocks.AIR) return;
            WorldCompat.setBlock(w, x, y, z, ElnDeviceRegistry.lightBlock, light, 2);
        }

        TileEntity t = WorldCompat.getTileEntity(w, x, y, z);
        if (t != null && t instanceof LightBlockEntity)
            ((LightBlockEntity) t).addLight(light, timeout);
        else
            Utils.println("ASSERT if(t != null && t instanceof LightBlockEntity)");
    }

    public static void addLight(Coordonate coord, int light, int timeout) {
        addLight(coord.world(), coord.x, coord.y, coord.z, light, timeout);
    }

	/*public static void removeLight(Coordonate coord, int light) {
		int blockId = coord.getBlockId();
		if (blockId != Eln.lightBlockId) return;
		((LightBlockEntity)coord.getTileEntity()).removeLight(light);
	}
	
	public static void replaceLight(Coordonate coord, int oldLight, int newLight) {
		int blockId = coord.getBlockId();
		if (blockId != Eln.lightBlockId) {
			//coord.setBlock(Eln.lightBlockId, newLight);
			Utils.println("ASSERT public static void replaceLight(Coordonate coord, int oldLight, int newLight) " + coord);
			return;
		}
		((LightBlockEntity)coord.getTileEntity()).replaceLight(oldLight,newLight);
	}*/

	/*public int getClientLight() {
		return clientLight;
	}
	
	int clientLight = 0;*/
}
