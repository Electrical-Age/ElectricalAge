package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

public class GhostElement implements INBTTReady {

    Coordonate elementCoordonate;
    Coordonate observatorCoordonate;
    int UUID;

    public Coordonate getObservatorCoordonate() {
        return observatorCoordonate;
    }

    public GhostElement() {
    }

    public GhostElement(Coordonate elementCoordonate, Coordonate observatorCoordonate, int UUID) {
        this.elementCoordonate = elementCoordonate;
        this.observatorCoordonate = observatorCoordonate;
        this.UUID = UUID;
    }

    public int getUUID() {
        return UUID;
    }

    public void breakBlock() {
        Eln.ghostManager.removeGhost(elementCoordonate);
        GhostObserver observer = Eln.ghostManager.getObserver(observatorCoordonate);
        if (observer != null) observer.ghostDestroyed(UUID);
    }

    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        GhostObserver observer = Eln.ghostManager.getObserver(observatorCoordonate);
        if (observer != null) return observer.ghostBlockActivated(UUID, entityPlayer, side, vx, vy, vz);
        return false;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        elementCoordonate = new Coordonate(nbt, str + "elemCoord");
        observatorCoordonate = new Coordonate(nbt, str + "obserCoord");
        UUID = nbt.getIntOr(str + "UUID", 0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {

        elementCoordonate.writeToNBT(nbt, str + "elemCoord");
        observatorCoordonate.writeToNBT(nbt, str + "obserCoord");
        nbt.putInt(str + "UUID", UUID);
    }
}
