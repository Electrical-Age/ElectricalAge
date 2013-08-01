package mods.eln.electricalmath;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalMathGui extends GuiScreenEln {

	GuiTextFieldEln expression;
	ElectricalMathRender render;
	
	public ElectricalMathGui(ElectricalMathRender render) {
		this.render = render;
	}
	
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 150+12, 12+12);
	}
   
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

		expression = newGuiTextField(6,6, 150);
		expression.setText(render.expression);
		expression.setObserver(this);
		expression.setComment(new String[]{"Set the output voltage"});
	}
	


	

	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
		if(object == expression){
			render.clientSetString(ElectricalMathElement.setExpressionId, expression.getText());
		}
	}
}
