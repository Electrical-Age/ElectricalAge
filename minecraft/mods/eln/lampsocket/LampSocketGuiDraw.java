package mods.eln.lampsocket;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeElementInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.StatCollector;
/*
public class MachineGuiClientExample extends GuiScreen {
	MachineBlockEntity tileEntity;
    public MachineGuiClientExample (InventoryPlayer inventoryPlayer,	NodeBlockEntity tileEntity) {
	    //the container is instanciated and passed to the superclass for handling
	    super();
	    this.tileEntity = (MachineBlockEntity) tileEntity;
	}

}
*/

public class LampSocketGuiDraw extends GuiContainerEln {

	
    private SixNodeElementInventory inventory;
    LampSocketRender lampRender;
    GuiButton buttonGrounded;
    
    GuiVerticalTrackBar alphaZ;
    public LampSocketGuiDraw(EntityPlayer player, IInventory inventory,LampSocketRender lampRender)
    {
        super(new LampSocketContainer(player, inventory,lampRender.lampSocketDescriptor));
        this.inventory = (SixNodeElementInventory) inventory;
        this.lampRender = lampRender;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	int x = 0;
    	if(lampRender.descriptor.alphaZMax == lampRender.descriptor.alphaZMin)
    	{
    		x = - 0;
    	}
    	
    	buttonGrounded = newGuiButton(x+176/2-30,20,60,"");
    	alphaZ  = newGuiVerticalTrackBar(176 - 8 - 20, 8, 20, 69);
    	alphaZ.setRange(lampRender.descriptor.alphaZMin, lampRender.descriptor.alphaZMax);
    	alphaZ.setStepIdMax(200);
    	alphaZ.setValue(lampRender.alphaZ);
    	
    	if(lampRender.descriptor.alphaZMax == lampRender.descriptor.alphaZMin)
    	{
    		alphaZ.setVisible(false);
    	}
    }
    

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if(object == buttonGrounded)
    	{
    		lampRender.clientSetGrounded(!lampRender.getGrounded());
    	}
    	else if(object == alphaZ)
    	{
    		lampRender.clientSetFloat(LampSocketElement.setAlphaZId, alphaZ.getValue());
    	}
    }
	/*
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
            fontRenderer.drawString("Tiny", 8, 6, 4210752);
            fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }*/

    

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new HelperStdContainer(this);
	}

	@Override
	protected void preDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.preDraw(f, x, y);
		if(lampRender.grounded)
			buttonGrounded.displayString = "Parallel";
		else
			buttonGrounded.displayString = "Serial";
		
		alphaZ.setComment(0, "Orientation " + (int)alphaZ.getValue()+" degree");
	}
}
