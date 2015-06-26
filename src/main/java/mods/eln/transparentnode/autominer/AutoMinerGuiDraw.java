package mods.eln.transparentnode.autominer;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.transparentnode.autominer.AutoMinerSlowProcess.jobType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class AutoMinerGuiDraw extends GuiContainerEln {

    private TransparentNodeElementInventory inventory;
    AutoMinerRender render;

    public AutoMinerGuiDraw(EntityPlayer player, IInventory inventory, AutoMinerRender render) {
        super(new AutoMinerContainer(null, player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        if (render.job == jobType.chestFull) {
            drawString(8, 7, "Chest missing on the");
            drawString(8, 7 + 9, "back of the Auto Miner!");
        }
        super.postDraw(f, x, y);
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 166 + 18 * 2 - 90, 8, 84 - 90 + 18 * 2);
    }
}
