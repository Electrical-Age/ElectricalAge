package mods.eln.gametest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A fake player that keeps what it is told, split by where it would show: the chat or the action bar. */
public final class ListeningPlayer extends net.neoforged.neoforge.common.util.FakePlayer {
    public final List<String> chat = new ArrayList<>(), actionBar = new ArrayList<>();

    public ListeningPlayer(ServerLevel level) {
        super(level, new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("eln-listener".getBytes()), "eln_listener"));
    }

    @Override
    public void sendSystemMessage(Component message, boolean overlay) {
        (overlay ? actionBar : chat).add(message.getString());
    }

    /** The last action-bar line, or "". */
    public String lastActionBar() {
        return actionBar.isEmpty() ? "" : actionBar.get(actionBar.size() - 1);
    }
}
