package mods.eln.misc;


import mods.eln.compat.WorldCompat;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;

public class TileEntityDestructor {

    ArrayList<TileEntity> destroyList = new ArrayList<TileEntity>();

    public TileEntityDestructor() {
        FMLCommonHandler.instance().bus().register(this);
    }

    public void clear() {
        destroyList.clear();
    }

    public void add(TileEntity tile) {
        destroyList.add(tile);
    }

    @SubscribeEvent
    public void tick(ServerTickEvent event) {
        if (event.phase != Phase.START) return;
        for (TileEntity t : destroyList) {
            if (t.getWorld() != null && WorldCompat.getTileEntity(t.getWorld(), t.getPos().getX(), t.getPos().getY(), t.getPos().getZ()) == t) {
                WorldCompat.setBlockToAir(t.getWorld(), t.getPos().getX(), t.getPos().getY(), t.getPos().getZ());
                Utils.println("destroy light at " + t.getPos().getX() + " " + t.getPos().getY() + " " + t.getPos().getZ());
            }
        }
        destroyList.clear();
    }
}
