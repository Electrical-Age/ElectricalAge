package mods.eln.server;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.server.DelayedTaskManager.ITask;
import net.minecraft.init.Blocks;

import java.util.HashSet;

public class DelayedBlockRemove implements ITask {

    Coordinate c;

	private static HashSet<Coordinate> blocks = new HashSet<Coordinate>();
	
	private DelayedBlockRemove(Coordinate c) {
		this.c = c;
	}

	public static void clear() {
		blocks.clear();
	}

	public static void add(Coordinate c) {
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
