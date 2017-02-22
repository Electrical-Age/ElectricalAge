package mods.eln.server;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeManager;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Save;
import net.minecraftforge.event.world.WorldEvent.Unload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.LinkedList;

public class ServerEventListener {

    private LinkedList<EntityLightningBolt> lightningListNext = new LinkedList<EntityLightningBolt>();
    private LinkedList<EntityLightningBolt> lightningList = new LinkedList<EntityLightningBolt>();

    public ServerEventListener() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void tick(ServerTickEvent event) {
        if (event.phase != Phase.END) return;

        lightningList = lightningListNext;
        lightningListNext = new LinkedList<EntityLightningBolt>();
    }

    @SubscribeEvent
    public void onNewEntity(EntityConstructing event) {
        if (event.entity instanceof EntityLightningBolt) {
            lightningListNext.add((EntityLightningBolt) event.entity);
        }
    }

    public void clear() {
        lightningList.clear();
    }

    public double getLightningClosestTo(Coordonate c) {
        double best = 10000000;
        for (EntityLightningBolt l : lightningList) {
            if (c.world() != l.worldObj) continue;
            double d = l.getDistance(c.x, c.y, c.z);
            if (d < best) best = d;
        }
        return best;
    }


    private String getEaWorldSaveName(World w) {
        return Utils.getMapFolder() + "data/electricalAgeWorld" + w.provider.dimensionId + ".dat";
    }

    private HashSet<Integer> loadedWorlds = new HashSet<Integer>();

    @SubscribeEvent
    public void onWorldLoad(Load e) {
        if (e.world.isRemote) return;
        loadedWorlds.add(e.world.provider.dimensionId);
        FileNames fileNames = new FileNames(e);

        try {
            FileInputStream fileStream = new FileInputStream(fileNames.worldSaveName);
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(fileStream);
            readFromEaWorldNBT(nbt);
            fileStream.close();
        } catch (Exception ex) {
            try {
                ex.printStackTrace();
                FileInputStream fileStream = new FileInputStream(fileNames.backupSaveName);
                System.out.println("Using BACKUP Electrical Age save: " + fileNames.backupSaveName);
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(fileStream);
                readFromEaWorldNBT(nbt);
                fileStream.close();
            } catch (Exception ex2) {
                ex2.printStackTrace();
                System.out.println("Failed to read backup save!");
                ElnWorldStorage storage = ElnWorldStorage.forWorld(e.world);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(Unload e) {
        if (e.world.isRemote) return;
        loadedWorlds.remove(e.world.provider.dimensionId);
        try {
            NodeManager.instance.unload(e.world.provider.dimensionId);
            Eln.ghostManager.unload(e.world.provider.dimensionId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @SubscribeEvent
    public void onWorldSave(Save e) {
        if (e.world.isRemote) return;
        if (!loadedWorlds.contains(e.world.provider.dimensionId)) {
            //System.out.println("I hate you minecraft");
            return;
        }
        try {
            NBTTagCompound nbt = new NBTTagCompound();
            writeToEaWorldNBT(nbt, e.world.provider.dimensionId);

            FileNames fileNames = new FileNames(e);
            String tempSaveName = fileNames.tempSaveName;
            String worldSaveName = fileNames.worldSaveName;
            String backupSaveName = fileNames.backupSaveName;

            File failedSave = new File(tempSaveName);
            if (failedSave.exists()) {
                failedSave.delete();
            }

            FileOutputStream fileStream = new FileOutputStream(tempSaveName);
            CompressedStreamTools.writeCompressed(nbt, fileStream);
            fileStream.flush();
            fileStream.close();

            new File(worldSaveName).renameTo(new File(backupSaveName));
            new File(tempSaveName).renameTo(new File(worldSaveName));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    static void readFromEaWorldNBT(NBTTagCompound nbt) {
        try {
            NodeManager.instance.loadFromNbt(nbt.getCompoundTag("nodes"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Eln.ghostManager.loadFromNBT(nbt.getCompoundTag("ghost"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void writeToEaWorldNBT(NBTTagCompound nbt, int dim) {
        try {
            NodeManager.instance.saveToNbt(Utils.newNbtTagCompund(nbt, "nodes"), dim);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Eln.ghostManager.saveToNBT(Utils.newNbtTagCompund(nbt, "ghost"), dim);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class FileNames {
        final String worldSaveName;
        final String tempSaveName;
        final String backupSaveName;

        FileNames(WorldEvent e) {
            worldSaveName = getEaWorldSaveName(e.world);
            tempSaveName = worldSaveName + ".tmp";
            backupSaveName = worldSaveName + ".bak";
        }
    }
}
