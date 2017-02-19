package mods.eln.sixnode.resistor;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import static mods.eln.i18n.I18N.tr;

public class ResistorGui extends GuiContainerEln {

    ResistorRender render;
    private SixNodeElementInventory inventory;

    public ResistorGui(EntityPlayer player, IInventory inventory, ResistorRender render) {
        super(new ResistorContainer(player, inventory));
        this.inventory = (SixNodeElementInventory) inventory;
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
        helper.drawString(8, 12, 0xFF000000, tr("Resistance: %1$Ω", Utils.plotValue(render.getDescriptor().getRsValue(render.getInventory()))));
        super.postDraw(f, x, y);
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54);
    }
}
