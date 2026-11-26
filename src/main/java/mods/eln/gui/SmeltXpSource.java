package mods.eln.gui;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

/**
 * A device whose smelting output slots accumulate recipe XP until a player takes from them.
 */
public interface SmeltXpSource {
    /** Drain and return all accumulated smelting XP, resetting the stored amount to zero. */
    double takeSmeltXp();

    /** Award any accumulated smelting XP to the player as experience orbs at their position. */
    default void awardSmeltXp(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        double xp = takeSmeltXp();
        if (xp <= 0) return;
        int whole = Mth.floor(xp);
        double frac = xp - whole;
        if (frac > 0 && sl.getRandom().nextDouble() < frac) whole++;
        if (whole > 0) ExperienceOrb.award(sl, player.position(), whole);
    }
}
