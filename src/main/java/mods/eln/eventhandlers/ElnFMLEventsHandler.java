package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import mods.eln.Eln;
import mods.eln.achievepackets.AchievePacket;

public class ElnFMLEventsHandler {

    @SubscribeEvent
    public void onCraft(ItemCraftedEvent e) {
        //Got it!
        if (e.crafting.getUnlocalizedName().equals("50v_macerator")) {
            Eln.achNetwork.sendToServer(new AchievePacket("craft50VMacerator"));
            System.out.println("Sending achievement packet!");
        }
    }
}
