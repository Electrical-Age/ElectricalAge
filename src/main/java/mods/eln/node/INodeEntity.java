package mods.eln.node;

import mods.eln.misc.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.io.DataInputStream;

/**
 * The client-facing side of a node block entity. 1.12 also had newGuiDraw (the GuiScreen); 26.x screens are
 * registered per MenuType on the client (P4).
 */
public interface INodeEntity {
    String getNodeUuid();

    void serverPublishUnserialize(DataInputStream stream);

    void serverPacketUnserialize(DataInputStream stream);

    AbstractContainerMenu newContainer(Direction side, Player player);
}
