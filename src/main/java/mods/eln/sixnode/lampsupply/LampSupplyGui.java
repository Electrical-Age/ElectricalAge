package mods.eln.sixnode.lampsupply;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class LampSupplyGui extends GuiContainerEln {

    GuiTextFieldEln channel;
    private LampSupplyRender render;

    public LampSupplyGui(LampSupplyRender render, EntityPlayer player, IInventory inventory) {
        super(new LampSupplyContainer(player, inventory));
        this.render = render;
    }

    @Override
    public void initGui() {
        super.initGui();
        channel = newGuiTextField(8, 8, 138);
        channel.setText(render.channel);
        channel.setComment(0, "Specify the channel to supply");
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 166 - 57, 8, 84 - 57);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        if (object == channel) {
            render.clientSetString(LampSupplyElement.setChannelId, channel.getText());
        }
        super.guiObjectEvent(object);
    }
}
