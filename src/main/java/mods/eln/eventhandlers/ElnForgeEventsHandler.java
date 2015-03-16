package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Achievements;
import mods.eln.Eln;
import mods.eln.achievepackets.AchievePacket;
import mods.eln.wiki.Root;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;

public class ElnForgeEventsHandler {

	@SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void openGuide(GuiOpenEvent e) {
        //System.out.println("The player: " + player);
        //System.out.println("GUI Opened: " + e.gui);
        if (e.gui instanceof Root) {
            //System.out.println("Giving player achievement!");
            Eln.achNetwork.sendToServer(new AchievePacket("Wiki"));
        }
    }
}
