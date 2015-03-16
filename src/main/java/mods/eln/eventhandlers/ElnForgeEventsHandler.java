package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mods.eln.Eln;
import mods.eln.achievepackets.AchievePacket;
import mods.eln.wiki.Root;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;

public class ElnForgeEventsHandler {

    @SubscribeEvent
    public void openGuide(GuiOpenEvent e) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        if (e.gui instanceof Root) {
            Eln.achNetwork.sendToServer(new AchievePacket("openWiki"));
        }
    }
}
