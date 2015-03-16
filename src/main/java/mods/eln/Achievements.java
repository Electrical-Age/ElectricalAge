package mods.eln;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

public class Achievements {

    public static Achievement openGuide;
    public static Achievement craft50VMacerator;

    public static AchievementPage achievementPageEln;

    public static void init() {
        openGuide = new Achievement("", "openGuide", 0, 0, Items.book, null);
        craft50VMacerator = new Achievement("", "craft50VMacerator", 0, 2, (ItemStack) Eln.findItemStack("50V Macerator", 0), openGuide);

        achievementPageEln = new AchievementPage("Electrical Age [WIP]", new Achievement[]{openGuide, craft50VMacerator});

        AchievementPage.registerAchievementPage(achievementPageEln);
    }
}
