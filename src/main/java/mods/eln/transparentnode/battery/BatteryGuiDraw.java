package mods.eln.transparentnode.battery;

import mods.eln.gui.*;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class BatteryGuiDraw extends GuiContainerEln {

    private TransparentNodeElementInventory inventory;
    BatteryRender render;
    GuiButtonEln buttonGrounded;
    GuiVerticalProgressBar energyBar;

    public BatteryGuiDraw(EntityPlayer player, IInventory inventory, BatteryRender render) {
        super(new BatteryContainer(null, player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

    public void initGui() {
        super.initGui();

        buttonGrounded = newGuiButton(8, 45, 100, "");
        buttonGrounded.visible = false;
        energyBar = newGuiVerticalProgressBar(167 - 16, 8, 16, 69);
        energyBar.setColor(0.2f, 0.5f, 0.8f);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);
        if (buttonGrounded == object) {
            render.clientSetGrounded(!render.grounded);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
        super.drawGuiContainerForegroundLayer(param1, param2);
    }

    @Override
    protected void preDraw(float f, int x, int y) {
        super.preDraw(f, x, y);
        buttonGrounded.displayString = "Grounded : " + render.grounded;
        energyBar.setValue((float) (render.energy / (render.descriptor.electricalStdEnergy * render.life)));
        energyBar.setComment(0, "Energy " + Utils.plotPercent("", energyBar.getValue()).replace(" ", ""));
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        super.postDraw(f, x, y);
        String str1 = "", str2 = "";

        double p = render.power;
        double energyMiss = render.descriptor.electricalStdEnergy * render.life - render.energy;

        if (Math.abs(p) < 5) {
            str1 = "No charge";
        } else if (p > 0) {
            str1 = "Discharge";
            str2 = Utils.plotTime("", render.energy / p);
        } else if (energyMiss > 0) {
            str1 = "Charge";
            str2 = Utils.plotTime("", -energyMiss / p);
        } else {
            str1 = "Charged";
        }

        int xDelta = 70;
        if (render.descriptor.lifeEnable) {
            drawString(8, 8, "Life:");
            drawString(xDelta, 8, Utils.plotPercent("", render.life));
        }
        drawString(8, 17, "Energy:");
        drawString(xDelta, 17,
                Utils.plotValue(render.energy, "J/") + Utils.plotValue(render.descriptor.electricalStdEnergy * render.life, "J"));

        if (render.power >= 0)
            drawString(8, 26, "Power out:");
        else
            drawString(8, 26, "Power in:");
        drawString(xDelta, 26, Utils.plotValue(Math.abs(render.power), "W/") + Utils.plotValue(render.descriptor.electricalStdP, "W"));

        drawString(8, 35, str1);
        drawString(xDelta, 35, str2);

        //   drawString(8, 44, "Thermal protection");
    }
    /*
        list.add("Nominal voltage : " + (int)(electricalU) + "V");
		list.add("Nominal power : " + (int)(electricalStdP) + "W");
     */

    @Override
    protected GuiHelperContainer newHelper() {
        return new HelperStdContainer(this);
    }
}
