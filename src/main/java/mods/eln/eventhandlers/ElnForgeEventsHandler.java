package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.achievepackets.AchievePacket;
import mods.eln.wiki.Root;
import net.minecraftforge.client.event.GuiOpenEvent;

public class ElnForgeEventsHandler {

    private final static AchievePacket p = new AchievePacket("openWiki");

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unused")
    public void openGuide(GuiOpenEvent e) {
        if (e.gui instanceof Root) {
            Eln.achNetwork.sendToServer(p);
        }
    }
}
