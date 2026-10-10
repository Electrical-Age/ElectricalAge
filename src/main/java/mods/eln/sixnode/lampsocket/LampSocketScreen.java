package mods.eln.sixnode.lampsocket;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Lamp socket (P4b; layout gui/lamp_socket.json, lamp_socket_fixed.json without the orientation): supply and channel. */
public class LampSocketScreen extends ElnScreen {
    final LampSocketRender render;
    EaField channel;
    EaSlider alpha;

    public LampSocketScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (LampSocketRender) render;
    }

    static boolean fixed(LampSocketDescriptor d) {
        return d.alphaZMin == d.alphaZMax;
    }

    /** The layout without the orientation slider (ClientScreens). */
    public static boolean fixedLayout(Object descriptor) {
        return fixed((LampSocketDescriptor) descriptor);
    }

    @Override
    protected void init() {
        super.init();
        button("supply", () -> render.poweredByLampSupply ? tr("Powered by Lamp Supply") : tr("Powered by cable"),
            () -> render.clientSend(LampSocketElement.tooglePowerSupplyType));
        channel = field("channel", () -> render.channel == null ? "" : render.channel,
            v -> render.clientSetString((byte) LampSocketElement.setChannel, v), tr("Specify the supply channel"));
        text("status", this::status, () -> render.poweredByLampSupply && render.isConnectedToLampSupply
            && !deviceItem(LampSocketContainer.cableSlotId).isEmpty() ? 0xFF00AA00 : 0xFFAA0000);
        LampSocketDescriptor d = render.descriptor;
        if (!fixed(d)) alpha = slider("alpha", d.alphaZMin, d.alphaZMax, (d.alphaZMax - d.alphaZMin) / 200, render.alphaZ,
            v -> Component.literal(tr("Orientation: %1$°", (int) v)), v -> render.clientSetFloat(LampSocketElement.setAlphaZId, (float) v));
        update();
    }

    String status() {
        if (!render.poweredByLampSupply) return "";
        if (deviceItem(LampSocketContainer.cableSlotId).isEmpty()) return tr("Cable slot empty");
        if (render.isConnectedToLampSupply) return tr("connected to %1$", render.channel);
        return tr("%1$ is not in range!", render.channel);
    }

    @Override
    protected void update() {
        channel.active = render.poweredByLampSupply;
        channel.setEditable(render.poweredByLampSupply);
        if (alpha != null) alpha.setFromDevice(render.alphaZ);
    }
}
