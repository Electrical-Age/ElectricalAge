package mods.eln.tutorialsign;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

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

public class TutorialSignGui extends GuiScreenEln {

	GuiTextFieldEln fileName;
	TutorialSignRender render;
	
	public TutorialSignGui(TutorialSignRender render) {
		this.render = render;
	}
	
	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 150 + 12, 12 + 12);
	}
   
	@Override
	public void initGui() {
		super.initGui();

		fileName = newGuiTextField(6, 6, 150);
		fileName.setText(render.baliseName);
		fileName.setObserver(this);
		fileName.setComment(new String[]{"Set balise name"});
	}
	
	@Override
	public void textFieldNewValue(GuiTextFieldEln textField, String value) {

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
		    DataOutputStream stream = new DataOutputStream(bos);   	
		
		    render.preparePacketForServer(stream);
			
			stream.writeByte(TutorialSignElement.setTextFileId);
			stream.writeUTF(fileName.getText());
			
			render.sendPacketToServer(bos);
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}
}
