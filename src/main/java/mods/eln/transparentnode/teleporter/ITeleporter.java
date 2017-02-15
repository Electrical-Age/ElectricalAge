package mods.eln.transparentnode.teleporter;

import mods.eln.misc.Coordonate;

public interface ITeleporter {
    public Coordonate getTeleportCoordonate();

    public String getName();

    boolean reservate();

    void reservateRefresh(boolean doorState, float processRatio);
}
