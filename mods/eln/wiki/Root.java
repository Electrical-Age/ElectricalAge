package mods.eln.wiki;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.print.attribute.standard.SheetCollate;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import mods.eln.Eln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiLabel;
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

public class Root extends Default{

	public Root(GuiScreen preview) {
		super(preview);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		
		int y = 8;
		for(Entry<String, ArrayList<ItemStack>> groupe : Data.groupes.entrySet()){
			y = addStackGroupe(groupe.getValue(),groupe.getKey() ,y);
		}
		
	}
	
	static final int stackPerLine = 10;
	int addStackGroupe(List<ItemStack> list,String name,int y)
	{
		int idx = 0;
		extender.add(new GuiLabel(8, y, name));
		y+=10;
		for(ItemStack stack : list){
			GuiItemStack gui = new GuiItemStack((idx % stackPerLine) * 18 +8, y + (idx / stackPerLine) * 18, stack, helper);
			extender.add(gui);
			idx++;
		}
		y += ((idx - 1) / stackPerLine + 1)*18 + 6; 
		return y;
	}
	
}
