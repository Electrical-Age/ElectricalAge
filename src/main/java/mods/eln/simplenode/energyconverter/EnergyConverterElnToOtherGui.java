package mods.eln.simplenode.energyconverter;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import net.minecraft.entity.player.EntityPlayer;

import static mods.eln.i18n.I18N.tr;

public class EnergyConverterElnToOtherGui extends GuiScreenEln {

    EnergyConverterElnToOtherEntity render;
    GuiVerticalTrackBar voltage;

    public EnergyConverterElnToOtherGui(EntityPlayer player, EnergyConverterElnToOtherEntity render) {
        this.render = render;
    }

    @Override
    public void initGui() {
        super.initGui();

        voltage = newGuiVerticalTrackBar(6, 6 + 2, 20, 50);
        voltage.setStepIdMax((int) 100);
        voltage.setEnable(true);
        voltage.setRange(0f, 1f);

        syncVoltage();
    }

    public void syncVoltage() {
        voltage.setValue(render.inPowerFactor);
        render.hasChanges = false;
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);
        if (object == voltage) {
            render.sender.clientSendFloat(EnergyConverterElnToOtherNode.setInPowerFactor, voltage.getValue());
        }
    }

    @Override
    protected void preDraw(float f, int x, int y) {
        super.preDraw(f, x, y);
        if (render.hasChanges) syncVoltage();
        voltage.setComment(0, tr("Input power is limited to %sW", (int) (voltage.getValue() * render.inPowerMax)));
    }

    @Override
    protected GuiHelper newHelper() {
        return new GuiHelper(this, 12 + 20, 12 + 50 + 4);
    }
}
