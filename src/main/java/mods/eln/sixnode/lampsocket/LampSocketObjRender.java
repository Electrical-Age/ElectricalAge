package mods.eln.sixnode.lampsocket;

import mods.eln.compat.IItemRenderer.ItemRenderType;

public interface LampSocketObjRender {

    void draw(LampSocketDescriptor descriptor, ItemRenderType type, double distanceToPlayer);

    void draw(LampSocketRender render, double distanceToPlayer);
}
