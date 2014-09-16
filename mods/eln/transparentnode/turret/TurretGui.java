package mods.eln.transparentnode.turret;

import mods.eln.gui.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.client.gui.GuiButton;

public class TurretGui extends GuiContainerEln {
    private GuiButton killOrSpareButton;
    private GuiVerticalTrackBar chargePower;
    private TurretRender render;

    public TurretGui(EntityPlayer player, IInventory inventory, TurretRender render) {
        super(new TurretContainer(player, inventory));
        this.render = render;
    }

    @Override
    public void initGui() {
        super.initGui();

        killOrSpareButton = newGuiButton(11, 6, 60, "");

        chargePower = newGuiVerticalTrackBar(106, 8, 20, 35);
        chargePower.setStepId(99);
        chargePower.setStepIdMax(99);
        chargePower.setRange(100, 10000);
        chargePower.setComment(0, "Recharge power:");
        chargePower.setValue(render.chargePower);
    }

    @Override
    protected void preDraw(float f, int x, int y) {
        super.preDraw(f, x, y);

        if (render.filterIsSpare)
            killOrSpareButton.displayString = "Not attack:";
        else
            killOrSpareButton.displayString = "Attack:";

        chargePower.setComment(1, "" + chargePower.getValue() + " W");
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 186 - 52, 8, 104 - 52);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);

        if (object == killOrSpareButton) {
            render.clientToggleFilterMeaning();
        } else if (object == chargePower) {
            render.clientSetChargePower(chargePower.getValue());
        }
    }
}

