package mods.eln.handbook;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The Ledger: using it opens the handbook (the server asks the client's GuideME to open it). */
public class HandbookItem extends Item {
    public HandbookItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            if (Handbook.guideAvailable()) GuideMeBridge.open(sp);
            else sp.sendSystemMessage(Component.translatable("item.eln.handbook.no_guideme"));
        }
        return InteractionResult.SUCCESS;
    }
}
