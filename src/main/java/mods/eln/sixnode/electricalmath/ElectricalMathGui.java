package mods.eln.sixnode.electricalmath;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import static mods.eln.i18n.I18N.tr;

public class ElectricalMathGui extends GuiContainerEln {

    GuiTextFieldEln expression;
    ElectricalMathRender render;

    public ElectricalMathGui(EntityPlayer player, IInventory inventory, ElectricalMathRender render) {
        super(new ElectricalMathContainer(null, player, inventory));
        //this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176 + 44, 166 - 38, 8 + 44 / 2, 84 - 38);
    }

    @Override
    public void initGui() {
        super.initGui();

        expression = newGuiTextField(8, 8, 176 - 16 + 44);
        expression.setText(render.expression);
        expression.setObserver(this);
        expression.setComment(new String[]{tr("Output voltage formula"),
            tr("Inputs are") + " \u00a74A \u00a72B \u00a71C"});
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);
        if (object == expression) {
            render.clientSetString(ElectricalMathElement.setExpressionId, expression.getText());
        }
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        super.postDraw(f, x, y);
        int c;
        int redNbr = 0;
        ItemStack stack = render.inventory.getStackInSlot(ElectricalMathContainer.Companion.getRestoneSlotId());

        if (stack != null)
            redNbr = stack.stackSize;
        if (!expression.getText().equals(render.expression)) {
            c = 0xFF404040;
            helper.drawString(8 + 44 / 2, 29, c, tr("Waiting for completion..."));
        } else if (expression.getText().equals("")) {
            c = 0xFF404040;
            helper.drawString(8 + 44 / 2, 29, c, tr("Equation required!"));
        } else if (render.equationIsValid) {
            if (redNbr >= render.redstoneRequired)
                c = 0xFF108F00;
            else
                c = 0xFFFF0000;
            helper.drawString(8 + 44 / 2, 29, c, tr("%1$ Redstone(s) required", render.redstoneRequired));
        } else {
            c = 0xFFFF0000;
            helper.drawString(8 + 44 / 2, 29, c, tr("Invalid equation!"));
        }
    }
}
