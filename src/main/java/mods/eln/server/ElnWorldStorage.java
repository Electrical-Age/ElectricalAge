package mods.eln.server;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class ElnWorldStorage extends WorldSavedData {

    private int dim;

    final static String key = "eln.worldStorage";

    public ElnWorldStorage(String str) {
        super(str);
    }

    public static ElnWorldStorage forWorld(World world) {
        // Retrieves the MyWorldData instance for the given world, creating it if necessary
        MapStorage storage = world.getPerWorldStorage();
        int dim = world.provider.getDimension();
        ElnWorldStorage result = (ElnWorldStorage) storage.getOrLoadData(ElnWorldStorage.class, key + dim);
        if (result == null) {
            result = (ElnWorldStorage) storage.getOrLoadData(ElnWorldStorage.class, key + dim + "back");
        }
        if (result == null) {
            result = new ElnWorldStorage(key + dim);
            result.dim = dim;
            storage.setData(key + dim, result);
        }
        return result;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        dim = nbt.getInteger("dim");
        ServerEventListener.readFromEaWorldNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("dim", dim);
        ServerEventListener.writeToEaWorldNBT(nbt, dim);
        return nbt;
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}
