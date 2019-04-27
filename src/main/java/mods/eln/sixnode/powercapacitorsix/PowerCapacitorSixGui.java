package mods.eln.sixnode.powercapacitorsix;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import static mods.eln.i18n.I18N.tr;

public class PowerCapacitorSixGui extends GuiContainerEln {

    private SixNodeElementInventory inventory;
    PowerCapacitorSixRender render;

    public PowerCapacitorSixGui(EntityPlayer player, IInventory inventory, PowerCapacitorSixRender render) {
        super(new PowerCapacitorSixContainer(player, inventory));
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
        helper.drawString(8, 8, 0xFF000000, tr("Capacity: %sF", Utils.plotValue(render.descriptor.getCValue(render.inventory))));
        helper.drawString(8, 8 + 8 + 1, 0xFF000000, tr("Nominal voltage: %sV", Utils.plotValue(render.descriptor.getUNominalValue(render.inventory))));
        super.postDraw(f, x, y);
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54);
    }
}
