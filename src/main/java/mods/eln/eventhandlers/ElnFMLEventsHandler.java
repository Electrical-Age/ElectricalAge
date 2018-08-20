package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import mods.eln.Eln;
import mods.eln.packets.AchievePacket;

public class ElnFMLEventsHandler {

    private final static AchievePacket p = new AchievePacket("craft50VMacerator");

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onCraft(ItemCraftedEvent e) {
        if (e.crafting.getUnlocalizedName().toLowerCase().equals("50v_macerator")) {
            Eln.elnNetwork.sendToServer(p);
        }
    }
}
