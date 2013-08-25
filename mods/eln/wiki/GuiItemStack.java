package mods.eln.wiki;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiItemStack extends Gui implements IGuiObject{

	public GuiItemStack(int x,int y,ItemStack stack,IGuiObjectObserver observer,GuiScreen gui) {
		this.posX = x;
		this.posY = y;
		h = 18;
		w = 18;
		this.stack = stack;
		this.observer = observer;
		this.gui = gui;
	}
	
	
	IGuiObjectObserver observer;
	
	int posX,posY,h,w;
	ItemStack stack;

	private GuiScreen gui;
	static final ResourceLocation slotSkin = new ResourceLocation("textures/gui/container/furnace.png");
	

	
	@Override
	public void idraw(int x, int y, float f) {
		RenderHelper.disableStandardItemLighting();
		GL11.glColor3f(1f, 1f, 1f);
		Utils.bindTexture(slotSkin);
		drawTexturedModalRect(posX-1, posY -1, 55, 16, 73-55, 34-16);	
		
		if(stack != null){
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
            RenderHelper.enableGUIStandardItemLighting();
	        
	        Utils.drawItemStack(stack, posX, posY, null);
	        
	        GL11.glEnable(GL11.GL_LIGHTING);
	        GL11.glEnable(GL11.GL_DEPTH_TEST);
	      //  RenderHelper.enableStandardItemLighting();
		}
		
	}

	@Override
	public void idraw2(int x, int y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
		if(x >= posX && y >= posY && x < posX + w && y < posY + h){
    		Utils.clientOpenGui(new ItemDefault(stack,gui));
			
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
