package mods.eln.node;

import mods.eln.misc.Direction;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.DataInputStream;

public interface INodeEntity {
    String getNodeUuid();

    void serverPublishUnserialize(DataInputStream stream);

    void serverPacketUnserialize(DataInputStream stream);

    @SideOnly(Side.CLIENT)
    GuiScreen newGuiDraw(Direction side, EntityPlayer player);

    Container newContainer(Direction side, EntityPlayer player);
}
