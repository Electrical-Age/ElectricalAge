package mods.eln;

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

        openGuide = new Achievement("achievement.openGuide", "openGuide", 0, 0, Items.book, null).registerStat();
        achievList.add(openGuide);

        craft50VMacerator = new Achievement("achievement.craft50VMacerator", "craft50VMacerator", 0, 2, Eln.findItemStack("50V Macerator", 0), openGuide).registerStat();
        achievList.add(craft50VMacerator);


        achievementPageEln = new AchievementPage("Electrical Age [WIP]", new Achievement[]{openGuide, craft50VMacerator});

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
