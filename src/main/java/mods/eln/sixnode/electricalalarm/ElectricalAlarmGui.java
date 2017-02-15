package mods.eln.sixnode.electricalalarm;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;

import static mods.eln.i18n.I18N.tr;

public class ElectricalAlarmGui extends GuiScreenEln {

    GuiButton toogleDefaultOutput;
    ElectricalAlarmRender render;

    public ElectricalAlarmGui(EntityPlayer player, ElectricalAlarmRender render) {
        this.render = render;
    }

    @Override
    public void initGui() {
        super.initGui();

        toogleDefaultOutput = newGuiButton(6, 32 / 2 - 10, 115, tr("Toggle switch"));
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);
        if (object == toogleDefaultOutput) {
            render.clientSend(ElectricalAlarmElement.clientSoundToggle);
        }
    }

    @Override
    protected void preDraw(float f, int x, int y) {
        super.preDraw(f, x, y);
        if (!render.mute)
            toogleDefaultOutput.displayString = tr("Sound is not muted");
        else
            toogleDefaultOutput.displayString = tr("Sound is muted");
    }

    @Override
    protected GuiHelper newHelper() {
        return new GuiHelper(this, 128, 32);
    }
}
