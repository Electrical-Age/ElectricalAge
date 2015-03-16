package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.achievepackets.AchievePacket;

public class ElnFMLEventsHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onCraft(ItemCraftedEvent e) {
        if (e.crafting.getDisplayName() == "50V Macerator") {
            Eln.achNetwork.sendToServer(new AchievePacket("craft50VMacerator"));
        }
    }
}
