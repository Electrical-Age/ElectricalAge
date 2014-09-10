package mods.eln.transparentnode.turret;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.sixnode.electricalentitysensor.TurretContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class TurretGui extends GuiContainerEln {
    public TurretGui(EntityPlayer player, IInventory inventory, TurretRender render) {
        super(new TurretContainer(player, inventory));
        this.render = render;
    }

    TurretRender render;

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 166 - 52, 8, 84 - 52);
    }
}
