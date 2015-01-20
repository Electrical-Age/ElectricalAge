package mods.eln.ghost;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.entity.player.EntityPlayer;

public interface GhostObserver {

	public abstract Coordonate getGhostObserverCoordonate();
	public abstract void ghostDestroyed(int UUID);
	public abstract boolean ghostBlockActivated(int UUID, EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz);
}
