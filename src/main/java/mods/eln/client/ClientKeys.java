package mods.eln.client;

import com.mojang.blaze3d.platform.InputConstants;
import mods.eln.Eln;
import mods.eln.network.ElnNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * EA's keys (1.12 ClientKeyHandler): the wrench key (C), held while clicking a switch to open its screen. With a chisel
 * in the main hand it opens the chisel's brush menu instead (ChiselRadial), and the server is not told the key is down.
 */
final class ClientKeys {
    private ClientKeys() {
    }

    static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Eln.MODID, "keys"));
    static final KeyMapping WRENCH = new KeyMapping("key.eln.wrench", InputConstants.KEY_C, CATEGORY);
    static boolean wrenchDown;

    static void register(RegisterKeyMappingsEvent e) {
        e.registerCategory(CATEGORY);
        e.register(WRENCH);
    }

    static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        boolean clicked = false;
        while (WRENCH.consumeClick()) clicked = true;
        boolean down = WRENCH.isDown();
        if ((down || clicked) && !wrenchDown && mc.gui.screen() == null && mc.player != null
            && mc.player.getMainHandItem().getItem() instanceof mods.eln.node.voxel.ChiselItem) {
            // (a tap shorter than a tick leaves only the click: the menu then stays open until a choice or Esc)
            mc.gui.setScreen(ChiselRadial.create(mc.player.getMainHandItem(), WRENCH, down));
            return;
        }
        if (down == wrenchDown) return;
        wrenchDown = down;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeByte(Eln.packetPlayerKey);
            out.writeByte(mods.eln.server.PlayerKeys.WRENCH);
            out.writeBoolean(down);
            ElnNet.sendToServer(bos);
        } catch (IOException ignored) {
        }
    }
}
