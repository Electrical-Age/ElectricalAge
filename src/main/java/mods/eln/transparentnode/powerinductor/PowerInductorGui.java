package mods.eln.transparentnode.powerinductor;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import static mods.eln.i18n.I18N.tr;


public class PowerInductorGui extends GuiContainerEln {


    private TransparentNodeElementInventory inventory;
    PowerInductorRender render;


    public PowerInductorGui(EntityPlayer player, IInventory inventory, PowerInductorRender render) {
        super(new PowerInductorContainer(player, inventory));
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
        helper.drawString(8, 12, 0xFF000000, tr("Inductance: %sH", Utils.plotValue(render.descriptor.getlValue(render.inventory))));
        super.postDraw(f, x, y);
    }

    @Override
    protected GuiHelperContainer newHelper() {

        return new GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54);
    }


}
