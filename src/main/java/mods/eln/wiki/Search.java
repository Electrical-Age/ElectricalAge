package mods.eln.wiki;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.print.attribute.standard.SheetCollate;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import mods.eln.Eln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class Search extends GuiScreenEln{

	private String bootText;

	public Search(String text) {
		bootText = text;
		
	}


	ArrayList<ItemStack> searchList = new ArrayList<ItemStack>();
	ArrayList<GuiItemStack> guiStackList = new ArrayList<GuiItemStack>();
	void searchStack(String text){
		for(GuiItemStack gui : guiStackList){
			remove(gui);
		}
		guiStackList.clear();
		searchList.clear();
		Utils.getItemStack(text, searchList);
		int idx = 0;;
		for(ItemStack stack : searchList){
			GuiItemStack gui = new GuiItemStack((idx % 8)*21+6, idx/8*21+24, stack,helper);
			guiStackList.add(gui);
			add(gui);
			idx++;
			if(idx > 8*7-1) break;
		}
	}
	
	
	
	GuiButton toogleDefaultOutput;
	GuiTextFieldEln searchText;

	@Override
	public void initGui() {
		
		super.initGui();

		//toogleDefaultOutput = newGuiButton(8, 8,176-16, "toogle switch");
		searchText = newGuiTextField(8, 8,176-16);
		searchText.setText(bootText);
		
		
		searchStack(searchText.getText());

	}

	@Override
	public void guiObjectEvent(IGuiObject object) {
		
		super.guiObjectEvent(object);
    	if(object == toogleDefaultOutput){
    	
    	}
    	else if(object == searchText){

    		searchStack(searchText.getText());
    	}
    	else if(object instanceof GuiItemStack){
    		GuiItemStack gui = (GuiItemStack) object;
    		//Utils.clientOpenGui(new ItemDefault(gui.stack));
    	}
	}
	@Override
	protected void preDraw(float f, int x, int y) {
		
		super.preDraw(f, x, y);

	}
	
	
	@Override
	protected void postDraw(float f, int x, int y) {
		
		super.postDraw(f, x, y);


	}

	@Override
	protected GuiHelper newHelper() {
		
		return helper = new GuiHelper(this, 176, 166+6);
	}


	GuiHelper helper;
	
}
