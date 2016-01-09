package mods.eln;

import net.minecraft.init.Items;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

import static mods.eln.i18n.I18N.*;

public class Achievements {

    public static Achievement openGuide;
    public static Achievement craft50VMacerator;
    public static AchievementPage achievementPageEln;

    public static void init() {
        openGuide = new Achievement(TR_NAME(Type.ACHIEVEMENT, "Wiki Power"),
            "open_guide", 0, 0, Items.book, null).registerStat();

        TR_DESC(Type.ACHIEVEMENT, "Open the wiki guide");

        craft50VMacerator = new Achievement(TR_NAME(Type.ACHIEVEMENT, "Crushing Novice"),
            "craft_50v_macerator", 0, 2, Eln.findItemStack("50V Macerator", 0), openGuide).registerStat();

        TR_DESC(Type.ACHIEVEMENT, "Craft a 50V Macerator");

        achievementPageEln = new AchievementPage(tr("Electrical Age [WIP]"),
            openGuide, craft50VMacerator);

        AchievementPage.registerAchievementPage(achievementPageEln);
    }
}
