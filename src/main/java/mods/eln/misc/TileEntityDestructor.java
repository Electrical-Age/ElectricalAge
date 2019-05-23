package mods.eln.misc;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import mods.eln.Eln;
import mods.eln.debug.DebugType;
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
                Eln.dp.println(DebugType.RENDER, "destroy light at " + t.xCoord + " " + t.yCoord + " " + t.zCoord);
            }
        }
        destroyList.clear();
    }
}
