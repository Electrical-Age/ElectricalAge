package mods.eln.mppt;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.HelperStdContainer;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.TransparentNodeElementInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.StatCollector;


public class MpptGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    MpptRender render;
    GuiButton buttonGrounded;
    GuiVerticalTrackBar vuMeterUtarget;
    
    
    public MpptGuiDraw(EntityPlayer player, IInventory inventory,MpptRender render)
    {
        super(new MpptContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    	vuMeterUtarget = newGuiVerticalTrackBar(width*1/3,height/5,20,60);
    	vuMeterUtarget.setStepIdMax((int)100);
    	vuMeterUtarget.setEnable(true);
    	vuMeterUtarget.setRange((float)render.descriptor.outUmin,(float)render.descriptor.outUmax);
    	syncVumeterUtarget();
    	
    	buttonGrounded = newGuiButton(width/2,height/2,100, "");
    }
    
    public void syncVumeterUtarget()
    {
    	vuMeterUtarget.setValue(render.UtargetSyncValue);
    	render.UtargetSyncNew = false;
    }
    

 
    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2,
                    int par3) {

            if(render.UtargetSyncNew) syncVumeterUtarget();

    }

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new HelperStdContainer(this);
	}
}
