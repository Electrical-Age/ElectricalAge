package mods.eln.sixnode.lampsocket;

import net.minecraftforge.client.IItemRenderer.ItemRenderType;

public interface LampSocketObjRender {

    void draw(LampSocketDescriptor descriptor, ItemRenderType type, double distanceToPlayer);

    void draw(LampSocketRender render, double distanceToPlayer);
}
