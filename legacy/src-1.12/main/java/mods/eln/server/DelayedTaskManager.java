package mods.eln.server;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DelayedTaskManager {

    LinkedList<ITask> tasks = new LinkedList<DelayedTaskManager.ITask>();

    public DelayedTaskManager() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    public void clear() {
        tasks.clear();
    }

    @SubscribeEvent
    public void tick(ServerTickEvent event) {
        if (event.phase != Phase.END) return;
        List<ITask> cpy = new ArrayList<DelayedTaskManager.ITask>(tasks);
        tasks.clear();
        for (ITask t : cpy) {
            t.run();
        }
    }

    interface ITask {
        void run();
    }

    public void add(ITask t) {
        tasks.add(t);
    }
}
