package mods.eln.sixnode.energymeter;

import java.text.NumberFormat;
import java.text.ParseException;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.sixnode.energymeter.EnergyMeterElement.Mod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class EnergyMeterGui extends GuiContainerEln {

	public EnergyMeterGui(EntityPlayer player, IInventory inventory, EnergyMeterRender render) {
		super(new EnergyMeterContainer(player, inventory));
		this.render = render;
	}

	GuiButton stateBt, passwordBt, modBt,setEnergyBt,resetTimeBt;
	GuiTextFieldEln textFeild;
	EnergyMeterRender render;

	enum SelectedType {
		none, min, max
	};

	@Override
	public void initGui() {
		super.initGui();
		int x = 6, y = 6;

		isLogged = render.password.equals("");
		textFeild = newGuiTextField(12, 58 / 2 + 3, 50);

		textFeild.setComment(0, "Minimum voltage before cutting off");

		x = 80;
		stateBt = newGuiButton(x, y, 70, "");
		y += 22;
		passwordBt = newGuiButton(x, y, 70, "");
		y += 22;
		modBt = newGuiButton(x, y, 70, "");
		y += 22;
		setEnergyBt = newGuiButton(x, y, 70, "Set energy counter");
		y += 22;
		resetTimeBt = newGuiButton(x, y, 70, "Reset time counter");
		y += 22;
	}

	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);

		if (object == stateBt) {
			render.clientSend(EnergyMeterElement.clientToggleStateId);
		}
		if (object == passwordBt) {
			if (isLogged) {
				render.clientSetString(EnergyMeterElement.clientPasswordId, textFeild.getText());
			} else {
				if (textFeild.getText().equals(render.password)) {
					isLogged = true;
				}
			}
		}
		
		if(object == modBt){
			switch (render.mod) {
			case ModCounter:
				render.clientSetString(EnergyMeterElement.clientModId, Mod.ModPrepay.name());
				break;
			case ModPrepay:
				render.clientSetString(EnergyMeterElement.clientModId, Mod.ModCounter.name());
				break;
			}
		}
		
		if(object == setEnergyBt){
			double newVoltage;
			try {
				newVoltage = NumberFormat.getInstance().parse(textFeild.getText()).doubleValue();
			} catch(ParseException e) {
				return;
			}
			
			render.clientSetDouble(EnergyMeterElement.clientEnergyStackId,newVoltage);
		}
		
		if(object == resetTimeBt){
			render.clientSend(EnergyMeterElement.clientTimeCounterId);
		}

	}

	boolean isLogged;

	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		if (!render.switchState)
			stateBt.displayString = "is OFF";
		else
			stateBt.displayString = "is ON";

		if (isLogged)
			passwordBt.displayString = "Change password";
		else
			passwordBt.displayString = "Try password";

		switch (render.mod) {
		case ModCounter:
			modBt.displayString = "In counter mod";
			break;
		case ModPrepay:
			modBt.displayString = "In prepay mod";
			break;
		}

		modBt.enabled = isLogged;
		stateBt.enabled = isLogged;

	}

	
	
	@Override
	protected void postDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.postDraw(f, x, y);
		
		helper.drawString(6, 60, 0xff000000, "E : " + (int)(render.energyStack));
	}
	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 50+166,8,50+84);
	}
}
