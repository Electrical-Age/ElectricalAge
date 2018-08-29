package mods.eln.eventhandlers;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.packets.AchievePacket;
import mods.eln.wiki.Root;
import net.minecraftforge.client.event.GuiOpenEvent;

public class ElnForgeEventsHandler {

    private final static AchievePacket p = new AchievePacket("openWiki");

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unused")
    public void openGuide(GuiOpenEvent e) {
        if (e.getGui() instanceof Root) {
            Eln.elnNetwork.sendToServer(p);
        }
    }
}
