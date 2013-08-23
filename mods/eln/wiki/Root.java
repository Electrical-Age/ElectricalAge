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
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.util.Icon;

public class Root extends GuiScreenEln{

	public Root(EntityPlayer player) {

	}


	GuiButton toogleDefaultOutput;
	GuiTextFieldEln searchText;

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

		toogleDefaultOutput = newGuiButton(6, 64/2-10,115, "toogle switch");
		searchText = newGuiTextField(0, 0, 100);

	}
	ArrayList<ItemStack> searchList = new ArrayList<ItemStack>();
	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
    	if(object == toogleDefaultOutput){
    	
    	}
    	else if(object == searchText){
    		searchList.clear();
    		Utils.getItemStack(searchText.getText(), searchList);
		//.out.println(searchList);
    			
    	}
	}
	@Override
	protected void preDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.preDraw(f, x, y);

	}
	
	
	@Override
	protected void postDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.postDraw(f, x, y);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        RenderHelper.enableGUIStandardItemLighting();
        
		for(int idx = 0;idx < searchList.size();idx++){
			ItemStack stack = searchList.get(idx);
			
			Utils.drawItemStack(stack,50,30+idx*16,"");
	       /* if (stack == null)
	        {
	            Icon icon = par1Slot.getBackgroundIconIndex();

	            if (icon != null)
	            {
	                GL11.glDisable(GL11.GL_LIGHTING);
	                this.mc.func_110434_K().func_110577_a(TextureMap.field_110576_c);
	                this.drawTexturedModelRectFromIcon(i, j, icon, 16, 16);
	                GL11.glEnable(GL11.GL_LIGHTING);
	                flag1 = true;
	            }
	        }*/
	     //   if (!flag1)
	     /*   {
	            if (flag)
	            {
	                drawRect(i, j, i + 16, j + 16, -2130706433);
	            }*/

	           // GL11.glEnable(GL11.GL_DEPTH_TEST);
	            //itemRenderer.renderItemAndEffectIntoGUI(this.fontRenderer, this.mc.func_110434_K(), stack, i, j);
	            //itemRenderer.renderItemOverlayIntoGUI(this.fontRenderer, this.mc.func_110434_K(), stack, i, j, s);
	      //  }
		}
    /*  
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableStandardItemLighting();*/
	}

	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return helper = new GuiHelper(this, 128, 64);
	}


	GuiHelper helper;
	
}
