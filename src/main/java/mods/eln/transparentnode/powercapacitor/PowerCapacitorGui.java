package mods.eln.transparentnode.powercapacitor;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import static mods.eln.i18n.I18N.tr;


public class PowerCapacitorGui extends GuiContainerEln {


    private TransparentNodeElementInventory inventory;
    PowerCapacitorRender render;


    public PowerCapacitorGui(EntityPlayer player, IInventory inventory, PowerCapacitorRender render) {
        super(new PowerCapacitorContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

    public void initGui() {
        super.initGui();
    }


    @Override
    public void guiObjectEvent(IGuiObject object) {

        super.guiObjectEvent(object);

    }

    @Override
    protected void preDraw(float f, int x, int y) {

        super.preDraw(f, x, y);
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        helper.drawString(8, 8, 0xFF000000, tr("Capacity: %1$F", Utils.plotValue(render.descriptor.getCValue(render.inventory))));
        helper.drawString(8, 8 + 8 + 1, 0xFF000000, tr("Nominal voltage: %1$V", Utils.plotValue(render.descriptor.getUNominalValue(render.inventory))));
        super.postDraw(f, x, y);
    }

    @Override
    protected GuiHelperContainer newHelper() {

        return new GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54);
    }


}
