package mods.eln.eventhandlers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import mods.eln.Achievements;
import mods.eln.Eln;

public class ElnFMLEventsHandler {

    @SubscribeEvent
    public void onCraft(ItemCraftedEvent e) {
        //TODO This needs to be fixed. Appears to be automagically triggered by first achievement.
        if (e.crafting == Eln.findItemStack("50V Macerator", 1)) {
            e.player.triggerAchievement(Achievements.craft50VMacerator);
        }
    }
}
