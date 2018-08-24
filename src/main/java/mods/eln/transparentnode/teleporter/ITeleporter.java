package mods.eln.transparentnode.teleporter;

import mods.eln.misc.Coordinate;

public interface ITeleporter {
    public Coordinate getTeleportCoordinate();

    public String getName();

    boolean reservate();

    void reservateRefresh(boolean doorState, float processRatio);
}
