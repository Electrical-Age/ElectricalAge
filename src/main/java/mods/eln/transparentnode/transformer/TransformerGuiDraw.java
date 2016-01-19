package mods.eln.transparentnode.transformer;

import mods.eln.gui.GuiButtonEln;
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
    GuiButtonEln isIsolator;

    
    
    public TransformerGuiDraw(EntityPlayer player, IInventory inventory,TransformerRender render)
    {
        super(new TransformerContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    
    	isIsolator = newGuiButton(176/2-36,8+3+60,72 , "");
        isIsolator.setComment(0,"Could be used to improve");
        isIsolator.setComment(1,"Electrical Age simulation time");
        isIsolator.setComment(2,"When isolated is selected");
        isIsolator.setComment(3,"it split the network at this place");
        isIsolator.setComment(4,"Usefull to isolate stuff that");
        isIsolator.setComment(5,"switch very very often like relay");
        isIsolator.setComment(5,"in a DC/DC circuit");
        isIsolator.setComment(6,"The downside of this mode is that");
        isIsolator.setComment(7,"will react like a little capacitor");

    }
    

    @Override
    protected void preDraw(float f, int x, int y) {
    	
    	super.preDraw(f, x, y);
    	if(render.isIsolator)
    		isIsolator.displayString = "Isolated";
    	else
    		isIsolator.displayString = "Not isolated";

    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	
    	super.guiObjectEvent(object);
    	if(object == isIsolator)
    	{
    		render.clientSendId(TransformerElement.toogleIsIsolator);
    	}
    }

	@Override
	protected GuiHelperContainer newHelper() {
		
			return new GuiHelperContainer(this, 176, 194-33+20,8,84 + 194 - 166-33+20, "transformer.png");
		//return new HelperStdContainer(this);
	}
    

    
    

}
