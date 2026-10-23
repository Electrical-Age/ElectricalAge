package mods.eln.sixnode.tutorialsign;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static mods.eln.i18n.I18N.tr;

/** Tutorial sign (P4b; layout gui/tutorial_sign.json): the beacon name. */
public class TutorialSignScreen extends ElnScreen {
    final TutorialSignRender render;

    public TutorialSignScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (TutorialSignRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field("name", () -> render.baliseName == null ? "" : render.baliseName, v -> {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);
                render.preparePacketForServer(stream);
                stream.writeByte(TutorialSignElement.setTextFileId);
                stream.writeUTF(v);
                render.sendPacketToServer(bos);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, tr("Set beacon name"));
    }
}
