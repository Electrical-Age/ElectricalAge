package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class GhostElement implements INBTTReady {

    Coordinate elementCoordinate;
    Coordinate observatorCoordinate;
    int UUID;

    public Coordinate getObservatorCoordinate() {
        return observatorCoordinate;
    }

    public GhostElement() {
    }

    public GhostElement(Coordinate elementCoordinate, Coordinate observatorCoordinate, int UUID) {
        this.elementCoordinate = elementCoordinate;
        this.observatorCoordinate = observatorCoordinate;
        this.UUID = UUID;
    }

    public int getUUID() {
        return UUID;
    }

    public void breakBlock() {
        Eln.ghostManager.removeGhost(elementCoordinate);
        GhostObserver observer = Eln.ghostManager.getObserver(observatorCoordinate);
        if (observer != null) observer.ghostDestroyed(UUID);
    }

    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        GhostObserver observer = Eln.ghostManager.getObserver(observatorCoordinate);
        if (observer != null) return observer.ghostBlockActivated(UUID, entityPlayer, side, vx, vy, vz);
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        elementCoordinate = new Coordinate(nbt, str + "elemCoord");
        observatorCoordinate = new Coordinate(nbt, str + "obserCoord");
        UUID = nbt.getInteger(str + "UUID");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {

        elementCoordinate.writeToNBT(nbt, str + "elemCoord");
        observatorCoordinate.writeToNBT(nbt, str + "obserCoord");
        nbt.setInteger(str + "UUID", UUID);
        return nbt;
    }
}
