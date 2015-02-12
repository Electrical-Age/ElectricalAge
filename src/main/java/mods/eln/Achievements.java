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
        openGuide = new Achievement("achievement.openGuide", "openGuide", 0, 0, Items.book, (Achievement)null).registerStat();
        craft50VMacerator = new Achievement("achievement.craft50VMacerator", "craft50VMacerator", 0, 2, (ItemStack) Eln.findItemStack("50V Macerator", 0), openGuide).registerStat();

        achievementPageEln = new AchievementPage("Electrical Age [WIP]", new Achievement[]{openGuide, craft50VMacerator});

        AchievementPage.registerAchievementPage(achievementPageEln);
    }
}
