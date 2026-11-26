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
 * A press with a device in hand turns its placement preview (DevicePreview.wrenchPressed, on the server).
 */
public final class ClientKeys {
    private ClientKeys() {
    }

    static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Eln.MODID, "keys"));
    static final KeyMapping WRENCH = new KeyMapping("key.eln.wrench", InputConstants.KEY_C, CATEGORY);
    /** Lights on/off: the worn headlamp and the flashlights in hand (Baughn 2026-10-30; K: free in the pack, L is advancements). */
    static final KeyMapping HEADLAMP = new KeyMapping("key.eln.headlamp", InputConstants.KEY_K, CATEGORY);
    /** The Ground Radar w/Goggles' band-pass edges, in its view (Baughn 2026-11-24: four keys): low down/up, high down/up. */
    public static final KeyMapping RADAR_LOW_DOWN = new KeyMapping("key.eln.radar_low_down", InputConstants.KEY_LBRACKET, CATEGORY),
        RADAR_LOW_UP = new KeyMapping("key.eln.radar_low_up", InputConstants.KEY_RBRACKET, CATEGORY),
        RADAR_HIGH_DOWN = new KeyMapping("key.eln.radar_high_down", InputConstants.KEY_SEMICOLON, CATEGORY),
        RADAR_HIGH_UP = new KeyMapping("key.eln.radar_high_up", InputConstants.KEY_APOSTROPHE, CATEGORY);
    /** The goggles' next named band (Baughn 1575645294446518445), beside the edge keys. */
    public static final KeyMapping RADAR_BAND = new KeyMapping("key.eln.radar_band", InputConstants.KEY_BACKSLASH, CATEGORY);
    static boolean wrenchDown;

    static void register(RegisterKeyMappingsEvent e) {
        e.registerCategory(CATEGORY);
        e.register(WRENCH);
        e.register(HEADLAMP);
        e.register(RADAR_LOW_DOWN);
        e.register(RADAR_LOW_UP);
        e.register(RADAR_HIGH_DOWN);
        e.register(RADAR_HIGH_UP);
        e.register(RADAR_BAND);
    }

    static void send(int key, boolean down) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeByte(Eln.packetPlayerKey);
            out.writeByte(key);
            out.writeBoolean(down);
            ElnNet.sendToServer(bos);
        } catch (IOException ignored) {
        }
    }

    static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        while (HEADLAMP.consumeClick()) if (mc.gui.screen() == null) send(mods.eln.server.PlayerKeys.HEADLAMP, true);
        boolean clicked = false;
        while (WRENCH.consumeClick()) clicked = true;
        boolean down = WRENCH.isDown();
        if ((down || clicked) && !wrenchDown && mc.gui.screen() == null && mc.player != null
            && mc.player.getMainHandItem().getItem() instanceof mods.eln.node.voxel.ChiselItem) {
            // (a tap shorter than a tick leaves only the click: the menu then stays open until a choice or Esc)
            mc.gui.setScreen(ChiselRadial.create(mc.player.getMainHandItem(), WRENCH, down));
            return;
        }
        // (a tap shorter than a tick: down and up, so the server sees the press; it turns a device in hand before placing)
        if (clicked && !down && !wrenchDown && mc.gui.screen() == null) syncWrench(true);
        syncWrench(down);
    }

    static void syncWrench(boolean down) {
        if (down == wrenchDown) return;
        wrenchDown = down;
        send(mods.eln.server.PlayerKeys.WRENCH, down);
    }

    /**
     * Before a click reaches the server: the tick sends the key's state only after the clicks of that tick, so C pressed
     * in the same tick as the right-click arrived after it (the click then opened the screen instead of turning).
     */
    static void beforeClick(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered e) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() != null && mc.player != null && mc.player.getMainHandItem().getItem() instanceof mods.eln.node.voxel.ToolbagItem) {
            // (the Toolbag's sneak-click opens its radial: shift pressed in the same tick as the click would reach the
            // server after it, as the wrench key did, and the click lay instead; AdventurAgent on a pipe route)
            boolean shift = mc.options.keyShift.isDown();
            var last = mc.player.getLastSentInput();
            if (last.shift() != shift) {
                var k = mc.player.input.keyPresses;
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(
                    new net.minecraft.world.entity.player.Input(k.forward(), k.backward(), k.left(), k.right(), k.jump(), shift, k.sprint())));
            }
        }
        if (mc.getConnection() == null || mc.player == null || mc.player.getMainHandItem().getItem() instanceof mods.eln.node.voxel.ChiselItem) return;
        syncWrench(WRENCH.isDown());
    }
}
