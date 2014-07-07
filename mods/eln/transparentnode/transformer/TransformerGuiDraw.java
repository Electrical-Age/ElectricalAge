package mods.eln.transparentnode.transformer;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;


public class TransformerGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    TransformerRender render;
    GuiButton buttonGrounded;

    
    
    public TransformerGuiDraw(EntityPlayer player, IInventory inventory,TransformerRender render)
    {
        super(new TransformerContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    
    	//buttonGrounded = newGuiButton(176/2-60,8+3,120 , "");
    }
    

    @Override
    protected void preDraw(float f, int x, int y) {
    	
    	super.preDraw(f, x, y);
    /*	if(render.grounded)
    		buttonGrounded.displayString = "Self Grounded";
    	else
    		buttonGrounded.displayString = "Externally Grounded";
*/
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	
    	super.guiObjectEvent(object);
    	/*if(object == buttonGrounded)
    	{
    		render.clientSetGrounded(!render.grounded);
    	}*/
    }

	@Override
	protected GuiHelperContainer newHelper() {
		
			return new GuiHelperContainer(this, 176, 194-33,8,84 + 194 - 166-33, "transformer.png");
		//return new HelperStdContainer(this);
	}
    

    
    

}
