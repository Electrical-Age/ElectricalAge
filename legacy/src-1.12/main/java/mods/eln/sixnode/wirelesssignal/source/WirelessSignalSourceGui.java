package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;

import static mods.eln.i18n.I18N.tr;

public class WirelessSignalSourceGui extends GuiScreenEln {

    GuiTextFieldEln channel;
    private WirelessSignalSourceRender render;

    public WirelessSignalSourceGui(WirelessSignalSourceRender render) {
        this.render = render;
    }

    @Override
    public void initGui() {
        super.initGui();
        channel = newGuiTextField(6, 6, 220);
        channel.setText(render.channel);
        channel.setComment(0, tr("Specify the channel"));
    }

    @Override
    protected GuiHelper newHelper() {
        return new GuiHelper(this, 220 + 12, 12 + 12);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        if (object == channel) {
            render.clientSetString(WirelessSignalSourceElement.setChannelId, channel.getText());
        }
        super.guiObjectEvent(object);
    }
}
