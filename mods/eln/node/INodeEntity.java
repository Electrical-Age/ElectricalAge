package mods.eln.node;

import java.io.DataInputStream;

import mods.eln.misc.Direction;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public interface INodeEntity {
	String getNodeUuid();
	void serverPublishUnserialize(DataInputStream stream);
	void serverPacketUnserialize(DataInputStream stream);
	GuiScreen newGuiDraw(Direction side, EntityPlayer player);
	Container newContainer(Direction side, EntityPlayer player);
}
