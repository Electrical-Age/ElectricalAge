package mods.eln;

import static mods.eln.i18n.I18N.*;
import net.minecraft.init.Items;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

import java.util.LinkedList;
import java.util.List;

public class Achievements {

    public static Achievement openGuide;
    public static Achievement craft50VMacerator;
    public static AchievementPage achievementPageEln;

    private static List<Achievement> achievList;

    public static void init() {
        achievList = new LinkedList<Achievement>();

        openGuide = new Achievement(TR("achievement.open_guide"),
            "open_guide", 0, 0, Items.book, null).registerStat();
        achievList.add(openGuide);

        TR_DESC(Type.ACHIEVEMENT, "open_guide");

        craft50VMacerator = new Achievement(TR("achievement.craft_50v_macerator"),
            "craft_50v_macerator", 0, 2, Eln.findItemStack("50V Macerator", 0), openGuide).registerStat();
        achievList.add(craft50VMacerator);

        TR_DESC(Type.ACHIEVEMENT, "craft_50v_macerator");

        achievementPageEln = new AchievementPage(tr("Electrical Age [WIP]"),
            new Achievement[]{openGuide, craft50VMacerator});

        AchievementPage.registerAchievementPage(achievementPageEln);
    }

    public static void populateLangFileKeys(){
        for(Achievement achievItem : achievList){
            String key = achievItem.statId;
            String value = '<'+"..."+'>'; //NB: statName of class StatBase is private with no accessor.
            Eln.langFile_DefaultKeys.put(key,value);
        }
    }
}
