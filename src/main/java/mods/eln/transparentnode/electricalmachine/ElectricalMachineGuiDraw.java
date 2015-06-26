package mods.eln.transparentnode.electricalmachine;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalVoltageSupplyBar;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class ElectricalMachineGuiDraw extends GuiContainerEln {

    private TransparentNodeElementInventory inventory;
    ElectricalMachineRender render;

    GuiVerticalVoltageSupplyBar voltageBar;

    public ElectricalMachineGuiDraw(EntityPlayer player, IInventory inventory, ElectricalMachineRender render) {
        super(new ElectricalMachineContainer(null, player, inventory, render.descriptor));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

    @Override
    public void initGui() {
        super.initGui();

        voltageBar = new GuiVerticalVoltageSupplyBar(176 - 1, 8, 20, 122 - 18, helper);
        voltageBar.setNominalU((float) render.descriptor.nominalU);
        helper.add(voltageBar);
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176 + 28, 122, 8, 40);
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        super.postDraw(f, x, y);

        //	drawTexturedModalRectEln(94, 33, 177, 14, (int)(22 * render.processState), 15);
        ((GuiHelperContainer) helper).drawProcess(67, 33 - 20 - 2, render.processState);
        //draw

        voltageBar.setVoltage((float) (render.UFactor * render.descriptor.nominalU));
        voltageBar.setPower((float) (render.powerFactor * render.descriptor.nominalP));
    }
}
