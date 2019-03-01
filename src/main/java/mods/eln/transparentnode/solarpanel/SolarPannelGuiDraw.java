package mods.eln.transparentnode.solarpanel;


import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import static mods.eln.i18n.I18N.tr;


public class SolarPannelGuiDraw extends GuiContainerEln {


    private TransparentNodeElementInventory inventory;
    SolarPanelRender render;

    GuiVerticalTrackBar vuMeterTemperature;

    public SolarPannelGuiDraw(EntityPlayer player, IInventory inventory, SolarPanelRender render) {
        super(new SolarPanelContainer(null, player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;


    }

    public void initGui() {
        super.initGui();


        vuMeterTemperature = newGuiVerticalTrackBar(176 / 2 + 12, 8, 20, 69);
        vuMeterTemperature.setStepIdMax(181);
        vuMeterTemperature.setEnable(true);
        vuMeterTemperature.setRange((float) render.descriptor.alphaMin, (float) render.descriptor.alphaMax);
        syncVumeter();
    }

    public void syncVumeter() {
        vuMeterTemperature.setValue(render.pannelAlphaSyncValue);
        render.pannelAlphaSyncNew = false;
    }


    @Override
    public void guiObjectEvent(IGuiObject object) {

        super.guiObjectEvent(object);
        if (vuMeterTemperature == object) {
            render.clientSetPannelAlpha(vuMeterTemperature.getValue());
        }
    }

    @Override
    protected void preDraw(float f, int x, int y) {

        super.preDraw(f, x, y);
        if (render.pannelAlphaSyncNew) syncVumeter();
        //vuMeterTemperature.temperatureHit = (float) (SolarPanelSlowProcess.getSolarAlpha(render.tileEntity.world));
        vuMeterTemperature.setEnable(!render.hasTracker);
        int sunAlpha = ((int) (180 / Math.PI * SolarPanelSlowProcess.getSolarAlpha(render.tileEntity.getWorld())) - 90);

        vuMeterTemperature.setComment(0, tr("Solar panel angle: %s°", ((int) (180 / Math.PI * vuMeterTemperature.getValue()) - 90)));
        if (Math.abs(sunAlpha) > 90)
            vuMeterTemperature.setComment(1, tr("It is night"));
        else
            vuMeterTemperature.setComment(1, tr("Sun angle: %s°", sunAlpha));
    }

    @Override
    protected void postDraw(float f, int x, int y) {

        super.postDraw(f, x, y);
        //drawString(8, 6,"Alpha " + render.pannelAlphaSyncNew);
    }

    @Override
    protected GuiHelperContainer newHelper() {

        return new GuiHelperContainer(this, 176, 166, 8, 84);
    }


}
