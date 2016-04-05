package mods.eln.server;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.server.DelayedTaskManager.ITask;
import net.minecraft.init.Blocks;

import java.util.HashSet;

public class DelayedBlockRemove implements ITask {

    Coordonate c;

	private static final HashSet<Coordonate> blocks = new HashSet<Coordonate>(); 
	
	private DelayedBlockRemove(Coordonate c) {
		this.c = c;
	}

	public static void clear() {
		blocks.clear();
	}

	public static void add(Coordonate c) {
		if (blocks.contains(c)) return;
		blocks.add(c);
		Eln.delayedTask.add(new DelayedBlockRemove(c));
	}

	@Override
	public void run() {
		blocks.remove(c);
		c.setBlock(Blocks.air);
	}
}
