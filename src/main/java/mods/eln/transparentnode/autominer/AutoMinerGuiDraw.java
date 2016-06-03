package mods.eln.transparentnode.autominer;

import static mods.eln.i18n.I18N.tr;

import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.transparentnode.autominer.AutoMinerSlowProcess.jobType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class AutoMinerGuiDraw extends GuiContainerEln {

    private static final String SK_TOUCH = tr("Silk Touch");
    private AutoMinerRender render;
    private GuiButtonEln silkTouch;

    public AutoMinerGuiDraw(EntityPlayer player, IInventory inventory, AutoMinerRender render) {
        super(new AutoMinerContainer(null, player, inventory));
        this.render = render;
    }

    @Override
    public void initGui() {
        super.initGui();
        silkTouch = newGuiButton(7, 6, 122, SK_TOUCH);
        silkTouch.setComment(0, tr("Halves speed, triples power draw"));
    }

    @Override
    protected void preDraw(float f, int x, int y) {
        super.preDraw(f, x, y);
        final String text = render.silkTouch ? tr("On") : tr("Off");
        silkTouch.displayString = String.format("%s %s", SK_TOUCH, text);
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        if (render.job == jobType.chestFull) {
            silkTouch.visible = false;
            String[] text = tr("Chest missing on the\nback of the auto miner!").split("\n");
            drawString(8, 7, text[0]);
            drawString(8, 7 + 9, text[1]);
        } else {
            silkTouch.visible = true;
        }
        super.postDraw(f, x, y);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        if (object == silkTouch) {
            render.clientSendId(AutoMinerElement.toggleSilkTouch);
        }
        super.guiObjectEvent(object);
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 166 + 18 * 2 - 90, 8, 84 - 90 + 18 * 2);
    }
}
