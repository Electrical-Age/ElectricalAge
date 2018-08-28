package mods.eln.client;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import mods.eln.Eln;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Root;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientKeyHandler {

    static public final int openWikiId = 0;
    static public final int wrenchId = 1;
    static final String openWiki = "Open Wiki";
    static final String wrench = "Wrench";
    private static final int[] keyValues = {Keyboard.KEY_X, Keyboard.KEY_C};
    private static final String[] desc = {openWiki, wrench};
    public static final KeyBinding[] keys = new KeyBinding[desc.length];

    boolean[] states = new boolean[desc.length];

    Minecraft mc;

    public ClientKeyHandler() {
        mc = Minecraft.getMinecraft();

        for (int i = 0; i < desc.length; ++i) {
            if (i != 3)
                states[i] = false;
            keys[i] = new KeyBinding(desc[i], keyValues[i], "ElectricalAge");
            ClientRegistry.registerKeyBinding(keys[i]);
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        for (int i = 0; i < desc.length; ++i) {
            boolean s = keys[i].isPressed();
            if (s == false) continue;
            if (states[i])
                setState(i, false);
            setState(i, true);
        }
    }

    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        for (int i = 0; i < desc.length; ++i) {
            boolean s = keys[i].isPressed();
            if (s == false && states[i] == true) {
                setState(i, false);
            }
        }
    }

    void setState(int id, boolean state) {
        states[id] = state;

        if (id == openWikiId) {
            UtilsClient.clientOpenGui(new Root(null));
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(Eln.packetPlayerKey);
            stream.writeByte(id);
            stream.writeBoolean(state);
        } catch (IOException e) {
            e.printStackTrace();
        }

        UtilsClient.sendPacketToServer(bos);
    }
}
