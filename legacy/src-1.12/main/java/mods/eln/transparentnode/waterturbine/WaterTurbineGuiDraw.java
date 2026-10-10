package mods.eln.transparentnode.waterturbine;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;


public class WaterTurbineGuiDraw extends GuiContainerEln {


    private TransparentNodeElementInventory inventory;
    WaterTurbineRender render;


    public WaterTurbineGuiDraw(EntityPlayer player, IInventory inventory, WaterTurbineRender render) {
        super(new WaterTurbineContainer(null, player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;


    }

    public void initGui() {
        super.initGui();


    }

    @Override
    protected GuiHelperContainer newHelper() {

        return new GuiHelperContainer(this, 176, 166, 8, 84);
    }

}
