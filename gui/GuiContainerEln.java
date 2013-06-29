package mods.eln.gui;

import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.gui.IGuiObject.IGuiObjectObserver;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

public abstract class GuiContainerEln extends GuiContainer implements IGuiObjectObserver, GuiTextFieldElnObserver{
	public GuiContainerEln(Container par1Container) {
		super(par1Container);
		// TODO Auto-generated constructor stub
	}

	GuiHelper helper;
	
	
	protected abstract GuiHelper newHelper();

	
	@Override
	public void initGui() {
		super.initGui();
		helper = newHelper();
	}
	
	public GuiTextFieldEln newGuiTextField(int x,int y,int width)
	{
		GuiTextFieldEln o =  helper.newGuiTextField(x, y, width);
		o.setObserver(this);
		return o;
	}

	public GuiButtonEln newGuiButton(int x,int y,int width,String name)
	{
		GuiButtonEln o =  helper.newGuiButton(x, y, width,name);
		o.setObserver(this);
		return o;		
	}
	public GuiVerticalTrackBar newGuiVerticalTrackBar(int x,int y,int width,int height)
	{
		GuiVerticalTrackBar o =  helper.newGuiVerticalTrackBar(x, y, width,height);
		o.setObserver(this);
		return o;	
	}
	public GuiVerticalTrackBarHeat newGuiVerticalTrackBarHeat(int x,int y,int width,int height)
	{
		GuiVerticalTrackBarHeat o =  helper.newGuiVerticalTrackBarHeat(x, y, width,height);
		o.setObserver(this);
		return o;			
	}
	
	@Override
	protected void keyTyped(char key, int code)
    {
		helper.keyTyped(key, code);
		super.keyTyped(key, code);
    }
    protected void mouseClicked(int x, int y, int code)
    {
    	helper.mouseClicked(x, y, code);
        super.mouseClicked(x, y, code);
    }
    @Override
    protected void mouseMovedOrUp(int x, int y, int witch) {
    	helper.mouseMovedOrUp(x, y, witch);
    	super.mouseMovedOrUp(x, y, witch);
    }
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    public void drawScreen(int x, int y, float f) {
    	// TODO Auto-generated method stub
    	super.drawScreen(x, y, f);
    	
    }
    


	@Override
	public void textFieldNewValue(GuiTextFieldEln textField, String value) {
		guiObjectEvent(textField);
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		preDraw(f, i, j);
		helper.mouseMove(i,j);
		helper.draw(i, j, f);
		postDraw(f, i, j);
		
	}


	protected void preDraw(float f, int x, int y)
	{
		
	}
	
	protected void postDraw(float f, int x, int y)
	{
		
	}
	
	protected void drawString(int x,int y,String str)
	{
		drawString(x, y, 4210752,str);
	}	
	protected void drawString(int x,int y,int color,String str)
	{
		helper.drawString(x,y,color,str);		
	}
}
