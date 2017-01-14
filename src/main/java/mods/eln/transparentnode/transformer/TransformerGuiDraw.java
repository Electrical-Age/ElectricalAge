package mods.eln.transparentnode.transformer;

import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class TransformerGuiDraw extends GuiContainerEln {
    private final TransparentNodeElementInventory inventory;
    private final TransformerRender render;
    private GuiButtonEln isIsolator;

    public TransformerGuiDraw(EntityPlayer player, IInventory inventory, TransformerRender render) {
        super(new TransformerContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

    public void initGui() {
        super.initGui();

        isIsolator = newGuiButton(176 / 2 - 36, 8 + 3 + 60, 72, "");
        isIsolator.setComment(0, "Can be used to improve");
        isIsolator.setComment(1, "simulation performance.");
        isIsolator.setComment(2, "When isolated is selected");
        isIsolator.setComment(3, "the network will be split.");
        isIsolator.setComment(4, "Useful to isolate circuits that");
        isIsolator.setComment(5, "switch very very often like");
        isIsolator.setComment(5, "relays in a DC/DC circuit.");
        isIsolator.setComment(6, "The downside of this mode is that");
        isIsolator.setComment(7, "it adds a little capacitance.");
    }

    @Override
    protected void preDraw(float f, int x, int y) {
        super.preDraw(f, x, y);
        if (render.isIsolator)
            isIsolator.displayString = "Isolated";
        else
            isIsolator.displayString = "Not isolated";
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);

        if (object == isIsolator) {
            render.clientSendId(TransformerElement.toogleIsIsolator);
        }
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 194 - 33 + 20, 8, 84 + 194 - 166 - 33 + 20, "transformer.png");
    }
}
