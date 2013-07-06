package mods.eln.electricalsource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalSourceGui extends GuiScreenEln {

	GuiTextFieldEln voltage;
	ElectricalSourceRender render;
	
	public ElectricalSourceGui(ElectricalSourceRender render) {
		this.render = render;
	}
	
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 128, 64, "electricalsource.png");
	}
   
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

		voltage = newGuiTextField(128/2-60/2, 64/2-6, 60);
		voltage.setText((float)render.voltage);
		voltage.setObserver(this);
		voltage.setComment(new String[]{"Set the output voltage"});
	}
	


	

	@Override
	public void textFieldNewValue(GuiTextFieldEln textField, String value) {
		// TODO Auto-generated method stub
		float newVoltage;
		
		try{
			newVoltage = Float.parseFloat (voltage.getText());
		} catch(NumberFormatException e)
		{
			return;
		}
		
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
		    DataOutputStream stream = new DataOutputStream(bos);   	
		
		    render.preparePacketForServer(stream);
			
			stream.writeByte(ElectricalSourceElement.setVoltageId);
			stream.writeFloat(newVoltage);
			
			render.sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 		
	}


}
