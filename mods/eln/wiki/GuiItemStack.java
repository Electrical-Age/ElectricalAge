package mods.eln.wiki;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiItemStack extends Gui implements IGuiObject{

	public GuiItemStack(int x,int y,ItemStack stack ,GuiHelper helper) {
		this.posX = x;
		this.posY = y;
		h = 18;
		w = 18;
		this.stack = stack;
		this.helper = helper;
	}
	
	
	
	int posX,posY,h,w;
	ItemStack stack;

	public GuiHelper helper;
	static final ResourceLocation slotSkin = new ResourceLocation("textures/gui/container/furnace.png");
	

	
	@Override
	public void idraw(int x, int y, float f) {
		//RenderHelper.disableStandardItemLighting();
		try {
			GL11.glColor3f(1f, 1f, 1f);
			UtilsClient.bindTexture(slotSkin);
			drawTexturedModalRect(posX-1, posY -1, 55, 16, 73-55, 34-16);	
			
			if(stack != null){
			//	RenderHelper.enableStandardItemLighting();
				RenderHelper.enableStandardItemLighting();
				RenderHelper.enableGUIStandardItemLighting();

				UtilsClient.drawItemStack(stack, posX, posY, null,true);

		        RenderHelper.disableStandardItemLighting();
		       // GL11.glEnable(GL11.GL_LIGHTING);
		      //  GL11.glEnable(GL11.GL_DEPTH_TEST);
		      //  RenderHelper.enableStandardItemLighting();
			}			
		} catch (Exception e) {
			// TODO: handle exception
		}

		
	}
	/*    GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    RenderHelper.disableStandardItemLighting();
    GL11.glDisable(GL11.GL_LIGHTING);
  //  GL11.glDisable(GL11.GL_DEPTH_TEST);
    RenderHelper.enableGUIStandardItemLighting();
    
    short short1 = 240;
    short short2 = 240;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)short1 / 1.0F, (float)short2 / 1.0F);

    GL11.glEnable(GL11.GL_LIGHTING);
    
    
  //  RenderHelper.enableStandardItemLighting();
  //  RenderHelper.disableStandardItemLighting();
 //   RenderHelper.enableStandardItemLighting();
 /*  GL11.glDepthFunc(GL11.GL_GREATER);
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glDepthMask(false);
 //   par2TextureManager.func_110577_a(field_110798_h);
    this.zLevel -= 50.0F;
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_DST_COLOR);
    GL11.glColor4f(0.5F, 0.25F, 0.8F, 1.0F);
  //  this.renderGlint(par4 * 431278612 + par5 * 32178161, par4 - 2, par5 - 2, 20, 20);
    GL11.glDisable(GL11.GL_BLEND);
    GL11.glDepthMask(true);
    this.zLevel += 50.0F;
    GL11.glEnable(GL11.GL_LIGHTING);
    GL11.glDepthFunc(GL11.GL_LEQUAL);
	*/
	
	//RenderHelper.enableGUIStandardItemLighting();
	//RenderHelper.enableStandardItemLighting();
	//RenderHelper.disableStandardItemLighting();
   // RenderHelper.enableGUIStandardItemLighting();
    
	@Override
	public void idraw2(int x, int y) {
		if(stack == null) return;
		if((x >= posX && y >= posY && x < posX + w && y < posY + h))
		{
			int px,py;
			px = posX;
			py = posY;
			List list = stack.getTooltip(null, false);
			helper.drawHoveringText(list, x, y, Minecraft.getMinecraft().fontRenderer);
		}
	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
		if(x >= posX && y >= posY && x < posX + w && y < posY + h){
    		if(stack != null){
    			UtilsClient.clientOpenGui(new ItemDefault(stack,helper.screen));
    		}
			/*if(observer != null){
				observer.guiObjectEvent(this);
			}*/
		}
			
	}

	@Override
	public void imouseMove(int x, int y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imouseMovedOrUp(int x, int y, int witch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void translate(int x, int y) {
		// TODO Auto-generated method stub
		this.posX +=x;
		this.posY +=y;
	}

	
	@Override
	public int getYMax() {
		// TODO Auto-generated method stub
		return posY + h;
	}
	
	

}
