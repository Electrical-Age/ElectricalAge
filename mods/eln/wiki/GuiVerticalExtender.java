package mods.eln.wiki;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import mods.eln.gui.IGuiObject.IGuiObjectObserver;
import mods.eln.solver.Equation.Add;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public class GuiVerticalExtender extends Gui implements IGuiObject,IGuiObjectObserver{
	
	GuiVerticalTrackBar slider;
	int offX,offY;
	
	public GuiVerticalExtender(int x,int y,int w,int h,GuiHelper helper) {
		this.posX = x;
		this.posY = y;
		this.h = h;
		this.w = w;
		this.offX = x;
		this.offY = y;
		slider = new GuiVerticalTrackBar(x+w-10, y, 10, h, helper);
		
		slider.setStepIdMax(200);
		slider.setStepId(200);
		refreshRange();

		//add(slider);
		slider.setObserver(observer);
		this.helper = helper;
	}
	
	public float getSliderPosition(){
		return (slider.getValue());
	}
	
	public void setSliderPosition(float position){
		refreshRange();
		slider.setValue(position);
	}
	
	void refreshRange()
	{

		int maxY = 10;
		for(IGuiObject o : objectList){
			maxY = Math.max(maxY, o.getYMax());
		}		
		float max = Math.min(0, -(maxY + 10 - h));
		slider.setRange(max, 0);

		
		slider.setVisible(max != 0);
		
	}
	
	GuiHelper helper;
	
	IGuiObjectObserver observer;
	
	int posX,posY,h,w;
	ItemStack stack;

	ArrayList<IGuiObject> objectList = new ArrayList<IGuiObject>();
	
	public void add(IGuiObject o){
		objectList.add(o);
	}
	void remove(IGuiObject o){
		objectList.remove(o);
	}
	int getxOffset(){
		return posX;
	}
	int getYOffset(){
		int sliderOffset = (int) slider.getValue();
		return posY + sliderOffset;
	}
    IGuiObject[] objectListCopy()
    {
    	IGuiObject[] cpy = new IGuiObject[objectList.size()];
    	for(int idx = 0;idx<cpy.length;idx++){
    		cpy[idx] = objectList.get(idx);
    	}
    	return cpy;
    }
    
	
	@Override
	public void idraw(int x, int y, float f) {
		refreshRange();
		
		slider.idraw(x, y, f);
		x -= getxOffset();
		y -= getYOffset();
		GL11.glPushMatrix();		
			int displayWidth =  Minecraft.getMinecraft().displayWidth;
			int displayHeight =  Minecraft.getMinecraft().displayHeight;
			float ratioY = ((float)displayHeight)/this.helper.screen.height ;
			float ratioX = ((float)displayWidth)/this.helper.screen.width ;
			float ratio = Math.max(ratioY, ratioX);

			GL11.glScissor(displayWidth-(int)((posX+w)*ratio),displayHeight-(int)((posY+h)*ratio),(int)(w*ratio),(int)(h*ratio));
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
		//	GL11.glEnable(GL11.GL_SCISSOR_BOX);
			
			GL11.glTranslatef(getxOffset(), getYOffset(), 0f);

			for(IGuiObject o : objectList){
				o.idraw(x,y, f);
			}
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			//GL11.glDisable(GL11.GL_SCISSOR_BOX);
		GL11.glPopMatrix();
		
	//	GL11.glColor3f(1f, 1f, 1f);
	}

	@Override
	public void idraw2(int x, int y) {
		slider.idraw2(x, y);
		x -= getxOffset();
		y -= getYOffset();
		GL11.glPushMatrix();		
			int displayWidth =  Minecraft.getMinecraft().displayWidth;
			int displayHeight =  Minecraft.getMinecraft().displayHeight;
			float ratioY = ((float)displayHeight)/this.helper.screen.height ;
			float ratioX = ((float)displayWidth)/this.helper.screen.width ;
			float ratio = Math.max(ratioY, ratioX);

		//	GL11.glScissor(displayWidth-(int)((posX+w)*ratio),displayHeight-(int)((posY+h)*ratio),(int)(w*ratio),(int)(h*ratio));
		//	GL11.glEnable(GL11.GL_SCISSOR_TEST);
		//	GL11.glEnable(GL11.GL_SCISSOR_BOX);
			
			GL11.glTranslatef(getxOffset(), getYOffset(), 0f);
			for(IGuiObject o : objectList){
				o.idraw2(x,y);
			}
		//	GL11.glDisable(GL11.GL_SCISSOR_TEST);
			//GL11.glDisable(GL11.GL_SCISSOR_BOX);
		GL11.glPopMatrix();
	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		for(IGuiObject o : objectListCopy()){
			if(o.ikeyTyped(key, code)) return true;
		}
		return false;
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
		slider.imouseClicked(x, y, code);
		x -= getxOffset();
		y -= getYOffset();
		for(IGuiObject o : objectListCopy()){
			o.imouseClicked(x,y, code);
		}		
	}

	@Override
	public void imouseMove(int x, int y) {
		slider.imouseMove(x, y);
		x -= getxOffset();
		y -= getYOffset();
		for(IGuiObject o : objectList){
			o.imouseMove(x,y);
		}	
	}

	@Override
	public void imouseMovedOrUp(int x, int y, int witch) {
		slider.imouseMovedOrUp(x, y, witch);
		x -= getxOffset();
		y -= getYOffset();
		for(IGuiObject o : objectList){
			o.imouseMovedOrUp(x,y, witch);
		}	
	}

	@Override
	public void translate(int x, int y) {
		slider.translate(x, y);
		posX+=x;
		posY+=y;
	}
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if(object == slider){
			
		}
	}
	@Override
	public int getYMax() {
		// TODO Auto-generated method stub
		return posY + h;
	}
	
	

}
