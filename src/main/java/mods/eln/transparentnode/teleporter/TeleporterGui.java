package mods.eln.transparentnode.teleporter;

import mods.eln.gui.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;

import static mods.eln.i18n.I18N.tr;

public class TeleporterGui extends GuiScreenEln {

    public TeleporterGui(EntityPlayer player, TeleporterRender render) {

        this.render = render;
    }

    GuiTextFieldEln name;
    GuiTextFieldEln target;
    GuiVerticalTrackBar chargePower;
    GuiVerticalTrackBarHeat chargeBar;
    GuiButton start;


    TeleporterRender render;

    @Override
    public void initGui() {

        super.initGui();


        name = newGuiTextField(6, 6, 80);
        target = newGuiTextField(6, 6 + 20, 80);
        start = newGuiButton(6, 6 + 20 + 6 + 12, 80, tr("Start"));

        chargePower = newGuiVerticalTrackBar(6 + 80 + 6, 7, 20, 56);
        chargePower.setRange(2000, 20000);
        chargePower.setStepIdMax(20 / 2 * 10);

        chargeBar = newGuiVerticalTrackBarHeat(6 + 80 + 6 + 20 + 6, 7, 20, 56);
        chargeBar.sliderDrawEnable = false;

        name.setText(render.name);
        target.setText(render.targetName);
        chargePower.setValue(render.chargePower);

        name.setComment(0, tr("Transporter name"));
        target.setComment(0, tr("Destination transporter"));
        chargePower.setComment(0, tr("Power consumption:"));
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {

        super.guiObjectEvent(object);
        if (object == start) {
            render.clientSendId(TeleporterElement.startId);
        }
        if (object == name) {
            render.clientSendString(TeleporterElement.setNameId, name.getText());
        }
        if (object == target) {
            render.clientSendString(TeleporterElement.setTargetNameId, target.getText());
        }
        if (object == chargePower) {
            render.clientSendFloat(TeleporterElement.setChargePowerId, chargePower.getValue());
        }
    }

    @Override
    protected void preDraw(float f, int x, int y) {

        super.preDraw(f, x, y);

        if (render.chargePowerNew) {
            chargePower.setValue(render.chargePower);
            render.chargePowerNew = false;
        }
        chargePower.setComment(0, tr("Power consumption: %1$W", chargePower.getValue()));
        start.enabled = render.state == TeleporterElement.StateIdle;

        chargeBar.setRange(0, render.energyTarget);
        chargeBar.temperatureHit = render.energyHit;
        chargeBar.setComment(0, tr("Required energy: %1$J", render.energyTarget));
        chargeBar.setComment(1, ((int) (render.processRatio * 100)) + "%");
    /*	if(render.defaultOutput)
			toogleDefaultOutput.displayString = "default output is high";
		else
			toogleDefaultOutput.displayString = "default output is low";*/
    }

    @Override
    protected GuiHelper newHelper() {

        return new GuiHelper(this, 6 + 80 + 6 + 20 + 6 + 2 + 26, 70);
    }


}
