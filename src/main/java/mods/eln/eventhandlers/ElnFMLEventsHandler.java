package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import mods.eln.Eln;
import mods.eln.achievepackets.AchievePacket;

public class ElnFMLEventsHandler {

    @SubscribeEvent
    public void onCraft(ItemCraftedEvent e) {
        /*TODO Find a better solution for getting the block (different languages will cause this not to work)
		Isn't there a way to get a block by referencing its class? Need to look into this*/
        if (e.crafting.getUnlocalizedName() == "50v_macerator") {
            Eln.achNetwork.sendToServer(new AchievePacket("craft50VMacerator"));
        }
    }
}
