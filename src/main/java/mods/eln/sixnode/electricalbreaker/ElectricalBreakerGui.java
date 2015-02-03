package mods.eln.sixnode.electricalbreaker;

import java.text.NumberFormat;
import java.text.ParseException;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.HelperStdContainerSmall;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class ElectricalBreakerGui extends GuiContainerEln {

    GuiButton toogleSwitch;
    GuiTextFieldEln setUmin, setUmax;
    ElectricalBreakerRender render;

    enum SelectedType{none, min, max}

	public ElectricalBreakerGui(EntityPlayer player, IInventory inventory, ElectricalBreakerRender render) {
		super(new ElectricalBreakerContainer(player, inventory));
		this.render = render;
	}

	@Override
	public void initGui() {
		super.initGui();

        setUmin = newGuiTextField(12, 58 / 2 + 3, 50);
        setUmax = newGuiTextField(12, 58 / 2 - 5 - 10, 50);

        setUmin.setText(render.uMin);
        setUmax.setText(render.uMax);
        
        setUmin.setComment(0, "Minimum voltage before cutting off");
        setUmax.setComment(0, "Maximum voltage before cutting off");
        
		toogleSwitch = newGuiButton(72 - 2, 58 / 2 - 10, 70, "toogle switch");
	}
    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if (object == setUmax) {
			try {
				render.clientSetVoltageMax(NumberFormat.getInstance().parse(setUmax.getText()).floatValue());
			} catch(ParseException e) {
			}
    	} else if (object == setUmin) {
			try {
				render.clientSetVoltageMin(NumberFormat.getInstance().parse(setUmin.getText()).floatValue());
			} catch(ParseException e) {
			}
    	} else if (object == toogleSwitch) {
    		render.clientToogleSwitch();
    	}
    }

    @Override
    protected void preDraw(float f, int x, int y) {
    	super.preDraw(f, x, y);
    	if (!render.switchState)
    		toogleSwitch.displayString = "Switch is OFF";
    	else
    		toogleSwitch.displayString = "Switch is ON";
    }

	@Override
	protected GuiHelperContainer newHelper() {
		return new HelperStdContainerSmall(this);
	}
}
