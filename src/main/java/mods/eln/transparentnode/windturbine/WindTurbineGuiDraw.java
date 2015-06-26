package mods.eln.transparentnode.windturbine;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;


public class WindTurbineGuiDraw extends GuiContainerEln {


    private TransparentNodeElementInventory inventory;
    WindTurbineRender render;


    public WindTurbineGuiDraw(EntityPlayer player, IInventory inventory, WindTurbineRender render) {
        super(new WindTurbineContainer(null, player, inventory));
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
