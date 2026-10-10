package mods.eln.sixnode.lampsupply;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static mods.eln.i18n.I18N.tr;

/** Lamp supply (P4b; layout gui/lamp_supply.json): per channel a power and a wireless channel name and the aggregator. */
public class LampSupplyScreen extends ElnScreen {
    final LampSupplyRender render;
    final java.util.List<Runnable> enabling = new java.util.ArrayList<>();

    public LampSupplyScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (LampSupplyRender) render;
    }

    interface Body {
        void write(DataOutputStream s) throws IOException;
    }

    void send(byte id, int channel, Body body) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);
            render.preparePacketForServer(stream);
            stream.writeByte(id);
            stream.writeByte(channel);
            body.write(stream);
            render.sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static final String[] AGGREGATORS = {"Biggest", "Smallest", "Toggle"};
    static final String[] AGGREGATOR_HELP = {"Uses the biggest\nvalue on the channel.", "Uses the smallest\nvalue on the channel.",
        "Toggles the output each time\nan emitter's value rises.\nUseful to allow multiple buttons\nto control the same light."};

    @Override
    protected void init() {
        super.init();
        enabling.clear();
        text("cols", () -> tr("power / wireless channel"));
        text("range", () -> tr("Range: %1$ blocks", render.descriptor.range + deviceItem(0).getCount()));
        for (int ch = 0; ch < render.descriptor.channelCount; ch++) {
            final int c = ch;
            field("p" + c, () -> render.entries.get(c).powerChannel, v -> send(LampSupplyElement.setPowerName, c, s -> s.writeUTF(v)),
                tr("Power channel name"));
            field("w" + c, () -> render.entries.get(c).wirelessChannel, v -> send(LampSupplyElement.setWirelessName, c, s -> s.writeUTF(v)),
                tr("Wireless channel name"));
            for (int i = 0; i < 3; i++) {
                final byte a = (byte) i;
                var b = button("a" + c + "_" + i, () -> render.entries.get(c).aggregator == a ? "> " + tr(AGGREGATORS[a]) : tr(AGGREGATORS[a]),
                    () -> send(LampSupplyElement.setSelectedAggregator, c, s -> s.writeByte(a)), tr(AGGREGATOR_HELP[a]).split("\n"));
                enabling.add(() -> b.active = render.entries.get(c).aggregator != a);
            }
        }
        update();
    }

    @Override
    protected void update() {
        for (Runnable r : enabling) r.run();
    }
}
