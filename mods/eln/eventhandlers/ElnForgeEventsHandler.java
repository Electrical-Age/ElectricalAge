package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mods.eln.Achievements;
import mods.eln.wiki.Root;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;

public class ElnForgeEventsHandler {

    @SubscribeEvent
    public void openGuide(GuiOpenEvent e) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        // Doesn't seem to trigger.
        if (e.gui instanceof Root) {
            player.triggerAchievement(Achievements.openGuide);
        }
    }
}
