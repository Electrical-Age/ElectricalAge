package mods.eln.turbine;

import mods.eln.client.ClientProxy;
import mods.eln.misc.Direction;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;


public class TurbineRender extends TransparentNodeElementRender{

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 64, this);
	
	TurbineDescriptor descriptor;
	
	public TurbineRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		this.descriptor = (TurbineDescriptor) descriptor;
	}

	@Override
	public void draw() {

		GL11.glPushMatrix();
		GL11.glScalef(1.0f, 1.0f, 1.0f);		
		descriptor.draw();
		GL11.glPopMatrix();
	}

	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
	//	return new TransformatorGuiDraw(player, inventory, this);
		return new TurbineGuiDraw(player, inventory, this);
	}
	
	
}
