package mods.eln.misc;

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
            if (t.getWorldObj() != null && t.getWorldObj().getTileEntity(t.xCoord, t.yCoord, t.zCoord) == t) {
                t.getWorldObj().setBlockToAir(t.xCoord, t.yCoord, t.zCoord);
                Utils.println("destroy light at " + t.xCoord + " " + t.yCoord + " " + t.zCoord);
            }
        }
        destroyList.clear();
    }
}
