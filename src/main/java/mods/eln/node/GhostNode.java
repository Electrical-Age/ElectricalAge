package mods.eln.node;

import mods.eln.Eln;

public abstract class GhostNode extends NodeBase {
    @Override
    public boolean mustBeSaved() {
        return false;
    }

    @Override
    public String getNodeUuid() {
        return Eln.ghostBlock.getNodeUuid();
    }
}
