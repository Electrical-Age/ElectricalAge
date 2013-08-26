package mods.eln.wiki;

import net.minecraft.client.gui.GuiScreen;
import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;

public class Default extends GuiScreenEln{
	public Default(GuiScreen preview) {
		this.preview = preview;
	}
	
	GuiScreen preview;
	GuiHelper helper;
	GuiButtonEln previewBt;
	GuiTextFieldEln searchText;
	
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return helper = new GuiHelper(this, 240, 166);
	}

	protected GuiVerticalExtender extender;
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		
		extender = new GuiVerticalExtender(6, 28, helper.xSize-12, helper.ySize-28-8,helper);
		add(extender);
		
		previewBt = newGuiButton(6, 6, 56, "Previous");

		searchText = newGuiTextField(6+56+6, 10, helper.xSize - 6-56-6-10);
	
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
		
		if(object == previewBt){
			Utils.clientOpenGui(preview);
		}
    	else if(object == searchText){
    		Utils.clientOpenGui(new Search(searchText.getText()));	
    	}
		
	}
	
}
