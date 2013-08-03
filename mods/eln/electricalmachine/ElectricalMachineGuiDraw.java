package mods.eln.electricalmachine;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.gui.GuiVerticalVoltageSupplyBar;
import mods.eln.gui.GuiVerticalWorkingZoneBar;
import mods.eln.gui.HelperStdContainer;
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


public class ElectricalMachineGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    ElectricalMachineRender render;

    
    public ElectricalMachineGuiDraw(EntityPlayer player, IInventory inventory,ElectricalMachineRender render)
    {
        super(new ElectricalMachineContainer(null,player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;     
    }
    
    GuiVerticalVoltageSupplyBar voltageBar;

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		
		
		
		voltageBar = new GuiVerticalVoltageSupplyBar(176-1 , 8, 20, 122-18, helper);
		voltageBar.setNominalU((float) render.descriptor.nominalU);
		helper.add(voltageBar);
	}

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176+28, 122,8,40);
	}
	
	
	@Override
	protected void postDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.postDraw(f, x, y);
		
	//	drawTexturedModalRectEln(94, 33,177,14 , (int) (22*render.processState), 15);
		((GuiHelperContainer)helper).drawProcess(94, 33-20-2,render.processState);
		//draw
		
		
		voltageBar.setVoltage((float) (render.UFactor*render.descriptor.nominalU));
		voltageBar.setPower((float) (render.powerFactor * render.descriptor.nominalP));
	}
}
