package mods.eln.misc;


import net.minecraft.util.math.BlockPos;
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
            BlockPos pos = t.getPos();
            if (t.getWorld().getTileEntity(new BlockPos(pos.getX(), pos.getY(), pos.getZ())) == t) {
                t.getWorld().setBlockToAir(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
                Utils.println("destroy light at " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            }
        }
        destroyList.clear();
    }
}
