package mods.eln.node;

import mods.eln.init.ModBlock;

public abstract class GhostNode extends NodeBase {
    @Override
    public boolean mustBeSaved() {
        return false;
    }

    @Override
    public String getNodeUuid() {
        return ModBlock.ghostBlock.getUuid();
    }
}
